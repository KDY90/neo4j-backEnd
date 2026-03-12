package com.lgcns.sdp.neo4j.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphStyleRequestDto {
    @Schema(title = "label", description = "노드")
    private String label;
    @Schema(title = "elementType", description = "엘리먼트 타입")
    private String elementType;
    @Schema(title = "styleConfig", description = "스타일 JSON")
    private Map<String, Object> styleConfig;
}

