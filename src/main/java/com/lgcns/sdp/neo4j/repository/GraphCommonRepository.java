package com.lgcns.sdp.neo4j.repository;

import com.lgcns.sdp.neo4j.constant.GraphQueryType;
import com.lgcns.sdp.neo4j.dto.*;
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
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class GraphCommonRepository {

    private final Neo4jClient neo4jClient;
    private final GraphUtil graphUtil;
    private final org.neo4j.driver.Driver driver;

    @Transactional(readOnly = true)
    public Collection<GraphSchemaDto> findSchemaInfo() {
        return neo4jClient.query(GraphQueryType.SCHEMA_INFO.getQuery()) // Enum 호출
                .fetchAs(GraphSchemaDto.class).mappedBy((typeSystem, record) ->

                {
                    String label = record.get("label").asString();
                    List<Value> propsList = record.get("propsList").asList(v -> v);

                    Map<String, String> propertiesMap = new HashMap<>();
                    for (Value val : propsList) {
                        propertiesMap.put(val.get("name").asString(), val.get("type").asString());
                    }
                    return new GraphSchemaDto(label, propertiesMap);
                }).all();
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
                                                conn.get("tail").asString()))
                                        .toList();

                                // [3] 관계 스타일 조회 (캐시 사용)
                                Map<String, Object> style = graphUtil.getStyleConfig(relationshipName, "RELATIONSHIP",
                                        styleCache);

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

    @Transactional(readOnly = true)
    public GraphDetailDto findSpecificNodeNeighbors(String elementId, String relation, String direction, String targetLabel) {

        String matchClause = "MATCH (n) WHERE elementId(n) = $elementId ";
        String optionalMatch = "";

        // 1. 관계 타입 처리
        String relType = (relation != null && !relation.trim().isEmpty()) ? ":" + relation : "";

        // 2. 타겟 라벨 처리 (추가됨)
        // connectedNode에 라벨 조건을 붙임 ex: (connectedNode:Person)
        String targetNodeStr = "(connectedNode";
        if (targetLabel != null && !targetLabel.trim().isEmpty()) {
            targetNodeStr += ":" + targetLabel;
        }
        targetNodeStr += ")";

        // 3. 방향 및 패턴 조립
        if ("OUT".equalsIgnoreCase(direction)) {
            // (n)-[r]->(connectedNode:Label)
            optionalMatch = String.format("OPTIONAL MATCH (n)-[r%s]->%s", relType, targetNodeStr);
        } else if ("IN".equalsIgnoreCase(direction)) {
            // (n)<-[r]-(connectedNode:Label)
            optionalMatch = String.format("OPTIONAL MATCH (n)<-[r%s]-%s", relType, targetNodeStr);
        } else {
            // (n)-[r]-(connectedNode:Label)
            optionalMatch = String.format("OPTIONAL MATCH (n)-[r%s]-%s", relType, targetNodeStr);
        }

        String query = matchClause + "\n" + optionalMatch + "\n RETURN n, r, connectedNode";

        Collection<Map<String, Object>> result = neo4jClient.query(query)
                .bind(elementId).to("elementId")
                .fetch()
                .all();

        return convertToGraphDetailDto(result);
    }

    @Transactional(readOnly = true)
    public GraphDetailDto findSpecificNodeNeighborsBatch(String elementId, List<GraphExpansionCriteriaDto> criteriaList) {

        // 1. 쿼리 작성: 모든 이웃을 찾은 뒤, 조건 리스트 중 '하나라도' 만족하는지 확인 (WHERE any(...))
        String query = """
            MATCH (n) WHERE elementId(n) = $elementId
            MATCH (n)-[r]-(connectedNode)
            
            // [핵심 로직] criteriaList 중 하나라도 조건에 맞으면 통과 (OR 로직과 유사)
            WHERE any(c IN $criteriaList WHERE 
                // 1. 관계 타입 체크 (null이거나 같거나)
                (c.relation IS NULL OR type(r) = c.relation)
                AND
                // 2. 타겟 라벨 체크 (null이거나 라벨을 포함하거나)
                (c.targetLabel IS NULL OR c.targetLabel IN labels(connectedNode))
                AND
                // 3. 방향 체크
                (
                    c.direction = 'ALL' OR c.direction IS NULL OR
                    (c.direction = 'OUT' AND startNode(r) = n) OR
                    (c.direction = 'IN' AND endNode(r) = n)
                )
            )
            
            RETURN n, r, connectedNode
            """;

        // 파라미터 매핑을 위해 DTO 리스트를 Map 리스트로 변환 필요 (혹은 Object mapper 사용)
        // 여기서는 Neo4jClient가 자동으로 매핑해주길 기대하거나, 수동 변환
        List<Map<String, Object>> mappedCriteria = criteriaList.stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("relation", c.getRelation());
            map.put("targetLabel", c.getTargetLabel());
            map.put("direction", (c.getDirection() == null || c.getDirection().isEmpty()) ? "ALL" : c.getDirection());
            return map;
        }).collect(Collectors.toList());

        Collection<Map<String, Object>> result = neo4jClient.query(query)
                .bind(elementId).to("elementId")
                .bind(mappedCriteria).to("criteriaList") // 리스트 바인딩
                .fetch()
                .all();

        return convertToGraphDetailDto(result); // 기존 변환 로직 재사용
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
                uniqueRels.put(relationship.elementId(),
                        mapRelationshipToMap(relationship, nodeIdToLabelMap, styleCache));
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
    private Map<String, Object> mapRelationshipToMap(Entity rel, Map<String, String> nodeIdToLabelMap,
            Map<String, Map<String, Object>> styleCache) {
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

            map.put("source", sourceId);
            map.put("target", targetId);

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

    public Map<String, Object> validateCypher(String cypherQuery) {
        String explainQuery = "EXPLAIN " + cypherQuery;
        Map<String, Object> result = new HashMap<>();

        // Using raw Driver session to ensure we catch exceptions directly without
        // Spring translation/transaction interference
        try (org.neo4j.driver.Session session = driver.session()) {
            session.run(explainQuery).consume();
            result.put("valid", true);
            result.put("message", "Valid Cypher Query");
        } catch (Exception e) {
            result.put("valid", false);
            // e.getMessage() usually contains the "Invalid input..." string from Neo4j
            result.put("message", e.getMessage());
        }
        return result;
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

    @Transactional(readOnly = true)
    public GraphExpansionStatsDto getNodeExpansionStats(String elementId, List<String> excludeRelIds) {

        // 제외할 ID가 없으면 빈 리스트 처리
        List<String> excludes = (excludeRelIds == null) ? Collections.emptyList() : excludeRelIds;

        String query = """
            MATCH (n) WHERE elementId(n) = $elementId
            MATCH (n)-[r]-(m)
            // [핵심] 이미 화면에 있는 관계(ID)는 제외
            WHERE NOT elementId(r) IN $excludeIds
            
            WITH 
                type(r) as relType, 
                labels(m) as targetLabels, 
                startNode(r) = n as isOutgoing, 
                count(m) as cnt
            RETURN 
                relType, 
                targetLabels, 
                CASE WHEN isOutgoing THEN 'OUT' ELSE 'IN' END as direction, 
                cnt
            """;

        Collection<Map<String, Object>> result = neo4jClient.query(query)
                .bind(elementId).to("elementId")
                .bind(excludes).to("excludeIds") // 파라미터 바인딩
                .fetch()
                .all();

        return convertToExpansionStatsDto(result); // 기존 변환 로직 그대로 사용
    }

    private GraphExpansionStatsDto convertToExpansionStatsDto(Collection<Map<String, Object>> result) {
        // 1. 집계용 Map 생성 (Key -> Dto)
        Map<String, GraphExpansionStatsDto.ExpansionItemDto> relMap = new HashMap<>();
        Map<String, GraphExpansionStatsDto.ExpansionItemDto> catMap = new HashMap<>();
        Map<String, GraphExpansionStatsDto.ExpansionItemDto> pairMap = new HashMap<>();

        for (Map<String, Object> row : result) {
            String relType = (String) row.get("relType");
            List<String> labels = (List<String>) row.get("targetLabels");
            String targetLabel = labels.isEmpty() ? "Unknown" : labels.get(0);
            String direction = (String) row.get("direction");
            long count = ((Number) row.get("cnt")).longValue();

            // ---------------------------------------------------------
            // (1) Relationships 집계: [관계명 + 방향]이 같으면 합침
            // ---------------------------------------------------------
            String relKey = relType + "|" + direction;
            relMap.compute(relKey, (k, v) -> {
                if (v == null) {
                    return GraphExpansionStatsDto.ExpansionItemDto.builder()
                            .id("rel-" + k) // 임시 ID
                            .label(relType)
                            .direction(direction)
                            .count(count)
                            .build();
                } else {
                    v.setCount(v.getCount() + count); // 카운트 누적
                    return v;
                }
            });

            // ---------------------------------------------------------
            // (2) Categories 집계: [타겟라벨]이 같으면 합침 (방향/관계 무시)
            // ---------------------------------------------------------
            String catKey = targetLabel;
            catMap.compute(catKey, (k, v) -> {
                if (v == null) {
                    return GraphExpansionStatsDto.ExpansionItemDto.builder()
                            .id("cat-" + k)
                            .label(targetLabel)
                            .count(count)
                            .build();
                } else {
                    v.setCount(v.getCount() + count);
                    return v;
                }
            });

            // ---------------------------------------------------------
            // (3) Pairs 집계: [관계명 + 방향 + 타겟라벨]이 같으면 합침
            //     (쿼리에서 이미 그룹핑되지만 안전하게 한 번 더)
            // ---------------------------------------------------------
            String pairKey = relType + "|" + direction + "|" + targetLabel;
            pairMap.compute(pairKey, (k, v) -> {
                if (v == null) {
                    return GraphExpansionStatsDto.ExpansionItemDto.builder()
                            .id("pair-" + k)
                            .label(relType)
                            .targetLabel(targetLabel)
                            .direction(direction)
                            .count(count)
                            .build();
                } else {
                    v.setCount(v.getCount() + count);
                    return v;
                }
            });
        }

        // Map -> List 변환 및 ID 재할당 (깔끔하게 index로)
        List<GraphExpansionStatsDto.ExpansionItemDto> rels = new ArrayList<>(relMap.values());
        List<GraphExpansionStatsDto.ExpansionItemDto> cats = new ArrayList<>(catMap.values());
        List<GraphExpansionStatsDto.ExpansionItemDto> pairs = new ArrayList<>(pairMap.values());

        // ID 예쁘게 다시 매기기 (선택사항)
        for (int i = 0; i < rels.size(); i++) rels.get(i).setId("rel-" + i);
        for (int i = 0; i < cats.size(); i++) cats.get(i).setId("cat-" + i);
        for (int i = 0; i < pairs.size(); i++) pairs.get(i).setId("pair-" + i);

        return GraphExpansionStatsDto.builder()
                .relationships(rels)
                .categories(cats)
                .pairs(pairs)
                .build();
    }

}