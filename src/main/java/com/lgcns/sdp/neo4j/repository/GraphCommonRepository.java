package com.lgcns.sdp.neo4j.repository;

import com.lgcns.sdp.neo4j.constant.GraphQueryType;
import com.lgcns.sdp.neo4j.dto.GraphDetailDto;
import com.lgcns.sdp.neo4j.dto.GraphLabelCountDto;
import com.lgcns.sdp.neo4j.dto.GraphSchemaDto;
import com.lgcns.sdp.neo4j.dto.GraphSearchBarDto;
import com.lgcns.sdp.neo4j.util.GraphUtil;
import org.neo4j.driver.types.Entity;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import lombok.RequiredArgsConstructor;
import org.neo4j.driver.Value;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


import java.util.*;

@Repository
@RequiredArgsConstructor
public class GraphCommonRepository {

    private final Neo4jClient neo4jClient;
    private final GraphUtil graphUtil;


    @Transactional(readOnly = true)
    public Collection<GraphSchemaDto> findSchemaInfo() {
        return neo4jClient.query(GraphQueryType.SCHEMA_INFO.getQuery()) // Enum 호출
                .fetchAs(GraphSchemaDto.class)
                .mappedBy((typeSystem, record) -> {
                    String label = record.get("label").asString();
                    List<Value> propsList = record.get("propsList").asList(v -> v);

                    Map<String, String> propertiesMap = new HashMap<>();
                    for (Value val : propsList) {
                        propertiesMap.put(val.get("name").asString(), val.get("type").asString());
                    }
                    return new GraphSchemaDto(label, propertiesMap);
                })
                .all();
    }

    @Transactional(readOnly = true)
    public Collection<GraphLabelCountDto> getLabelCounts() {
        return neo4jClient.query(GraphQueryType.LABEL_COUNTS.getQuery()) // Enum 호출
                .fetchAs(GraphLabelCountDto.class)
                .mappedBy((typeSystem, record) -> new GraphLabelCountDto(
                        record.get("label").asString(),
                        record.get("count").asLong()))
                .all();
    }

    @Transactional(readOnly = true)
    public GraphSearchBarDto findSearchBarSchema() {
        return neo4jClient.query(GraphQueryType.SEARCH_BAR.getQuery())
                .fetchAs(GraphSearchBarDto.class)
                .mappedBy((typeSystem, record) -> {

                    // [1] 스타일 조회를 위한 로컬 캐시 생성 (요청 1번당 1개)
                    Map<String, Map<String, Object>> styleCache = new HashMap<>();

                    Value schema = record.get("schema");

                    // --- 1. 노드 처리 ---
                    List<Value> nodesList = schema.get("nodes").asList(v -> v);
                    List<GraphSearchBarDto.NodeSchema> nodeSchemas = nodesList.stream()
                            .map(nodeVal -> {
                                String label = nodeVal.get("label").asString();

                                List<Value> propsVal = nodeVal.get("properties").asList(v -> v);
                                List<GraphSearchBarDto.PropertySchema> propertySchemas = propsVal
                                        .stream()
                                        .map(p -> new GraphSearchBarDto.PropertySchema(
                                                p.get("key").asString(),
                                                p.get("type").asString()))
                                        .toList();

                                // [2] 노드 스타일 조회 (캐시 사용)
                                Map<String, Object> style = graphUtil.getStyleConfig(label, "NODE", styleCache);

                                // 생성자에 style Map 전달 -> DTO 내부 @JsonAnyGetter가 처리
                                return new GraphSearchBarDto.NodeSchema(label, propertySchemas, style);
                            }).toList();

                    // --- 2. 관계(Relationship) 처리 ---
                    List<Value> relsList = schema.get("relationships").asList(v -> v);
                    List<GraphSearchBarDto.RelationshipSchema> relSchemas = relsList.stream()
                            .map(relVal -> {
                                String relationshipName = relVal.get("relationship").asString();

                                List<Value> connectionVals = relVal.get("list").asList(v -> v);
                                List<GraphSearchBarDto.ConnectionSchema> connections = connectionVals.stream()
                                        .map(conn -> new GraphSearchBarDto.ConnectionSchema(
                                                conn.get("header").asString(),
                                                conn.get("tail").asString()
                                        ))
                                        .toList();

                                // [3] 관계 스타일 조회 (캐시 사용)
                                Map<String, Object> style = graphUtil.getStyleConfig(relationshipName, "RELATIONSHIP", styleCache);

                                // 생성자에 style Map 전달
                                return new GraphSearchBarDto.RelationshipSchema(relationshipName, connections, style);
                            })
                            .toList();

                    return new GraphSearchBarDto(nodeSchemas, relSchemas);
                })
                .one()
                .orElse(new GraphSearchBarDto(Collections.emptyList(), Collections.emptyList()));
    }


    // (참고) 결과를 Nodes와 Relationships 리스트로 분리하는 헬퍼 메서드
    @Transactional(readOnly = true)
    public GraphDetailDto findNodeAndNeighbors(String elementId) {
        // [쿼리] 시스템 ID(elementId)로 특정 노드와 그 이웃들을 조회
        String query = """
                MATCH (n)
                WHERE elementId(n) = $elementId
                OPTIONAL MATCH (n)-[r]-(connectedNode)
                RETURN n, r, connectedNode
                """;

        Collection<Map<String, Object>> result = neo4jClient.query(query)
                .bind(elementId).to("elementId")
                .fetch()
                .all();

        // 변환 메서드 호출
        return convertToGraphDetailDto(result);
    }

    // [변환 헬퍼 메서드] Raw 데이터를 GraphDetailDto로 변환
    private GraphDetailDto convertToGraphDetailDto(Collection<Map<String, Object>> result) {
        Map<String, Map<String, Object>> uniqueNodes = new HashMap<>();
        Map<String, Map<String, Object>> uniqueRels = new HashMap<>();

        // 1. 노드 ID와 라벨 매핑용 Map
        Map<String, String> nodeIdToLabelMap = new HashMap<>();

        // [추가] 2. 스타일 캐시 생성 (요청 1회당 1개)
        Map<String, Map<String, Object>> styleCache = new HashMap<>();

        Map<String, Object> centerNodeData = null;

        for (Map<String, Object> row : result) {
            // --- 1. 중심 노드 (n) 처리 ---
            Entity centerEntity = (Entity) row.get("n");

            // [변경] mapNodeToMap 호출 시 styleCache 전달
            Map<String, Object> centerMap = mapNodeToMap(centerEntity, styleCache);

            uniqueNodes.put(centerEntity.elementId(), centerMap);
            saveNodeLabel(centerEntity, nodeIdToLabelMap);

            if (centerNodeData == null) {
                centerNodeData = centerMap;
            }

            // --- 2. 이웃 노드 (connectedNode) 처리 ---
            Entity neighborEntity = (Entity) row.get("connectedNode");
            if (neighborEntity != null) {
                // [변경] mapNodeToMap 호출 시 styleCache 전달
                uniqueNodes.put(neighborEntity.elementId(), mapNodeToMap(neighborEntity, styleCache));
                saveNodeLabel(neighborEntity, nodeIdToLabelMap);
            }

            // --- 3. 관계 (r) 처리 ---
            Entity relationship = (Entity) row.get("r");
            if (relationship != null) {
                // [변경] mapRelationshipToMap 호출 시 styleCache 전달
                uniqueRels.put(relationship.elementId(), mapRelationshipToMap(relationship, nodeIdToLabelMap, styleCache));
            }
        }

        return GraphDetailDto.builder()
                .centerNode(centerNodeData)
                .nodes(new ArrayList<>(uniqueNodes.values()))
                .relationships(new ArrayList<>(uniqueRels.values()))
                .build();
    }

    private void saveNodeLabel(Entity entity, Map<String, String> map) {
        if (entity instanceof Node) {
            Node node = (Node) entity;
            String label = "Node";
            if (node.labels().iterator().hasNext()) {
                label = node.labels().iterator().next();
            }
            map.put(node.elementId(), label);
        }
    }

    // (참고) Neo4j Node 객체를 Map으로 바꾸는 메서드 (기존 로직 활용)
    private Map<String, Object> mapNodeToMap(Entity node, Map<String, Map<String, Object>> styleCache) {
        Map<String, Object> map = new HashMap<>(node.asMap());
        map.put("id", node.elementId());

        if (node instanceof Node) {
            Node n = (Node) node;
            map.put("labels", n.labels());

            // 대표 라벨 추출
            String label = "Node";
            if (n.labels().iterator().hasNext()) {
                label = n.labels().iterator().next();
            }
            map.put("type", label);

            // [추가] 스타일 조회 및 적용
            // 라벨을 기준으로 스타일을 가져와서 'style' 키에 저장
            Map<String, Object> style = graphUtil.getStyleConfig(label, "NODE", styleCache);
            if (style != null) {
                map.put("style", style);
            }
        }
        return map;
    }

    // (참고) Neo4j Relationship 객체를 Map으로 바꾸는 메서드
    private Map<String, Object> mapRelationshipToMap(Entity rel, Map<String, String> nodeIdToLabelMap, Map<String, Map<String, Object>> styleCache) {
        Map<String, Object> map = new HashMap<>(rel.asMap());
        map.put("id", rel.elementId());

        if (rel instanceof Relationship) {
            Relationship r = (Relationship) rel;
            String type = r.type();
            map.put("label", type);

            String sourceId = r.startNodeElementId();
            String targetId = r.endNodeElementId();

            String sourceLabel = nodeIdToLabelMap.getOrDefault(sourceId, "Unknown");
            String targetLabel = nodeIdToLabelMap.getOrDefault(targetId, "Unknown");

            map.put("sourceLabel", sourceLabel);
            map.put("targetLabel", targetLabel);

            Map<String, Object> sourceStyle = graphUtil.getStyleConfig(sourceLabel, "NODE", styleCache);
            if (sourceStyle != null) {
                map.put("sourceStyle", sourceStyle);
            }

            Map<String, Object> targetStyle = graphUtil.getStyleConfig(targetLabel, "NODE", styleCache);
            if (targetStyle != null) {
                map.put("targetStyle", targetStyle);
            }

            Map<String, Object> relStyle = graphUtil.getStyleConfig(type, "RELATIONSHIP", styleCache);
            if (relStyle != null) {
                map.put("style", relStyle); // 관계 자체의 스타일
            }
        }
        return map;
    }


    // [3] 외부 쿼리 실행기 (보안 강화 버전)
    // readOnly = true: DB 드라이버 차원에서 쓰기 시도 시 예외 발생시킴
    @Transactional(readOnly = true)
    public Collection<Map<String, Object>> executeRawCypher(String cypherQuery) {

        // 1차 방어: 금칙어 검사 (대소문자 무시)
        String upperQuery = cypherQuery.toUpperCase().trim();

        // 위험한 키워드가 있으면 즉시 차단
        if (upperQuery.contains("DELETE") ||
                upperQuery.contains("DETACH") ||
                upperQuery.contains("CREATE") ||
                upperQuery.contains("SET") ||
                upperQuery.contains("MERGE") ||
                upperQuery.contains("REMOVE")) {
            throw new IllegalArgumentException("보안 경고: 데이터 변경 쿼리(DELETE, CREATE 등)는 실행할 수 없습니다.");
        }

        // 2차 방어: @Transactional(readOnly=true)에 의해 쓰기 작업 시 롤백/예외 발생
        return neo4jClient.query(cypherQuery)
                .fetch()
                .all();
    }

}