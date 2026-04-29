package com.empasy.graph.api.repository;

import com.empasy.graph.api.dto.*;
import com.empasy.graph.api.util.GraphUtil;
import lombok.RequiredArgsConstructor;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.cypherdsl.core.renderer.Configuration;
import org.neo4j.cypherdsl.core.renderer.Dialect;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.neo4j.driver.types.Node;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class GenericNodeRepositoryImpl implements GenericNodeRepository {

    private final Neo4jClient neo4jClient;
    private final GraphUtil graphUtil;

    @Override
    public Collection<Map<String, Object>> findAllByLabel(String label) {
        String query = String.format("MATCH (n:%s) RETURN n{.*, id: elementId(n)} as data", label);
        return neo4jClient.query(query).fetch().all();
    }

    @Override
    public GraphLabelNodesResponseDto getNodesByLabelForTable(GraphLabelNodesRequestDto requestDto) {
        String label = requestDto.getLabel();
        if (label == null || label.trim().isEmpty()) {
            throw new IllegalArgumentException("Label cannot be empty");
        }

        int pageIndex = requestDto.getPageIndex();
        int pageSize = requestDto.getPageSize();
        int skip = pageIndex * pageSize;

        List<CypherBlockDto> blocks = requestDto.getCypherBlocks();

        boolean isSavedQuery = false;
        String savedCypherQuery = null;

        if (blocks != null) {
            Optional<CypherBlockDto> savedQueryBlock = blocks.stream()
                    .filter(block -> "SAVED_QUERY".equals(block.getType()))
                    .findFirst();

            if (savedQueryBlock.isPresent()) {
                isSavedQuery = true;
                SavedQueryContentDto savedContent = savedQueryBlock.get().getSavedQueryContent();
                savedCypherQuery = savedContent.getCypherQuery();
                String inputValue = savedContent.getInputValue();

                if (inputValue != null && !inputValue.trim().isEmpty()) {

                    boolean isNumeric = inputValue.matches("-?\\d+(\\.\\d+)?");
                    boolean isArray = inputValue.startsWith("[") && inputValue.endsWith("]");
                    boolean isAlreadyQuoted = (inputValue.startsWith("\"") && inputValue.endsWith("\"")) ||
                            (inputValue.startsWith("'") && inputValue.endsWith("'"));

                    String replacementValue;

                    if (isNumeric || isArray || isAlreadyQuoted) {
                        replacementValue = inputValue;
                    } else {
                        replacementValue = "\"" + inputValue + "\"";
                    }

                    savedCypherQuery = savedCypherQuery.replaceAll("\\$\\w+", java.util.regex.Matcher.quoteReplacement(replacementValue));
                }
            }
        }

        String countQuery;
        String dataQuery;

        if (isSavedQuery) {
            countQuery = "CALL () {\n" + savedCypherQuery + "\n} RETURN count(*) AS total";
            dataQuery = "CALL () {\n" + savedCypherQuery + "\n} RETURN * SKIP " + skip + " LIMIT " + pageSize;
        } else if (blocks != null && !blocks.isEmpty()) {
            StringBuilder matchClause = new StringBuilder("MATCH ");
            String targetVar = "n0";

            for (int i = 0; i < blocks.size(); i++) {
                CypherBlockDto block = blocks.get(i);
                String type = block.getType();
                String blockLabel = block.getLabel();

                if ("NODE".equals(type) || i % 2 == 0) {
                    matchClause.append("(n").append(i);
                    if (blockLabel != null && !"ANY".equals(blockLabel)) {
                        matchClause.append(":`").append(blockLabel).append("`");
                    }
                    matchClause.append(")");

                    if (label.equals(blockLabel)) {
                        targetVar = "n" + i;
                    }
                } else if ("RELATIONSHIP".equals(type) || i % 2 != 0) {
                    String direction = block.getDirection();
                    String relTypeStr = (blockLabel != null && !"ANY".equals(blockLabel)) ? ":`" + blockLabel + "`" : "";

                    if ("OUT".equalsIgnoreCase(direction)) {
                        matchClause.append("-[r").append(i).append(relTypeStr).append("]->");
                    } else if ("IN".equalsIgnoreCase(direction)) {
                        matchClause.append("<-[r").append(i).append(relTypeStr).append("]-");
                    } else {
                        matchClause.append("-[r").append(i).append(relTypeStr).append("]-");
                    }
                }
            }
            countQuery = matchClause + " RETURN count(DISTINCT " + targetVar + ") AS total";
            dataQuery = matchClause +
                    " WITH DISTINCT " + targetVar + " AS root " +
                    " RETURN root, exists((root)<-[]-()) AS hasChildren " +
                    " SKIP " + skip + " LIMIT " + pageSize;
        } else {
            countQuery = "MATCH (n:`" + label + "`) RETURN count(n) AS total";
            dataQuery = "MATCH (root:`" + label + "`) RETURN root, exists((root)<-[]-()) AS hasChildren SKIP " + skip + " LIMIT " + pageSize;
        }

        Long rowCount = 0L;
        if (countQuery != null) {
            rowCount = neo4jClient.query(countQuery).fetchAs(Long.class).one().orElse(0L);
        }

        Collection<Map<String, Object>> rawResults = neo4jClient.query(dataQuery).fetch().all();

        List<GraphNodeDto> finalData = new ArrayList<>();

        for (Map<String, Object> row : rawResults) {
            Node rootNode = null;

            for (Object value : row.values()) {
                if (value instanceof Node) {
                    Node tempNode = (Node) value;

                    boolean hasTargetLabel = false;
                    for (String nodeLabel : tempNode.labels()) {
                        if (nodeLabel.equals(label)) {
                            hasTargetLabel = true;
                            break;
                        }
                    }
                    if (hasTargetLabel) {
                        rootNode = tempNode;
                        break;
                    }
                }
            }
            if (rootNode != null) {
                Boolean hasChildren = row.get("hasChildren") != null ? (Boolean) row.get("hasChildren") : false;
                finalData.add(GraphNodeDto.of(rootNode, hasChildren));
            }
        }

        return GraphLabelNodesResponseDto.builder()
                .data(finalData)
                .rowCount(rowCount)
                .build();
    }

    @Override
    public GraphNodeChildrenResponseDto getChildrenNodes(String elementId) {
        if (elementId == null || elementId.trim().isEmpty()) {
            throw new IllegalArgumentException("elementId cannot be empty");
        }

        org.neo4j.cypherdsl.core.Node parent = Cypher.anyNode("parent");
        org.neo4j.cypherdsl.core.Node child = Cypher.anyNode("child");

        Statement statement = Cypher.match(parent.relationshipFrom(child))
                .where(parent.elementId().isEqualTo(Cypher.parameter("elementId")))
                .returning(
                        child.asExpression(),
                        Cypher.exists(child.relationshipFrom(Cypher.anyNode())).as("hasChildren")
                )
                .build();

        String query = Renderer.getRenderer(Configuration.newConfig().withDialect(Dialect.NEO4J_5).build()).render(statement);

        Collection<Map<String, Object>> rawResults = neo4jClient.query(query)
                .bind(elementId).to("elementId")
                .fetch().all();

        List<GraphNodeDto> finalData = new ArrayList<>();
        for (Map<String, Object> row : rawResults) {
            Node childNode = (Node) row.get("child");
            Boolean hasChildren = (Boolean) row.get("hasChildren");
            finalData.add(GraphNodeDto.of(childNode, hasChildren));
        }

        return GraphNodeChildrenResponseDto.builder()
                .data(finalData)
                .build();
    }

    @Override
    public GraphCreateNodeResponseDto createNode(GraphCreateNodeRequestDto requestDto) {
        String label = requestDto.getLabel();
        Map<String, Object> properties = requestDto.getProperties();

        if (label == null || label.trim().isEmpty()) {
            throw new IllegalArgumentException("Label cannot be empty");
        }

        String query = String.format("CREATE (n:`%s` $props) RETURN n", label);

        return neo4jClient.query(query)
                .bind(properties).to("props")
                .fetchAs(GraphCreateNodeResponseDto.class)
                .mappedBy((typeSystem, record) -> {
                    Node node = record.get("n").asNode();
                    String nodeLabel = node.labels().iterator().next();

                    Map<String, Object> styleConfig = graphUtil.getStyleConfig(
                            nodeLabel,
                            "NODE",
                            new HashMap<>());

                    return GraphCreateNodeResponseDto.builder()
                            .elementId(node.elementId())
                            .label(nodeLabel)
                            .properties(node.asMap())
                            .style(styleConfig)
                            .build();
                })
                .one()
                .orElseThrow(() -> new RuntimeException("Failed to create node"));
    }

    @Override
    public GraphCreateNodeResponseDto updateNode(String elementId, GraphUpdateNodeRequestDto requestDto) {
        requestDto.getProperties().remove("labels");

        String query = "MATCH (n) WHERE elementId(n) = $elementId SET n += $props RETURN n";

        return neo4jClient.query(query)
                .bind(elementId).to("elementId")
                .bind(requestDto.getProperties()).to("props")
                .fetchAs(GraphCreateNodeResponseDto.class)
                .mappedBy((typeSystem, record) -> {
                    Node node = record.get("n").asNode();
                    String nodeLabel = node.labels().iterator().next();

                    Map<String, Object> styleConfig = graphUtil.getStyleConfig(
                            nodeLabel,
                            "NODE",
                            new HashMap<>());

                    return GraphCreateNodeResponseDto.builder()
                            .elementId(node.elementId())
                            .label(nodeLabel)
                            .properties(node.asMap())
                            .style(styleConfig)
                            .build();
                })
                .one()
                .orElseThrow(() -> new RuntimeException("Failed to update node with id: " + elementId));
    }

    @Override
    public void deleteNode(String elementId) {
        String query = "MATCH (n) WHERE elementId(n) = $elementId DETACH DELETE n";
        neo4jClient.query(query)
                .bind(elementId).to("elementId")
                .run();
    }
}
