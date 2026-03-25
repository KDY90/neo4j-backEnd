package com.empasy.graph.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.Map;

@Builder
public record GraphSchemaDto(
        @Schema(title = "스키마 label", description = "스키마 label")
        String label,
        @Schema(title = "스키마 properties", description = "스키마 properties")
        Map<String, String> properties) {
}
