package com.lgcns.sdp.neo4j.service;

import com.lgcns.sdp.neo4j.dto.GraphSearchRequestDto.CypherBlock;
import com.lgcns.sdp.neo4j.dto.GraphSearchResponseDto;
import lombok.RequiredArgsConstructor;

import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.ExposesRelationships;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.PatternElement;
import org.neo4j.cypherdsl.core.Property;
import org.neo4j.cypherdsl.core.PropertyContainer;
import org.neo4j.cypherdsl.core.Relationship;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.neo4j.driver.types.Path;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class GraphSearchService {

    private final Neo4jClient neo4jClient;

    @Transactional(readOnly = true)
    public GraphSearchResponseDto searchByCyphers(List<CypherBlock> cyphers) {
        // 1. 방어 로직: 입력이 비어있으면 빈 결과 반환
        if (cyphers == null || cyphers.isEmpty()) {
            return GraphSearchResponseDto.builder()
                    .nodes(Collections.emptyList())
                    .relationships(Collections.emptyList())
                    .build();
        }

        List<Condition> whereConditions = new ArrayList<>();

        // 2. 첫 번째 노드 생성
        CypherBlock firstBlock = cyphers.get(0);
        Node rootNode = createNode(firstBlock, 0);

        collectConditions(rootNode, firstBlock, whereConditions);

        // [핵심 수정 1] 타입을 ExposesRelationships<?>로 선언 (Relationship과 Chain 모두 수용 가능)
        ExposesRelationships<?> currentPath = rootNode;

        // 3. 루프를 돌며 경로 확장 (노드 -> 관계 -> 노드 ...)
        for (int i = 1; i < cyphers.size(); i += 2) {
            if (i + 1 >= cyphers.size()) break;

            CypherBlock relBlock = cyphers.get(i);
            CypherBlock nextNodeBlock = cyphers.get(i + 1);

            Node nextNode = createNode(nextNodeBlock, i + 1);

            // [핵심 수정 2] extendPath 호출 (Relationship으로 캐스팅하지 않음)
            currentPath = extendPath(currentPath, nextNode, relBlock, i);

            // [핵심 수정 3] 관계에 대한 WHERE 조건 처리
            // currentPath는 Chain 객체일 수 있어 PropertyContainer가 아님.
            // 따라서 조건 생성을 위한 "프록시(가짜) 관계 객체"를 이름만 따서 따로 만듦.
            String relName = "r" + i;
            Relationship relProxy = Cypher.anyNode()
                    .relationshipTo(Cypher.anyNode(), relBlock.getLabel())
                    .named(relName);

            collectConditions(relProxy, relBlock, whereConditions);
            collectConditions(nextNode, nextNodeBlock, whereConditions);
        }

        // 4. 최종 경로 생성
        // RelationshipChain과 Node 등은 모두 PatternElement를 구현하므로 안전하게 캐스팅 가능
        PatternElement finalPattern = (PatternElement) currentPath;

        // 5. WHERE 조건절 합치기
        Condition finalCondition = whereConditions.stream()
                .reduce(Condition::and)
                .orElse(Cypher.noCondition());

        // 6. 쿼리 빌드 (MATCH p = (A)-[r]->(B)... WHERE ... RETURN p)
        Statement statement = Cypher.match(Cypher.path("p").definedBy(finalPattern))
                .where(finalCondition)
                .returning(Cypher.name("p"))
                .build();

        String queryString = Renderer.getDefaultRenderer().render(statement);

        // 7. 실행
        Collection<Map<String, Object>> queryResult = neo4jClient.query(queryString)
                .bindAll(statement.getCatalog().getParameters())
                .fetch()
                .all();

        // 8. 결과 변환 및 반환
        return convertToGroupData(queryResult);
    }

    // 노드 생성 헬퍼
    private Node createNode(CypherBlock block, int index) {
        return "ANY".equals(block.getLabel())
                ? Cypher.anyNode().named("n" + index)
                : Cypher.node(block.getLabel()).named("n" + index);
    }

    /**
     * [핵심 수정 4] 리턴 타입을 ExposesRelationships<?>로 변경
     * 이렇게 해야 Relationship(단일)과 RelationshipChain(다중)을 모두 반환할 수 있음.
     */
    private ExposesRelationships<?> extendPath(ExposesRelationships<?> from, Node to, CypherBlock block, int index) {
        String label = "ANY".equals(block.getLabel()) ? "" : block.getLabel();
        String direction = block.getDirection() != null ? block.getDirection() : "BOTH";
        String relName = "r" + index;

        // 1. 시작점이 '노드'일 때 (예: 첫 번째 연결)
        if (from instanceof Node node) {
            return switch (direction) {
                case "OUT" -> node.relationshipTo(to, label).named(relName);
                case "IN" -> node.relationshipFrom(to, label).named(relName);
                default -> node.relationshipBetween(to, label).named(relName);
            };
        }
        // 2. 시작점이 '관계 체인'일 때 (예: 두 번째 이상 연결)
        else if (from instanceof org.neo4j.cypherdsl.core.RelationshipChain chain) {
            return switch (direction) {
                case "OUT" -> chain.relationshipTo(to, label).named(relName);
                case "IN" -> chain.relationshipFrom(to, label).named(relName);
                default -> chain.relationshipBetween(to, label).named(relName);
            };
        }

        throw new IllegalArgumentException("지원하지 않는 경로 타입입니다: " + from.getClass().getName());
    }

    // 조건 수집 (operator 처리 포함)
    private void collectConditions(PropertyContainer container, CypherBlock block, List<Condition> conditions) {
        if (block.getProperties() == null || block.getProperties().isEmpty()) {
            return;
        }

        block.getProperties().forEach((key, val) -> {
            Property property = container.property(key);

            String operator = "EQUALS";
            Object value = val;

            if (val instanceof Map<?, ?> valMap) {
                if (valMap.containsKey("value")) {
                    value = valMap.get("value");
                }
                if (valMap.containsKey("operator")) {
                    String opParam = (String) valMap.get("operator");
                    if (opParam != null && !opParam.isEmpty()) {
                        operator = opParam;
                    }
                }
            }
            conditions.add(buildCondition(property, operator, value));
        });
    }

    // 조건식 빌더
    private Condition buildCondition(Property property, String operator, Object value) {
        if ("IS_NULL".equals(operator)) return property.isNull();
        if ("IS_NOT_NULL".equals(operator)) return property.isNotNull();

        Expression valExpr = Cypher.literalOf(value);

        return switch (operator) {
            case "NOT_EQUALS" -> property.isNotEqualTo(valExpr);
            case "GREATER_THAN" -> property.gt(valExpr);
            case "LESS_THAN" -> property.lt(valExpr);
            case "GREATER_THAN_OR_EQUAL" -> property.gte(valExpr);
            case "LESS_THAN_OR_EQUAL" -> property.lte(valExpr);
            case "BETWEEN" -> {
                if (value instanceof List<?> list && list.size() == 2) {
                    Expression min = Cypher.literalOf(list.get(0));
                    Expression max = Cypher.literalOf(list.get(1));
                    yield property.gte(min).and(property.lte(max));
                }
                throw new IllegalArgumentException("BETWEEN operator requires list of 2 values");
            }
            case "NOT_BETWEEN" -> {
                if (value instanceof List<?> list && list.size() == 2) {
                    Expression min = Cypher.literalOf(list.get(0));
                    Expression max = Cypher.literalOf(list.get(1));
                    yield property.gte(min).and(property.lte(max)).not();
                }
                throw new IllegalArgumentException("NOT_BETWEEN operator requires list of 2 values");
            }
            case "CONTAINS" -> property.contains(valExpr);
            case "STARTS_WITH" -> property.startsWith(valExpr);
            case "ENDS_WITH" -> property.endsWith(valExpr);
            default -> property.isEqualTo(valExpr);
        };
    }

    // 결과 변환 (Path -> Node/Edge List)
    private GraphSearchResponseDto convertToGroupData(Collection<Map<String, Object>> queryResult) {
        List<Map<String, Object>> nodeList = new ArrayList<>();
        List<Map<String, Object>> edgeList = new ArrayList<>();

        Set<String> visitedNodeIds = new HashSet<>();
        Set<String> visitedEdgeIds = new HashSet<>();

        for (Map<String, Object> row : queryResult) {
            Object p = row.get("p");

            if (p instanceof Path path) {
                // 노드 추출
                path.nodes().forEach(node -> {
                    String id = node.elementId();
                    if (!visitedNodeIds.contains(id)) {
                        visitedNodeIds.add(id);
                        Map<String, Object> nodeData = new HashMap<>(node.asMap());
                        nodeData.put("id", id);
                        nodeData.put("label", node.labels().iterator().hasNext() ? node.labels().iterator().next() : "");
                        nodeList.add(nodeData);
                    }
                });

                // 관계 추출
                path.relationships().forEach(rel -> {
                    String id = rel.elementId();
                    if (!visitedEdgeIds.contains(id)) {
                        visitedEdgeIds.add(id);
                        Map<String, Object> relData = new HashMap<>(rel.asMap());
                        relData.put("id", id);
                        relData.put("source", rel.startNodeElementId());
                        relData.put("target", rel.endNodeElementId());
                        relData.put("label", rel.type());
                        edgeList.add(relData);
                    }
                });
            }
        }

        return GraphSearchResponseDto.builder()
                .nodes(nodeList)
                .relationships(edgeList)
                .build();
    }
}