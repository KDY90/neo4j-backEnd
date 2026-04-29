package com.empasy.graph.api.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import lombok.Builder;
import lombok.Getter;
import org.neo4j.driver.types.Node;

import java.util.HashMap;
import java.util.Map;

@Getter
@Builder
public class GraphNodeDto {
    private String id;
    private String label;
    private Boolean hasChildren;

    @JsonAnyGetter
    private Map<String, Object> properties;

    public static GraphNodeDto of(Node node, Boolean hasChildren) {
        if (node == null) return null;

        String label = node.labels().iterator().hasNext() ? node.labels().iterator().next() : "";

        return GraphNodeDto.builder()
                .id(node.elementId())
                .label(label)
                .hasChildren(hasChildren != null ? hasChildren : false)
                .properties(new HashMap<>(node.asMap()))
                .build();
    }
}