package com.lgcns.sdp.neo4j.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphSearchBarDto {

    private List<NodeSchema> nodes;
    private List<RelationshipSchema> relationships;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor  
    public static class NodeSchema {
        private String label;
        private List<PropertySchema> properties;

         
        private Map<String, Object> style;

         
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor  
    public static class RelationshipSchema {
        private String relationship;
        private List<ConnectionSchema> connections;

         
        private Map<String, Object> style;

         
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
    public static class ConnectionSchema {
        private String header;
        private String tail;
    }
}