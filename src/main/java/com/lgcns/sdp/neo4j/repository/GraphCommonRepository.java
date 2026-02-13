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
        return neo4jClient.query(GraphQueryType.SCHEMA_INFO.getQuery())  
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
        return neo4jClient.query(GraphQueryType.LABEL_COUNTS.getQuery())  
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

                     
                    Map<String, Map<String, Object>> styleCache = new HashMap<>();

                    Value schema = record.get("schema");

                     
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

                                 
                                Map<String, Object> style = graphUtil.getStyleConfig(label, "NODE", styleCache);

                                 
                                return new GraphSearchBarDto.NodeSchema(label, propertySchemas, style);
                            }).toList();

                     
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

                                 
                                Map<String, Object> style = graphUtil.getStyleConfig(relationshipName, "RELATIONSHIP",
                                        styleCache);

                                 
                                return new GraphSearchBarDto.RelationshipSchema(relationshipName, connections, style);
                            })
                            .toList();

                    return new GraphSearchBarDto(nodeSchemas, relSchemas);
                })
                .one()
                .orElse(new GraphSearchBarDto(Collections.emptyList(), Collections.emptyList()));
    }

     
    @Transactional(readOnly = true)
    public GraphDetailDto findNodeAndNeighbors(String elementId) {
         
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

         
        return convertToGraphDetailDto(result);
    }

    @Transactional(readOnly = true)
    public GraphDetailDto findSpecificNodeNeighbors(String elementId, String relation, String direction, String targetLabel) {

        String matchClause = "MATCH (n) WHERE elementId(n) = $elementId ";
        String optionalMatch = "";

         
        String relType = (relation != null && !relation.trim().isEmpty()) ? ":" + relation : "";

         
         
        String targetNodeStr = "(connectedNode";
        if (targetLabel != null && !targetLabel.trim().isEmpty()) {
            targetNodeStr += ":" + targetLabel;
        }
        targetNodeStr += ")";

         
        if ("OUT".equalsIgnoreCase(direction)) {
             
            optionalMatch = String.format("OPTIONAL MATCH (n)-[r%s]->%s", relType, targetNodeStr);
        } else if ("IN".equalsIgnoreCase(direction)) {
             
            optionalMatch = String.format("OPTIONAL MATCH (n)<-[r%s]-%s", relType, targetNodeStr);
        } else {
             
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
    public GraphDetailDto findSpecificNodeNeighborsBatch(String elementId, List<GraphExpansionCriteriaDto> criteriaList, Integer limit) {

         
        String baseQuery = """
        MATCH (n) WHERE elementId(n) = $elementId
        MATCH (n)-[r]-(connectedNode)
        
        WHERE any(c IN $criteriaList WHERE
            (c.relation IS NULL OR type(r) = c.relation)
            AND
            (c.targetLabel IS NULL OR c.targetLabel IN labels(connectedNode))
            AND
            (
                c.direction = 'ALL' OR c.direction IS NULL OR
                (c.direction = 'OUT' AND startNode(r) = n) OR
                (c.direction = 'IN' AND endNode(r) = n)
            )
        )
        
        RETURN n, r, connectedNode
        """;

        String finalQuery = baseQuery;
        if (limit != null && limit > 0) {
            finalQuery += " LIMIT $limit";
        }

        List<Map<String, Object>> mappedCriteria = criteriaList.stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("relation", c.getRelation());
            map.put("targetLabel", c.getTargetLabel());
            map.put("direction", (c.getDirection() == null || c.getDirection().isEmpty()) ? "ALL" : c.getDirection());
            return map;
        }).toList();

        var runner = neo4jClient.query(finalQuery)
                .bind(elementId).to("elementId")
                .bind(mappedCriteria).to("criteriaList");

        if (limit != null && limit > 0) {
            runner = runner.bind(limit).to("limit");
        }

        Collection<Map<String, Object>> result = runner.fetch().all();

         
        GraphDetailDto dto = convertToGraphDetailDto(result);

         
         
        enrichWithGlobalConnectivity(dto.getNodes());

        return dto;
    }

    private void enrichWithGlobalConnectivity(List<Map<String, Object>> nodeList) {
        if (nodeList == null || nodeList.isEmpty()) return;

         
        List<String> nodeIds = nodeList.stream()
                .map(n -> String.valueOf(n.get("id")))
                .toList();

         
        String statQuery = """
            MATCH (n)-[r]-()
            WHERE elementId(n) IN $nodeIds
            RETURN 
                elementId(n) as id, 
                type(r) as relation,
                CASE WHEN elementId(startNode(r)) = elementId(n) THEN 'TAIL' ELSE 'HEAD' END as position,
                count(r) as count
        """;

        Collection<Map<String, Object>> statsResults = neo4jClient.query(statQuery)
                .bindAll(Map.of("nodeIds", nodeIds))
                .fetch()
                .all();

         
        Map<String, List<Map<String, Object>>> statsMap = new HashMap<>();

        for (Map<String, Object> row : statsResults) {
            String id = String.valueOf(row.get("id"));
            String relation = (String) row.get("relation");
            String position = (String) row.get("position");
            long count = ((Number) row.get("count")).longValue();

            statsMap.putIfAbsent(id, new ArrayList<>());

            Map<String, Object> detailItem = new HashMap<>();
            detailItem.put("relation", relation);
            detailItem.put("position", position);
            detailItem.put("count", count);

            statsMap.get(id).add(detailItem);
        }

         
        for (Map<String, Object> node : nodeList) {
            String id = String.valueOf(node.get("id"));
            List<Map<String, Object>> details = statsMap.getOrDefault(id, new ArrayList<>());

            long totalConnectCount = details.stream()
                    .mapToLong(d -> (long) d.get("count"))
                    .sum();

            node.put("details", details);
            node.put("totalConnectCount", totalConnectCount);
        }
    }


     
    private GraphDetailDto convertToGraphDetailDto(Collection<Map<String, Object>> result) {
        Map<String, Map<String, Object>> uniqueNodes = new HashMap<>();
        Map<String, Map<String, Object>> uniqueRels = new HashMap<>();

         
        Map<String, String> nodeIdToLabelMap = new HashMap<>();

         
        Map<String, Map<String, Object>> styleCache = new HashMap<>();

        Map<String, Object> centerNodeData = null;

        for (Map<String, Object> row : result) {
             
            Entity centerEntity = (Entity) row.get("n");

             
            Map<String, Object> centerMap = mapNodeToMap(centerEntity, styleCache);

            uniqueNodes.put(centerEntity.elementId(), centerMap);
            saveNodeLabel(centerEntity, nodeIdToLabelMap);

            if (centerNodeData == null) {
                centerNodeData = centerMap;
            }

             
            Entity neighborEntity = (Entity) row.get("connectedNode");
            if (neighborEntity != null) {
                 
                uniqueNodes.put(neighborEntity.elementId(), mapNodeToMap(neighborEntity, styleCache));
                saveNodeLabel(neighborEntity, nodeIdToLabelMap);
            }

             
            Entity relationship = (Entity) row.get("r");
            if (relationship != null) {
                 
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

     
    private Map<String, Object> mapNodeToMap(Entity node, Map<String, Map<String, Object>> styleCache) {
        Map<String, Object> map = new HashMap<>(node.asMap());
        map.put("id", node.elementId());

        if (node instanceof Node) {
            Node n = (Node) node;
            map.put("labels", n.labels());

             
            String label = "Node";
            if (n.labels().iterator().hasNext()) {
                label = n.labels().iterator().next();
            }

             
             
            Map<String, Object> style = graphUtil.getStyleConfig(label, "NODE", styleCache);
            if (style != null) {
                map.put("style", style);
            }
        }
        return map;
    }

     
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
                map.put("style", relStyle);  
            }
        }
        return map;
    }

    public Map<String, Object> validateCypher(String cypherQuery) {
        String explainQuery = "EXPLAIN " + cypherQuery;
        Map<String, Object> result = new HashMap<>();

         
         
        try (org.neo4j.driver.Session session = driver.session()) {
            session.run(explainQuery).consume();
            result.put("valid", true);
            result.put("message", "Valid Cypher Query");
        } catch (Exception e) {
            result.put("valid", false);
             
            result.put("message", e.getMessage());
        }
        return result;
    }

     
     
    @Transactional(readOnly = true)
    public Collection<Map<String, Object>> executeRawCypher(String cypherQuery) {

         
        String upperQuery = cypherQuery.toUpperCase().trim();

         
        if (upperQuery.contains("DELETE") ||
                upperQuery.contains("DETACH") ||
                upperQuery.contains("CREATE") ||
                upperQuery.contains("SET") ||
                upperQuery.contains("MERGE") ||
                upperQuery.contains("REMOVE")) {
            throw new IllegalArgumentException("보안 경고: 데이터 변경 쿼리(DELETE, CREATE 등)는 실행할 수 없습니다.");
        }

         
        return neo4jClient.query(cypherQuery)
                .fetch()
                .all();
    }

    @Transactional(readOnly = true)
    public GraphExpansionStatsDto getNodeExpansionStats(String elementId, List<String> excludeRelIds) {

         
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
                .bind(excludes).to("excludeIds")  
                .fetch()
                .all();

        return convertToExpansionStatsDto(result);  
    }

    private GraphExpansionStatsDto convertToExpansionStatsDto(Collection<Map<String, Object>> result) {
         
        Map<String, GraphExpansionStatsDto.ExpansionItemDto> relMap = new HashMap<>();
        Map<String, GraphExpansionStatsDto.ExpansionItemDto> catMap = new HashMap<>();
        Map<String, GraphExpansionStatsDto.ExpansionItemDto> pairMap = new HashMap<>();

        for (Map<String, Object> row : result) {
            String relType = (String) row.get("relType");
            List<String> labels = (List<String>) row.get("targetLabels");
            String targetLabel = labels.isEmpty() ? "Unknown" : labels.get(0);
            String direction = (String) row.get("direction");
            long count = ((Number) row.get("cnt")).longValue();

             
             
             
            String relKey = relType + "|" + direction;
            relMap.compute(relKey, (k, v) -> {
                if (v == null) {
                    return GraphExpansionStatsDto.ExpansionItemDto.builder()
                            .id("rel-" + k)  
                            .label(relType)
                            .direction(direction)
                            .count(count)
                            .build();
                } else {
                    v.setCount(v.getCount() + count);  
                    return v;
                }
            });

             
             
             
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

         
        List<GraphExpansionStatsDto.ExpansionItemDto> rels = new ArrayList<>(relMap.values());
        List<GraphExpansionStatsDto.ExpansionItemDto> cats = new ArrayList<>(catMap.values());
        List<GraphExpansionStatsDto.ExpansionItemDto> pairs = new ArrayList<>(pairMap.values());

         
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