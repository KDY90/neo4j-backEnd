package com.empasy.graph.api.service;

import com.empasy.graph.api.dto.GraphSearchRequestDto;
import com.empasy.graph.api.dto.GraphSearchRequestDto.CypherBlock;
import com.empasy.graph.api.dto.GraphSearchResponseDto;
import com.empasy.graph.api.entity.GraphCypherQuery;
import com.empasy.graph.api.repository.GraphCypherQueryRepository;
import com.empasy.graph.api.util.GraphUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.cypherdsl.core.*;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.Set;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphSearchService {

    private final Neo4jClient neo4jClient;
    private final GraphUtil graphUtil;
    private final GraphCypherQueryRepository graphCypherQueryRepository;

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
            return executeSavedQuery(savedQueryBlock.get(), limit);
        }

        List<Condition> whereConditions = new ArrayList<>();
        CypherBlock firstBlock = cyphers.get(0);
        Node rootNode = createDslNode(firstBlock, 0);

        collectConditions(rootNode, firstBlock, whereConditions, requestDto.isCaseInsensitiveSearch());

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

            collectConditions(relProxy, relBlock, whereConditions, requestDto.isCaseInsensitiveSearch());
            collectConditions(nextNode, nextNodeBlock, whereConditions, requestDto.isCaseInsensitiveSearch());
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

        List<Expression> countExpressions = new ArrayList<>();
        for (int i = 0; i < cyphers.size(); i++) {
            String varName = (i % 2 == 0) ? "n" + i : "r" + i;
            countExpressions.add(Cypher.countDistinct(Cypher.name(varName)).as("count_" + i));
        }

        Statement countStatement = Cypher.match(finalPattern)
                .where(finalCondition)
                .returning(countExpressions.toArray(new Expression[0]))
                .build();

        String countQueryString = Renderer.getDefaultRenderer().render(countStatement);
        Map<String, Object> countResult = neo4jClient.query(countQueryString)
                .bindAll(countStatement.getCatalog().getParameters())
                .fetch()
                .one()
                .orElse(Collections.emptyMap());

        return convertToGroupData(queryResult, cyphers, countResult);
    }

    private GraphSearchResponseDto executeSavedQuery(CypherBlock block, int limit) {
        Map<String, Object> contentMap = block.getSavedQueryContent();

        if (contentMap == null || !contentMap.containsKey("id")) {
            throw new IllegalArgumentException("저장된 쿼리의 ID(PK)가 없습니다.");
        }

        Long queryId = ((Number) contentMap.get("id")).longValue();

        String queryType = "cypher";
        if (contentMap.containsKey("queryType") && contentMap.get("queryType") != null) {
            queryType = String.valueOf(contentMap.get("queryType")).trim();
        }

        GraphCypherQuery cypherQueryEntity = graphCypherQueryRepository.findByIdAndQueryType(queryId, queryType)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID와 일치하는 쿼리를 찾을 수 없습니다: " + queryId));

        String rawQuery = cypherQueryEntity.getCypherQuery();

        if (rawQuery == null || rawQuery.trim().isEmpty()) {
            throw new IllegalArgumentException("조회된 쿼리 내용이 없습니다.");
        }

        if (contentMap.containsKey("inputValue") && contentMap.get("inputValue") != null) {
            String inputValue = String.valueOf(contentMap.get("inputValue")).trim();

            if (!inputValue.isEmpty()) {
                boolean isNumeric = inputValue.matches("-?\\d+(\\.\\d+)?");

                boolean isArray = inputValue.startsWith("[") && inputValue.endsWith("]");

                String replacementValue = (isNumeric || isArray) ? inputValue : "\"" + inputValue + "\"";

                rawQuery = rawQuery.replaceAll("\\$\\w+", replacementValue);
            }
        }

        if (limit > 0 && !rawQuery.toLowerCase().contains("limit")) {
            rawQuery += " LIMIT " + limit;
        }

        Collection<Map<String, Object>> queryResult = neo4jClient.query(rawQuery)
                .fetch()
                .all();

        return convertToGroupData(queryResult, null, null);
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
        } else if (from instanceof Relationship rel) {
            return switch (direction) {
                case "OUT" -> rel.relationshipTo(to, label).named(relName);
                case "IN" -> rel.relationshipFrom(to, label).named(relName);
                default -> rel.relationshipBetween(to, label).named(relName);
            };
        } else if (from instanceof RelationshipChain chain) {
            return switch (direction) {
                case "OUT" -> chain.relationshipTo(to, label).named(relName);
                case "IN" -> chain.relationshipFrom(to, label).named(relName);
                default -> chain.relationshipBetween(to, label).named(relName);
            };
        }
        throw new IllegalArgumentException("지원하지 않는 경로 타입입니다: " + from.getClass().getName());
    }

    private void collectConditions(PropertyContainer container, CypherBlock block, List<Condition> conditions, boolean caseInsensitive) {
        if (block.getProperties() == null || block.getProperties().isEmpty()) return;

        block.getProperties().forEach((key, val) -> {
            Property property = container.property(key);
            String operator = "EQUALS";
            Object value = val;
            String type = "String"; // 기본 타입

            if (val instanceof Map<?, ?> valMap) {
                if (valMap.containsKey("value")) value = valMap.get("value");
                if (valMap.containsKey("operator")) operator = (String) valMap.get("operator");

                if (valMap.containsKey("type") && valMap.get("type") != null) {
                    type = (String) valMap.get("type");
                }
            }

            Object castedValue;
            if (type.toLowerCase().contains("list") && "CONTAINS".equals(operator)) {
                String strVal = String.valueOf(value).trim();
                if (type.toLowerCase().contains("long")) {
                    try { castedValue = Long.valueOf(strVal); }
                    catch (Exception e) { castedValue = strVal; }
                } else {
                    castedValue = strVal;
                }
            } else {
                castedValue = castValueToType(value, type);
            }

            conditions.add(buildCondition(property, operator, castedValue, type, caseInsensitive));
        });
    }

    private Condition buildCondition(Property property, String operator, Object value, String type, boolean caseInsensitive) {
        if ("IS_NULL".equals(operator)) return property.isNull();
        if ("IS_NOT_NULL".equals(operator)) return property.isNotNull();

        boolean isListType = type != null && type.toLowerCase().contains("list");

        if (isListType && "CONTAINS".equals(operator)) {
            return Cypher.literalOf(value).in(property);
        }

        if ("IN_ARRAY".equals(operator)) {
            return Cypher.literalOf(value).in(property);
        }

        if (caseInsensitive && value instanceof String strValue) {
            Expression propertyLower = Cypher.toLower(property);
            Expression valueLower = Cypher.literalOf(strValue.toLowerCase());

            return switch (operator) {
                case "NOT_EQUALS" -> propertyLower.isNotEqualTo(valueLower);
                case "CONTAINS" -> propertyLower.contains(valueLower);
                case "STARTS_WITH" -> propertyLower.startsWith(valueLower);
                case "ENDS_WITH" -> propertyLower.endsWith(valueLower);
                case "GREATER_THAN", "AFTER" -> propertyLower.gt(valueLower);
                case "LESS_THAN", "BEFORE" -> propertyLower.lt(valueLower);
                case "GREATER_THAN_OR_EQUAL" -> propertyLower.gte(valueLower);
                case "LESS_THAN_OR_EQUAL" -> propertyLower.lte(valueLower);
                default -> propertyLower.isEqualTo(valueLower);
            };
        }

        Expression valExpr = Cypher.literalOf(value);
        return switch (operator) {
            case "NOT_EQUALS" -> property.isNotEqualTo(valExpr);
            case "GREATER_THAN", "AFTER" -> property.gt(valExpr);
            case "LESS_THAN", "BEFORE" -> property.lt(valExpr);
            case "GREATER_THAN_OR_EQUAL" -> property.gte(valExpr);
            case "LESS_THAN_OR_EQUAL" -> property.lte(valExpr);

            case "CONTAINS" -> property.contains(valExpr);
            case "STARTS_WITH" -> property.startsWith(valExpr);
            case "ENDS_WITH" -> property.endsWith(valExpr);
            default -> property.isEqualTo(valExpr);
        };
    }

    private GraphSearchResponseDto convertToGroupData(Collection<Map<String, Object>> queryResult, List<CypherBlock> cyphers, Map<String, Object> patternCountResult) {
        List<Map<String, Object>> nodeList = new ArrayList<>();
        List<Map<String, Object>> edgeList = new ArrayList<>();

        Set<String> visitedNodeIds = new HashSet<>();
        Set<String> visitedEdgeIds = new HashSet<>();

        Map<String, Object> globalNodeStyles = new HashMap<>();
        Map<String, Object> globalRelStyles = new HashMap<>();

        Map<String, Map<String, Object>> nodeInfoMap = new HashMap<>();
        Map<String, Map<String, Object>> dbStyleCache = new HashMap<>();

        for (Map<String, Object> row : queryResult) {
            for (Object value : row.values()) {
                processResultItem(value, nodeList, edgeList, visitedNodeIds, visitedEdgeIds,
                        dbStyleCache, globalNodeStyles, globalRelStyles, nodeInfoMap);
            }
        }

        enrichWithGlobalConnectivity(nodeList, cyphers);

        Map<String, Long> nodeCountMap = new HashMap<>();
        Map<String, Long> relationCountMap = new HashMap<>();

        if (patternCountResult != null && cyphers != null) {
            for (int i = 0; i < cyphers.size(); i++) {
                CypherBlock block = cyphers.get(i);

                long count = 0L;
                Object countObj = patternCountResult.get("count_" + i);
                if (countObj instanceof Number num) {
                    count = num.longValue();
                }

                String label = block.getLabel();
                if ("ANY".equals(label)) {
                    label = (i % 2 == 0) ? "Total Nodes" : "Total Relations";
                } else {
                    if (i % 2 == 0) {
                        String targetLabel = label;
                        boolean hasMatchedLabel = nodeList.stream()
                                .anyMatch(n -> targetLabel.equals(n.get("label")));

                        if (!hasMatchedLabel && !nodeList.isEmpty()) {
                            label = (String) nodeList.get(0).get("label");
                        }
                    } else {
                        String targetLabel = label;
                        boolean hasMatchedLabel = edgeList.stream()
                                .anyMatch(e -> targetLabel.equals(e.get("label")));

                        if (!hasMatchedLabel && !edgeList.isEmpty()) {
                            label = (String) edgeList.get(0).get("label");
                        }
                    }
                }

                if (i % 2 == 0) {
                    nodeCountMap.merge(label, count, Long::sum);
                } else {
                    relationCountMap.merge(label, count, Long::sum);
                }
            }
        } else {
            for (Map<String, Object> node : nodeList) {
                String nodeLabel = (String) node.get("label");
                if (nodeLabel != null) {
                    nodeCountMap.merge(nodeLabel, 1L, Long::sum);
                }
            }

            for (Map<String, Object> edge : edgeList) {
                String edgeLabel = (String) edge.get("label");
                if (edgeLabel != null) {
                    relationCountMap.merge(edgeLabel, 1L, Long::sum);
                }
            }
        }

        return GraphSearchResponseDto.builder()
                .nodes(nodeList)
                .relationships(edgeList)
                .nodeStyles(globalNodeStyles)
                .relationshipStyles(globalRelStyles)
                .nodeCount(nodeCountMap)
                .relationCount(relationCountMap)
                .build();
    }

    private void processResultItem(Object item,
                                   List<Map<String, Object>> nodeList,
                                   List<Map<String, Object>> edgeList,
                                   Set<String> visitedNodeIds,
                                   Set<String> visitedEdgeIds,
                                   Map<String, Map<String, Object>> dbStyleCache,
                                   Map<String, Object> globalNodeStyles,
                                   Map<String, Object> globalRelStyles,
                                   Map<String, Map<String, Object>> nodeInfoMap) {
        if (item == null) return;

        if (item instanceof org.neo4j.driver.types.Path path) {
            path.nodes().forEach(node -> processNode(node, nodeList, visitedNodeIds, dbStyleCache, globalNodeStyles, nodeInfoMap));
            path.relationships().forEach(rel -> processRelationship(rel, edgeList, visitedEdgeIds, dbStyleCache, globalRelStyles, nodeInfoMap));
        } else if (item instanceof org.neo4j.driver.types.Node node) {
            processNode(node, nodeList, visitedNodeIds, dbStyleCache, globalNodeStyles, nodeInfoMap);
        } else if (item instanceof org.neo4j.driver.types.Relationship rel) {
            processRelationship(rel, edgeList, visitedEdgeIds, dbStyleCache, globalRelStyles, nodeInfoMap);
        } else if (item instanceof List<?> list) {
            for (Object subItem : list) {
                processResultItem(subItem, nodeList, edgeList, visitedNodeIds, visitedEdgeIds, dbStyleCache, globalNodeStyles, globalRelStyles, nodeInfoMap);
            }
        }
    }

    private void processNode(org.neo4j.driver.types.Node node,
                             List<Map<String, Object>> nodeList,
                             Set<String> visitedNodeIds,
                             Map<String, Map<String, Object>> dbStyleCache,
                             Map<String, Object> globalNodeStyles,
                             Map<String, Map<String, Object>> nodeInfoMap) {
        String id = node.elementId();
        String label = node.labels().iterator().hasNext() ? node.labels().iterator().next() : "Unknown";
        Map<String, Object> style = graphUtil.getStyleConfig(label, "NODE", dbStyleCache);

        if (!nodeInfoMap.containsKey(id)) {
            Map<String, Object> info = new HashMap<>();
            info.put("label", label);
            info.put("style", style);
            nodeInfoMap.put(id, info);
        }

        if (style != null && !globalNodeStyles.containsKey(label)) {
            globalNodeStyles.put(label, style);
        }

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
                        if (value != null) displayCaptions.add(value.toString());
                    }
                }
            }
        }

        String finalDisplayLabel = String.join(",", displayCaptions);

        if (!visitedNodeIds.contains(id)) {
            visitedNodeIds.add(id);
            Map<String, Object> nodeData = new HashMap<>(nodeProps);
            nodeData.put("id", id);
            nodeData.put("label", label);
            nodeData.put("displayLabel", finalDisplayLabel);
            nodeList.add(nodeData);
        }
    }

    private void processRelationship(org.neo4j.driver.types.Relationship rel,
                                     List<Map<String, Object>> edgeList,
                                     Set<String> visitedEdgeIds,
                                     Map<String, Map<String, Object>> dbStyleCache,
                                     Map<String, Object> globalRelStyles,
                                     Map<String, Map<String, Object>> nodeInfoMap) {
        String id = rel.elementId();
        String label = rel.type();
        String sourceId = rel.startNodeElementId();
        String targetId = rel.endNodeElementId();

        Map<String, Object> style = graphUtil.getStyleConfig(label, "RELATIONSHIP", dbStyleCache);

        if (style != null && !globalRelStyles.containsKey(label)) {
            globalRelStyles.put(label, style);
        }

        if (!visitedEdgeIds.contains(id)) {
            visitedEdgeIds.add(id);
            Map<String, Object> relData = new HashMap<>(rel.asMap());

            if (style != null) relData.put("style", style);

            if (nodeInfoMap.containsKey(sourceId)) {
                relData.put("sourceLabel", nodeInfoMap.get(sourceId).get("label"));
                relData.put("sourceStyle", nodeInfoMap.get(sourceId).get("style"));
            }
            if (nodeInfoMap.containsKey(targetId)) {
                relData.put("targetLabel", nodeInfoMap.get(targetId).get("label"));
                relData.put("targetStyle", nodeInfoMap.get(targetId).get("style"));
            }

            relData.put("id", id);
            relData.put("source", rel.startNodeElementId());
            relData.put("target", rel.endNodeElementId());
            relData.put("label", label);

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

    private Object castValueToType(Object value, String type) {
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            return value;
        }

        String strValue = String.valueOf(value).trim();
        String lowerType = type.toLowerCase().replace("_", "");

        try {
            if (lowerType.equals("integer") || lowerType.equals("long") || lowerType.equals("int")) {
                return Long.valueOf(strValue);
            } else if (lowerType.equals("float") || lowerType.equals("double")) {
                return Double.valueOf(strValue);
            } else if (lowerType.equals("boolean")) {
                return Boolean.valueOf(strValue);
            } else if (lowerType.contains("list(string)")) {
                String cleanValue = strValue;

                if (cleanValue.startsWith("[") && cleanValue.endsWith("]")) {
                    cleanValue = cleanValue.substring(1, cleanValue.length() - 1);
                }

                return Arrays.stream(cleanValue.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
            }
            else if (lowerType.contains("list(long)")) {
                String cleanValue = strValue;

                if (cleanValue.startsWith("[") && cleanValue.endsWith("]")) {
                    cleanValue = cleanValue.substring(1, cleanValue.length() - 1);
                }

                return Arrays.stream(cleanValue.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Long::valueOf) // 핵심: 요소를 Long 타입으로 캐스팅
                        .toList();
            }
            else if (lowerType.equals("date") || lowerType.equals("localdate")) {
                if (strValue.contains("T")) {
                    strValue = strValue.split("T")[0];
                }
                return LocalDate.parse(strValue);
            }
            else if (lowerType.equals("datetime") || lowerType.equals("localdatetime")) {
                if (!strValue.contains("T")) {
                    strValue += "T00:00:00";
                }

                try {
                    return OffsetDateTime.parse(strValue).toZonedDateTime();
                } catch (DateTimeParseException e) {
                    return LocalDateTime.parse(strValue);
                }
            }
        } catch (NumberFormatException e) {
            log.warn("Value casting failed (NumberFormat) for value: '{}' and type: '{}'", strValue, type);
        } catch (DateTimeParseException e) {
            log.warn("Value casting failed (DateTimeFormat) for value: '{}' and type: '{}'", strValue, type);
        }

        return strValue;
    }

}