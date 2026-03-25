package com.empasy.graph.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphSearchBarDto {
    @Schema(title = "노드", description = "노드")
    private List<NodeSchema> nodes;
    @Schema(title = "릴레이션", description = "릴레이션")
    private List<RelationshipSchema> relationships;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor  
    public static class NodeSchema {
        @Schema(title = "label", description = "label")
        private String label;
        @Schema(title = "properties", description = "properties")
        private List<PropertySchema> properties;
        @Schema(title = "노드 style", description = "노드 style")
        private Map<String, Object> style;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor  
    public static class RelationshipSchema {
        @Schema(title = "릴레이션", description = "릴레이션")
        private String relationship;
        @Schema(title = "connections", description = "연결된 connections")
        private List<ConnectionSchema> connections;
        @Schema(title = "릴레이션 style", description = "릴레이션 style")
        private Map<String, Object> style;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PropertySchema {
        @Schema(title = "property key", description = "property key")
        private String key;
        @Schema(title = "property type", description = "property type")
        private String type;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConnectionSchema {
        @Schema(title = "header 부분", description = "header 부분")
        private String header;
        @Schema(title = "tail 부분", description = "tail 부분")
        private String tail;
    }
}
