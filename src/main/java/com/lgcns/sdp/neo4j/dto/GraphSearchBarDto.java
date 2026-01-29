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
    @AllArgsConstructor // [추가] 생성자 자동 생성 사용
    public static class NodeSchema {
        private String label;
        private List<PropertySchema> properties;

        // [수정] @JsonIgnore 제거: 이제 이 필드가 JSON 결과에 "style": {...} 로 나옵니다.
        private Map<String, Object> style;

        // [삭제] 기존 수동 생성자 및 @JsonAnyGetter 메서드는 필요 없으므로 삭제
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor // [추가]
    public static class RelationshipSchema {
        private String relationship;
        private List<ConnectionSchema> connections;

        // [수정] @JsonIgnore 제거
        private Map<String, Object> style;

        // [삭제] 기존 수동 생성자 및 @JsonAnyGetter 메서드 삭제
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