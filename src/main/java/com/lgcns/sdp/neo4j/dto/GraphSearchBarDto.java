package com.lgcns.sdp.neo4j.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
    public static class NodeSchema {
        private String label;
        private List<PropertySchema> properties;

        @JsonIgnore
        private Map<String, Object> style;

        public NodeSchema(String label, List<PropertySchema> properties, Map<String, Object> style) {
            this.label = label;
            this.properties = properties;
            this.style = style;
        }

        @JsonAnyGetter
        public Map<String, Object> getStyleMap() {
            return style;
        }
    }

    @Data
    @NoArgsConstructor
    public static class RelationshipSchema {
        private String relationship;
        private List<ConnectionSchema> connections;

        @JsonIgnore
        private Map<String, Object> style;

        public RelationshipSchema(String relationship, List<ConnectionSchema> connections, Map<String, Object> style) {
            this.relationship = relationship;
            this.connections = connections;
            this.style = style;
        }

        @JsonAnyGetter
        public Map<String, Object> getStyleMap() {
            return style;
        }
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