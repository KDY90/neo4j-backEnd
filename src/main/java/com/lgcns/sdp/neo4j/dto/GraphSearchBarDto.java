package com.lgcns.sdp.neo4j.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphSearchBarDto {

    private List<NodeSchema> nodes;
    private List<RelationshipSchema> relationships;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeSchema {
        private String label;
        private List<PropertySchema> properties;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PropertySchema {
        private String key;
        private String type;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelationshipSchema {
        private String relationship;

        private List<ConnectionSchema> list;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConnectionSchema {
        private String header;
        private String tail;
    }
}