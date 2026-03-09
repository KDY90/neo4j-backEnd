package com.lgcns.sdp.neo4j.service;

import com.lgcns.sdp.neo4j.dto.GraphCreateNodeRequestDto;
import com.lgcns.sdp.neo4j.dto.GraphCreateNodeResponseDto;
import com.lgcns.sdp.neo4j.dto.GraphLabelNodesRequestDto;
import com.lgcns.sdp.neo4j.dto.GraphLabelNodesResponseDto;
import com.lgcns.sdp.neo4j.dto.GraphNodeChildrenResponseDto;
import com.lgcns.sdp.neo4j.util.GraphUtil;
import lombok.RequiredArgsConstructor;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.cypherdsl.core.renderer.Configuration;
import org.neo4j.cypherdsl.core.renderer.Dialect;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.neo4j.driver.types.Node;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class GenericNodeService {

    private final Neo4jClient neo4jClient;
    private final GraphUtil graphUtil;

    public Collection<Map<String, Object>> findAllByLabel(String label) {
        String query = String.format("MATCH (n:`%s`) RETURN n{.*, id: elementId(n)} as data", label);
        return neo4jClient.query(query).fetch().all();
    }

    public GraphLabelNodesResponseDto getNodesByLabelForTable(GraphLabelNodesRequestDto requestDto) {
        String label = requestDto.getLabel();
        if (label == null || label.trim().isEmpty()) {
            throw new IllegalArgumentException("Label cannot be empty");
        }

        int pageIndex = requestDto.getPageIndex();
        int pageSize = requestDto.getPageSize();
        int skip = pageIndex * pageSize;

        // Total Row Count Query using Cypher-DSL
        org.neo4j.cypherdsl.core.Node n = Cypher.node(label).named("n");
        Statement countStatement = Cypher.match(n)
                .returning(Cypher.count(n).as("total"))
                .build();
        String countQuery = Renderer.getRenderer(Configuration.newConfig().withDialect(Dialect.NEO4J_5).build()).render(countStatement);
        Long rowCount = neo4jClient.query(countQuery).fetchAs(Long.class).one().orElse(0L);

        // Data Query using Cypher-DSL
        org.neo4j.cypherdsl.core.Node root = Cypher.node(label).named("root");
        Statement dataStatement = Cypher.match(root)
                .returning(
                        root.asExpression(),
                        Cypher.exists(root.relationshipFrom(Cypher.anyNode())).as("hasChildren")
                )
                .skip(skip)
                .limit(pageSize)
                .build();
        String dataQuery = Renderer.getRenderer(Configuration.newConfig().withDialect(Dialect.NEO4J_5).build()).render(dataStatement);

        Collection<Map<String, Object>> rawResults = neo4jClient.query(dataQuery).fetch().all();

        List<Map<String, Object>> finalData = new ArrayList<>();

        for (Map<String, Object> row : rawResults) {
            Node rootNode = (Node) row.get("root");
            Boolean hasChildren = (Boolean) row.get("hasChildren");

            Map<String, Object> nodeData = new HashMap<>(rootNode.asMap());
            nodeData.put("id", rootNode.elementId());
            nodeData.put("label", rootNode.labels().iterator().hasNext() ? rootNode.labels().iterator().next() : "");
            nodeData.put("hasChildren", hasChildren);
            
            finalData.add(nodeData);
        }

        return GraphLabelNodesResponseDto.builder()
                .data(finalData)
                .rowCount(rowCount)
                .build();
    }

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

        List<Map<String, Object>> finalData = new ArrayList<>();
        for (Map<String, Object> row : rawResults) {
            Node childNode = (Node) row.get("child");
            Boolean hasChildren = (Boolean) row.get("hasChildren");

            Map<String, Object> nodeData = new HashMap<>(childNode.asMap());
            nodeData.put("id", childNode.elementId());
            nodeData.put("label", childNode.labels().iterator().hasNext() ? childNode.labels().iterator().next() : "");
            nodeData.put("hasChildren", hasChildren);

            finalData.add(nodeData);
        }

        return GraphNodeChildrenResponseDto.builder()
                .data(finalData)
                .build();
    }


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

    public GraphCreateNodeResponseDto updateNode(String elementId, Map<String, Object> properties) {
         
        if (properties.containsKey("labels")) {
            properties.remove("labels");
        }

        String query = "MATCH (n) WHERE elementId(n) = $elementId SET n += $props RETURN n";

        return neo4jClient.query(query)
                .bind(elementId).to("elementId")
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
                .orElseThrow(() -> new RuntimeException("Failed to update node with id: " + elementId));
    }

    public void deleteNode(String elementId) {
        String query = "MATCH (n) WHERE elementId(n) = $elementId DETACH DELETE n";
        neo4jClient.query(query)
                .bind(elementId).to("elementId")
                .run();
    }
}
