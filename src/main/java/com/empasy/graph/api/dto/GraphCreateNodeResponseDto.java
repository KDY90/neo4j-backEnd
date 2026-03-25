package com.empasy.graph.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class GraphCreateNodeResponseDto {
    @Schema(title = "엘리먼트 ID", description = "엘리먼트 ID")
    private String elementId;
    @Schema(title = "라벨", description = "라벨")
    private String label;
    @Schema(title = "프로퍼티스", description = "프로퍼티스")
    private Map<String, Object> properties;
    @Schema(title = "스타일", description = "스타일")
    private Map<String, Object> style;
}

