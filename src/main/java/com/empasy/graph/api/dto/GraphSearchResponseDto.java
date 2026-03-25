package com.empasy.graph.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;
import java.util.Map;

import lombok.Builder;

@Getter
@Builder
public class GraphSearchResponseDto {
    @Schema(title = "nodes", description = "노드")
    private List<Map<String, Object>> nodes;
    @Schema(title = "relationships", description = "릴레이션")
    private List<Map<String, Object>> relationships;
    @Schema(title = "nodeStyles", description = "노드 스타일")
    private Map<String, Object> nodeStyles;
    @Schema(title = "relationshipStyles", description = "릴레이션 스타일")
    private Map<String, Object> relationshipStyles;
    @Schema(title = "nodeCount", description = "노드 갯수")
    private Map<String, Long> nodeCount;
    @Schema(title = "relationCount", description = "릴레이션 갯수")
    private Map<String, Long> relationCount;

}
