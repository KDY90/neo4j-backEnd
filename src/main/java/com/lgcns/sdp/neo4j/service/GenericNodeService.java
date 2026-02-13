package com.lgcns.sdp.neo4j.service;

import com.lgcns.sdp.neo4j.dto.GraphCreateNodeRequestDto;
import com.lgcns.sdp.neo4j.dto.GraphCreateNodeResponseDto;
import com.lgcns.sdp.neo4j.util.GraphUtil;
import lombok.RequiredArgsConstructor;
import org.neo4j.driver.types.Node;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GenericNodeService {

    private final Neo4jClient neo4jClient;
    private final GraphUtil graphUtil;

    public Collection<Map<String, Object>> findAllByLabel(String label) {
        String query = String.format("MATCH (n:%s) RETURN n{.*, id: elementId(n)} as data", label);
        return neo4jClient.query(query).fetch().all();
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
