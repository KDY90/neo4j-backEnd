package com.lgcns.sdp.neo4j.service;

import com.lgcns.sdp.neo4j.dto.GraphSearchRequestDto.CypherBlock;
import com.lgcns.sdp.neo4j.dto.GraphSearchResponseDto;
import com.lgcns.sdp.neo4j.util.GraphUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.cypherdsl.core.PatternElement;
import org.neo4j.cypherdsl.core.ExposesRelationships;
import org.neo4j.cypherdsl.core.PropertyContainer;
import org.neo4j.cypherdsl.core.Property;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.cypherdsl.core.RelationshipChain;
import org.neo4j.cypherdsl.core.renderer.Renderer;

import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Relationship;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphSearchService {

    private final Neo4jClient neo4jClient;
    private final GraphUtil graphUtil;

    @Transactional(readOnly = true)
    public GraphSearchResponseDto searchByCyphers(List<CypherBlock> cyphers) {
        if (cyphers == null || cyphers.isEmpty()) {
            return GraphSearchResponseDto.builder()
                    .nodes(Collections.emptyList())
                    .relationships(Collections.emptyList())
                    .build();
        }

        // 1. SAVED_QUERY 체크
        Optional<CypherBlock> savedQueryBlock = cyphers.stream()
                .filter(block -> "SAVED_QUERY".equals(block.getType()))
                .findFirst();

        if (savedQueryBlock.isPresent()) {
            return executeSavedQuery(savedQueryBlock.get());
        }

        // 2. 동적 쿼리 빌드 로직 (여기서 쓰는 Node/Relationship은 위에서 임포트한 DSL 타입입니다)
        List<Condition> whereConditions = new ArrayList<>();

        CypherBlock firstBlock = cyphers.get(0);
        Node rootNode = createDslNode(firstBlock, 0);

        collectConditions(rootNode, firstBlock, whereConditions);

        ExposesRelationships<?> currentPath = rootNode;

        for (int i = 1; i < cyphers.size(); i += 2) {
            if (i + 1 >= cyphers.size()) break;

            CypherBlock relBlock = cyphers.get(i);
            CypherBlock nextNodeBlock = cyphers.get(i + 1);

            Node nextNode = createDslNode(nextNodeBlock, i + 1);

            currentPath = extendPath(currentPath, nextNode, relBlock, i);

            String relName = "r" + i;
            // 관계에 대한 조건 생성을 위한 프록시 객체 (DSL Relationship)
            Relationship relProxy = Cypher.anyNode()
                    .relationshipTo(Cypher.anyNode(), relBlock.getLabel())
                    .named(relName);

            collectConditions(relProxy, relBlock, whereConditions);
            collectConditions(nextNode, nextNodeBlock, whereConditions);
        }

        PatternElement finalPattern = (PatternElement) currentPath;

        Condition finalCondition = whereConditions.stream()
                .reduce(Condition::and)
                .orElse(Cypher.noCondition());

        Statement statement = Cypher.match(Cypher.path("p").definedBy(finalPattern))
                .where(finalCondition)
                .returning(Cypher.name("p"))
                .build();

        String queryString = Renderer.getDefaultRenderer().render(statement);

        // 실행 (결과는 Generic Map으로 받음)
        Collection<Map<String, Object>> queryResult = neo4jClient.query(queryString)
                .bindAll(statement.getCatalog().getParameters())
                .fetch()
                .all();

        return convertToGroupData(queryResult);
    }

    private GraphSearchResponseDto executeSavedQuery(CypherBlock block) {
        Map<String, Object> contentMap = block.getSavedQueryContent();
        String rawQuery = "";

        if (contentMap != null && contentMap.containsKey("cypherQuery")) {
            rawQuery = (String) contentMap.get("cypherQuery");
        }

        if (rawQuery == null || rawQuery.trim().isEmpty()) {
            throw new IllegalArgumentException("저장된 쿼리 내용이 없습니다.");
        }

        log.info("Executing Saved Query: {}", rawQuery);

        Collection<Map<String, Object>> queryResult = neo4jClient.query(rawQuery)
                .fetch()
                .all();

        return convertToGroupData(queryResult);
    }

    // --- Helper Methods (Query Building) ---

    // 리턴 타입 Node는 org.neo4j.cypherdsl.core.Node
    private Node createDslNode(CypherBlock block, int index) {
        return "ANY".equals(block.getLabel())
                ? Cypher.anyNode().named("n" + index)
                : Cypher.node(block.getLabel()).named("n" + index);
    }

    private ExposesRelationships<?> extendPath(ExposesRelationships<?> from, Node to, CypherBlock block, int index) {
        String label = "ANY".equals(block.getLabel()) ? "" : block.getLabel();
        String direction = block.getDirection() != null ? block.getDirection() : "BOTH";
        String relName = "r" + index;

        if (from instanceof Node node) {
            return switch (direction) {
                case "OUT" -> node.relationshipTo(to, label).named(relName);
                case "IN" -> node.relationshipFrom(to, label).named(relName);
                default -> node.relationshipBetween(to, label).named(relName);
            };
        } else if (from instanceof RelationshipChain chain) {
            return switch (direction) {
                case "OUT" -> chain.relationshipTo(to, label).named(relName);
                case "IN" -> chain.relationshipFrom(to, label).named(relName);
                default -> chain.relationshipBetween(to, label).named(relName);
            };
        }
        throw new IllegalArgumentException("지원하지 않는 경로 타입입니다.");
    }

    private void collectConditions(PropertyContainer container, CypherBlock block, List<Condition> conditions) {
        if (block.getProperties() == null || block.getProperties().isEmpty()) return;

        block.getProperties().forEach((key, val) -> {
            Property property = container.property(key);
            String operator = "EQUALS";
            Object value = val;

            if (val instanceof Map<?, ?> valMap) {
                if (valMap.containsKey("value")) value = valMap.get("value");
                if (valMap.containsKey("operator")) operator = (String) valMap.get("operator");
            }
            conditions.add(buildCondition(property, operator, value));
        });
    }

    private Condition buildCondition(Property property, String operator, Object value) {
        if ("IS_NULL".equals(operator)) return property.isNull();
        if ("IS_NOT_NULL".equals(operator)) return property.isNotNull();
        Expression valExpr = Cypher.literalOf(value);

        return switch (operator) {
            case "NOT_EQUALS" -> property.isNotEqualTo(valExpr);
            case "GREATER_THAN" -> property.gt(valExpr);
            case "LESS_THAN" -> property.lt(valExpr);
            case "CONTAINS" -> property.contains(valExpr);
            case "STARTS_WITH" -> property.startsWith(valExpr);
            case "ENDS_WITH" -> property.endsWith(valExpr);
            default -> property.isEqualTo(valExpr);
        };
    }

    // =================================================================================
    // [결과 변환 로직]
    // 중요: 여기서부터는 'org.neo4j.driver.types' 패키지를 '풀 패키지명'으로 직접 명시합니다.
    // 상단 import는 DSL용이므로, 여기서 그냥 Node를 쓰면 에러가 납니다.
    // =================================================================================

    private GraphSearchResponseDto convertToGroupData(Collection<Map<String, Object>> queryResult) {
        List<Map<String, Object>> nodeList = new ArrayList<>();
        List<Map<String, Object>> edgeList = new ArrayList<>();

        Set<String> visitedNodeIds = new HashSet<>();
        Set<String> visitedEdgeIds = new HashSet<>();

        Map<String, Map<String, Object>> styleCache = new HashMap<>();
        Map<String, Map<String, Object>> nodeInfoMap = new HashMap<>();

        for (Map<String, Object> row : queryResult) {
            for (Object value : row.values()) {
                processResultItem(value, nodeList, edgeList, visitedNodeIds, visitedEdgeIds, styleCache, nodeInfoMap);
            }
        }

        return GraphSearchResponseDto.builder()
                .nodes(nodeList)
                .relationships(edgeList)
                .build();
    }

    private void processResultItem(Object item,
                                   List<Map<String, Object>> nodeList,
                                   List<Map<String, Object>> edgeList,
                                   Set<String> visitedNodeIds,
                                   Set<String> visitedEdgeIds,
                                   Map<String, Map<String, Object>> styleCache,
                                   Map<String, Map<String, Object>> nodeInfoMap) {
        if (item == null) return;

        // [중요] org.neo4j.driver.types.Path
        if (item instanceof org.neo4j.driver.types.Path) {
            org.neo4j.driver.types.Path path = (org.neo4j.driver.types.Path) item;
            path.nodes().forEach(node -> processNode(node, nodeList, visitedNodeIds, styleCache, nodeInfoMap));
            path.relationships().forEach(rel -> processRelationship(rel, edgeList, visitedEdgeIds, styleCache, nodeInfoMap));
        }
        // [중요] org.neo4j.driver.types.Node
        else if (item instanceof org.neo4j.driver.types.Node) {
            processNode((org.neo4j.driver.types.Node) item, nodeList, visitedNodeIds, styleCache, nodeInfoMap);
        }
        // [중요] org.neo4j.driver.types.Relationship
        else if (item instanceof org.neo4j.driver.types.Relationship) {
            processRelationship((org.neo4j.driver.types.Relationship) item, edgeList, visitedEdgeIds, styleCache, nodeInfoMap);
        }
        // List (collect 등)
        else if (item instanceof List<?>) {
            for (Object subItem : (List<?>) item) {
                processResultItem(subItem, nodeList, edgeList, visitedNodeIds, visitedEdgeIds, styleCache, nodeInfoMap);
            }
        }
    }

    // 파라미터 타입을 Driver Node로 명시
    private void processNode(org.neo4j.driver.types.Node node,
                             List<Map<String, Object>> nodeList,
                             Set<String> visitedNodeIds,
                             Map<String, Map<String, Object>> styleCache,
                             Map<String, Map<String, Object>> nodeInfoMap) {

        String id = node.elementId();
        // Driver Node의 labels()는 Iterable<String>을 반환합니다.
        String label = node.labels().iterator().hasNext() ? node.labels().iterator().next() : "Unknown";

        Map<String, Object> style = graphUtil.getStyleConfig(label, "NODE", styleCache);

        Map<String, Object> info = new HashMap<>();
        info.put("label", label);
        if (style != null) info.put("style", style);
        nodeInfoMap.put(id, info);

        if (!visitedNodeIds.contains(id)) {
            visitedNodeIds.add(id);
            // Driver Node는 asMap()을 가지고 있습니다.
            Map<String, Object> nodeData = new HashMap<>(node.asMap());
            nodeData.put("id", id);
            nodeData.put("label", label);
            if (style != null) nodeData.put("style", style);
            nodeList.add(nodeData);
        }
    }

    // 파라미터 타입을 Driver Relationship으로 명시
    private void processRelationship(org.neo4j.driver.types.Relationship rel,
                                     List<Map<String, Object>> edgeList,
                                     Set<String> visitedEdgeIds,
                                     Map<String, Map<String, Object>> styleCache,
                                     Map<String, Map<String, Object>> nodeInfoMap) {

        String id = rel.elementId();

        if (!visitedEdgeIds.contains(id)) {
            visitedEdgeIds.add(id);
            // Driver Relationship은 asMap(), type(), startNodeElementId() 등을 가지고 있습니다.
            Map<String, Object> relData = new HashMap<>(rel.asMap());

            String label = rel.type();
            String sourceId = rel.startNodeElementId();
            String targetId = rel.endNodeElementId();

            relData.put("id", id);
            relData.put("source", sourceId);
            relData.put("target", targetId);
            relData.put("label", label);

            Map<String, Object> style = graphUtil.getStyleConfig(label, "RELATIONSHIP", styleCache);
            if (style != null) relData.put("style", style);

            if (nodeInfoMap.containsKey(sourceId)) {
                relData.put("sourceLabel", nodeInfoMap.get(sourceId).get("label"));
                relData.put("sourceStyle", nodeInfoMap.get(sourceId).get("style"));
            }
            if (nodeInfoMap.containsKey(targetId)) {
                relData.put("targetLabel", nodeInfoMap.get(targetId).get("label"));
                relData.put("targetStyle", nodeInfoMap.get(targetId).get("style"));
            }

            edgeList.add(relData);
        }
    }
}