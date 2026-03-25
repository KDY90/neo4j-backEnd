package com.empasy.graph.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GraphCreateNodeRequestDto {
    @Schema(title = "라벨", description = "라벨")
    private String label;
    @Schema(title = "프로퍼티스", description = "프로퍼티스")
    private Map<String, Object> properties;
}

