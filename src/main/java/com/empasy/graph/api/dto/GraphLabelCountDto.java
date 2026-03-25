package com.empasy.graph.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record GraphLabelCountDto(
        @Schema(title = "라벨", description = "라벨")
        String label,
        @Schema(title = "카운트", description = "갯수")
        Long count) {
}
