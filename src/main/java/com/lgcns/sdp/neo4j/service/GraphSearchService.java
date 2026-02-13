package com.lgcns.sdp.neo4j.service;

import com.lgcns.sdp.neo4j.dto.GraphSearchRequestDto;
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
    public GraphSearchResponseDto searchByCyphers(GraphSearchRequestDto requestDto) {
        List<CypherBlock> cyphers = requestDto.getCyphers();
        int limit = requestDto.getLimit();

        if (cyphers == null || cyphers.isEmpty()) {
            return GraphSearchResponseDto.builder()
                    .nodes(Collections.emptyList())
                    .relationships(Collections.emptyList())
                    .build();
        }

         
        Optional<CypherBlock> savedQueryBlock = cyphers.stream()
                .filter(block -> "SAVED_QUERY".equals(block.getType()))
                .findFirst();

        if (savedQueryBlock.isPresent()) {
            return executeSavedQuery(savedQueryBlock.get(),limit);
        }

         
        List<Condition> whereConditions = new ArrayList<>();

        CypherBlock firstBlock = cyphers.get(0);
         
        Node rootNode = createDslNode(firstBlock, 0);

        collectConditions(rootNode, firstBlock, whereConditions ,requestDto.isCaseInsensitiveSearch());

        ExposesRelationships<?> currentPath = rootNode;

        for (int i = 1; i < cyphers.size(); i += 2) {
            if (i + 1 >= cyphers.size()) break;

            CypherBlock relBlock = cyphers.get(i);
            CypherBlock nextNodeBlock = cyphers.get(i + 1);

            Node nextNode = createDslNode(nextNodeBlock, i + 1);

            currentPath = extendPath(currentPath, nextNode, relBlock, i);

            String relName = "r" + i;
             
            Relationship relProxy = Cypher.anyNode()
                    .relationshipTo(Cypher.anyNode(), relBlock.getLabel())
                    .named(relName);

            collectConditions(relProxy, relBlock, whereConditions,requestDto.isCaseInsensitiveSearch());
            collectConditions(nextNode, nextNodeBlock, whereConditions,requestDto.isCaseInsensitiveSearch());
        }

        PatternElement finalPattern = (PatternElement) currentPath;

        Condition finalCondition = whereConditions.stream()
                .reduce(Condition::and)
                .orElse(Cypher.noCondition());

        Statement statement = Cypher.match(Cypher.path("p").definedBy(finalPattern))
                .where(finalCondition)
                .returning(Cypher.name("p"))
                .limit(limit)
                .build();

        String queryString = Renderer.getDefaultRenderer().render(statement);

         
        Collection<Map<String, Object>> queryResult = neo4jClient.query(queryString)
                .bindAll(statement.getCatalog().getParameters())
                .fetch()
                .all();

        return convertToGroupData(queryResult , cyphers);
    }

    private GraphSearchResponseDto executeSavedQuery(CypherBlock block, int limit) {
        Map<String, Object> contentMap = block.getSavedQueryContent();
        String rawQuery = "";

        if (contentMap != null && contentMap.containsKey("cypherQuery")) {
            rawQuery = (String) contentMap.get("cypherQuery");
        }

        if (rawQuery == null || rawQuery.trim().isEmpty()) {
            throw new IllegalArgumentException("저장된 쿼리 내용이 없습니다.");
        }

        if (limit > 0 && !rawQuery.toLowerCase().contains("limit")) {
            rawQuery += " LIMIT " + limit;
        }

        log.info("Executing Saved Query: {}", rawQuery);

        Collection<Map<String, Object>> queryResult = neo4jClient.query(rawQuery)
                .fetch()
                .all();

        return convertToGroupData(queryResult,null);
    }

     

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
        }
         
        else if (from instanceof Relationship rel) {
            return switch (direction) {
                case "OUT" -> rel.relationshipTo(to, label).named(relName);
                case "IN" -> rel.relationshipFrom(to, label).named(relName);
                default -> rel.relationshipBetween(to, label).named(relName);
            };
        }
        else if (from instanceof RelationshipChain chain) {
            return switch (direction) {
                case "OUT" -> chain.relationshipTo(to, label).named(relName);
                case "IN" -> chain.relationshipFrom(to, label).named(relName);
                default -> chain.relationshipBetween(to, label).named(relName);
            };
        }
        throw new IllegalArgumentException("지원하지 않는 경로 타입입니다: " + from.getClass().getName());
    }

    private void collectConditions(PropertyContainer container, CypherBlock block, List<Condition> conditions,boolean caseInsensitive) {
        if (block.getProperties() == null || block.getProperties().isEmpty()) return;

        block.getProperties().forEach((key, val) -> {
            Property property = container.property(key);
            String operator = "EQUALS";
            Object value = val;

            if (val instanceof Map<?, ?> valMap) {
                if (valMap.containsKey("value")) value = valMap.get("value");
                if (valMap.containsKey("operator")) operator = (String) valMap.get("operator");
            }
            conditions.add(buildCondition(property, operator, value,caseInsensitive));
        });
    }

    private Condition buildCondition(Property property, String operator, Object value, boolean caseInsensitive) {
        if ("IS_NULL".equals(operator)) return property.isNull();
        if ("IS_NOT_NULL".equals(operator)) return property.isNotNull();

         
        if (caseInsensitive && value instanceof String strValue) {
            Expression propertyLower = Cypher.toLower(property);
            Expression valueLower = Cypher.literalOf(strValue.toLowerCase());

            return switch (operator) {
                case "NOT_EQUALS" -> propertyLower.isNotEqualTo(valueLower);
                case "CONTAINS" -> propertyLower.contains(valueLower);
                case "STARTS_WITH" -> propertyLower.startsWith(valueLower);
                case "ENDS_WITH" -> propertyLower.endsWith(valueLower);
                case "GREATER_THAN" -> propertyLower.gt(valueLower);
                case "LESS_THAN" -> propertyLower.lt(valueLower);
                default -> propertyLower.isEqualTo(valueLower);
            };
        }

         
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

    private GraphSearchResponseDto convertToGroupData(Collection<Map<String, Object>> queryResult ,List<CypherBlock> cyphers) {
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

        enrichWithGlobalConnectivity(nodeList,cyphers);

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

         
        if (item instanceof org.neo4j.driver.types.Path) {
            org.neo4j.driver.types.Path path = (org.neo4j.driver.types.Path) item;
            path.nodes().forEach(node -> processNode(node, nodeList, visitedNodeIds, styleCache, nodeInfoMap));
            path.relationships().forEach(rel -> processRelationship(rel, edgeList, visitedEdgeIds, styleCache, nodeInfoMap));
        }
         
        else if (item instanceof org.neo4j.driver.types.Node) {
            processNode((org.neo4j.driver.types.Node) item, nodeList, visitedNodeIds, styleCache, nodeInfoMap);
        }
         
        else if (item instanceof org.neo4j.driver.types.Relationship) {
            processRelationship((org.neo4j.driver.types.Relationship) item, edgeList, visitedEdgeIds, styleCache, nodeInfoMap);
        }
        else if (item instanceof List<?>) {
            for (Object subItem : (List<?>) item) {
                processResultItem(subItem, nodeList, edgeList, visitedNodeIds, visitedEdgeIds, styleCache, nodeInfoMap);
            }
        }
    }

    private void processNode(org.neo4j.driver.types.Node node,
                             List<Map<String, Object>> nodeList,
                             Set<String> visitedNodeIds,
                             Map<String, Map<String, Object>> styleCache,
                             Map<String, Map<String, Object>> nodeInfoMap) {

        String id = node.elementId();
        String label = node.labels().iterator().hasNext() ? node.labels().iterator().next() : "Unknown";

         
        Map<String, Object> style = graphUtil.getStyleConfig(label, "NODE", styleCache);

         
        List<String> displayCaptions = new ArrayList<>();
        Map<String, Object> nodeProps = node.asMap();

        if (style != null && style.containsKey("captions")) {
            List<Map<String, Object>> captions = (List<Map<String, Object>>) style.get("captions");

            for (Map<String, Object> config : captions) {
                String propertyKey = (String) config.get("property");
                boolean showOnNode = (boolean) config.getOrDefault("showOnNode", false);

                if (showOnNode) {
                    if ("nodeLabel".equals(propertyKey)) {
                         
                        displayCaptions.add(label);
                    } else if (nodeProps.containsKey(propertyKey)) {
                         
                        Object value = nodeProps.get(propertyKey);
                        if (value != null) {
                            displayCaptions.add(value.toString());
                        }
                    }
                }
            }
        }




         
         

         
        String finalDisplayLabel = String.join(",", displayCaptions);

        Map<String, Object> info = new HashMap<>();
        info.put("label", label);
        if (style != null) info.put("style", style);
        nodeInfoMap.put(id, info);

        if (!visitedNodeIds.contains(id)) {
            visitedNodeIds.add(id);
            Map<String, Object> nodeData = new HashMap<>(nodeProps);
            nodeData.put("id", id);
            nodeData.put("label", label);
            nodeData.put("displayLabel", finalDisplayLabel);  
            if (style != null) nodeData.put("style", style);
            nodeList.add(nodeData);
        }
    }

    private void processRelationship(org.neo4j.driver.types.Relationship rel,
                                     List<Map<String, Object>> edgeList,
                                     Set<String> visitedEdgeIds,
                                     Map<String, Map<String, Object>> styleCache,
                                     Map<String, Map<String, Object>> nodeInfoMap) {

        String id = rel.elementId();

        if (!visitedEdgeIds.contains(id)) {
            visitedEdgeIds.add(id);
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

     
    private void enrichWithGlobalConnectivity(List<Map<String, Object>> nodeList, List<CypherBlock> cyphers) {
        if (nodeList.isEmpty()) return;

         
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

}