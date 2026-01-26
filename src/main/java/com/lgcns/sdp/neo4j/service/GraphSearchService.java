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
    public List<GraphSearchResponseDto> searchByCyphers(List<CypherBlock> cyphers) {
        if (cyphers == null || cyphers.isEmpty()) {
            return Collections.emptyList();
        }

        List<Condition> whereConditions = new ArrayList<>();

        // ---------------------------------------------------------
        // 1. Cypher-DSL을 이용한 동적 쿼리 빌드 (기존 로직 유지)
        // ---------------------------------------------------------
        CypherBlock firstBlock = cyphers.get(0);
        org.neo4j.cypherdsl.core.Node rootNode = createNode(firstBlock, 0);

        collectConditions(rootNode, firstBlock, whereConditions);

        ExposesRelationships<?> currentPath = rootNode;

        for (int i = 1; i < cyphers.size(); i += 2) {
            if (i + 1 >= cyphers.size()) break;

            CypherBlock relBlock = cyphers.get(i);
            CypherBlock nextNodeBlock = cyphers.get(i + 1);

            org.neo4j.cypherdsl.core.Node nextNode = createNode(nextNodeBlock, i + 1);

            // 관계 생성
            org.neo4j.cypherdsl.core.Relationship relationship = extendPath(currentPath, nextNode, relBlock, i);

            collectConditions(relationship, relBlock, whereConditions);
            collectConditions(nextNode, nextNodeBlock, whereConditions);

            currentPath = relationship;
        }

        PatternElement finalPattern = (PatternElement) currentPath;

        Condition finalCondition = whereConditions.stream()
                .reduce(Condition::and)
                .orElse(Cypher.noCondition());

        // 2. 최종 쿼리 생성
        Statement statement = Cypher.match(Cypher.path("p").definedBy(finalPattern))
                .where(finalCondition)
                .returning(Cypher.name("p")) // Path 전체를 반환
                .build();

        String queryString = Renderer.getDefaultRenderer().render(statement);
        System.out.println("Generated Query: " + queryString);

        // ---------------------------------------------------------
        // 3. 실행 및 데이터 분류 (수정된 부분)
        // ---------------------------------------------------------
        Collection<Map<String, Object>> queryResult = neo4jClient.query(queryString)
                .bindAll(statement.getCatalog().getParameters())
                .fetch()
                .all();

        // 4. 결과 변환 (Nodes와 Edges 리스트 분리)
        return convertToGroupData(queryResult);
    }

    // --- Helper Methods ---

    private Node createNode(CypherBlock block, int index) {
        return "ANY".equals(block.getLabel())
                ? Cypher.anyNode().named("n" + index)
                : Cypher.node(block.getLabel()).named("n" + index);
    }

    private Relationship extendPath(ExposesRelationships<?> from, Node to, CypherBlock block, int index) {
        String label = "ANY".equals(block.getLabel()) ? "" : block.getLabel();
        String direction = block.getDirection() != null ? block.getDirection() : "BOTH";
        String relName = "r" + index;

        return switch (direction) {
            case "OUT" -> (Relationship) from.relationshipTo(to, label).named(relName);
            case "IN" -> (Relationship) from.relationshipFrom(to, label).named(relName);
            default -> (Relationship) from.relationshipBetween(to, label).named(relName);
        };
    }

    /**
     * [수정됨] operator가 없거나 null일 경우 EQUALS로 처리하도록 로직 개선
     */
    private void collectConditions(PropertyContainer container, CypherBlock block, List<Condition> conditions) {
        if (block.getProperties() == null || block.getProperties().isEmpty()) {
            return;
        }

        block.getProperties().forEach((key, val) -> {
            Property property = container.property(key);

            // 1. 기본값 설정 (operator가 없으면 EQUALS)
            String operator = "EQUALS";
            Object value = val;

            // 2. Map 형태인 경우 파싱 ({operator: "GT", value: 10} 또는 {value: 10})
            if (val instanceof Map<?, ?> valMap) {
                // value 키가 있으면 값을 덮어씌움
                if (valMap.containsKey("value")) {
                    value = valMap.get("value");
                }

                // operator 키가 있고 null이 아니면 덮어씌움 (null이면 위에서 설정한 EQUALS 유지)
                if (valMap.containsKey("operator")) {
                    String opParam = (String) valMap.get("operator");
                    if (opParam != null && !opParam.isEmpty()) {
                        operator = opParam;
                    }
                }
            }

            // 3. 조건 빌더 호출
            conditions.add(buildCondition(property, operator, value));
        });
    }

    private Condition buildCondition(Property property, String operator, Object value) {
        if ("IS_NULL".equals(operator)) return property.isNull();
        if ("IS_NOT_NULL".equals(operator)) return property.isNotNull();

        Expression valExpr = Cypher.literalOf(value);

        // switch 문에서 null safe 처리가 필요하다면 default로 빠지게 됨
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
            // EQUALS, null, 그 외 모든 경우는 EQUALS로 처리
            case "EQUALS" -> property.isEqualTo(valExpr);
            default -> property.isEqualTo(valExpr);
        };
    }

    // --- 변환 로직 (기존 유지) ---
    private List<GraphSearchResponseDto> convertToGroupData(Collection<Map<String, Object>> queryResult) {
        List<Map<String, Object>> nodeList = new ArrayList<>();
        List<Map<String, Object>> edgeList = new ArrayList<>();

        Set<String> visitedNodeIds = new HashSet<>();
        Set<String> visitedEdgeIds = new HashSet<>();

        // 결과 순회
        for (Map<String, Object> row : queryResult) {
            // 쿼리에서 returning(Cypher.name("p"))로 Path를 반환했으므로
            Object p = row.get("p");

            if (p instanceof Path path) {
                // 1. Path 내부의 모든 노드 추출
                path.nodes().forEach(node -> {
                    String id = node.elementId();
                    if (!visitedNodeIds.contains(id)) {
                        visitedNodeIds.add(id);

                        Map<String, Object> nodeData = new HashMap<>(node.asMap());
                        nodeData.put("id", id);
                        // 라벨 처리 (첫번째 라벨 사용)
                        nodeData.put("label", node.labels().iterator().hasNext() ? node.labels().iterator().next() : "");

                        nodeList.add(nodeData);
                    }
                });

                // 2. Path 내부의 모든 관계(엣지) 추출
                path.relationships().forEach(rel -> {
                    String id = rel.elementId();
                    if (!visitedEdgeIds.contains(id)) {
                        visitedEdgeIds.add(id);

                        Map<String, Object> relData = new HashMap<>(rel.asMap());
                        relData.put("id", id);
                        relData.put("source", rel.startNodeElementId());
                        relData.put("target", rel.endNodeElementId());
                        relData.put("label", rel.type()); // 관계 타입

                        edgeList.add(relData);
                    }
                });
            }
        }

        // 최종 결과 생성: [ {group: nodes, data: [...]}, {group: edges, data: [...]} ]
        List<GraphSearchResponseDto> result = new ArrayList<>();

        result.add(GraphSearchResponseDto.builder()
                .group("nodes")
                .data(nodeList)
                .build());

        result.add(GraphSearchResponseDto.builder()
                .group("edges")
                .data(edgeList)
                .build());

        return result;
    }
}