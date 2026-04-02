package com.empasy.graph.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphLabelNodesRequestDto {
    @Schema(title = "라벨", description = "라벨")
    private String label;
    @Schema(title = "pageIndex", description = "pageIndex")
    private int pageIndex = 0;
    @Schema(title = "pageSize", description = "pageSize")
    private int pageSize = 10;
    @Schema(title = "cypherBlocks", description = "관계 필터링 블록")
    private List<Map<String, Object>> cypherBlocks;
}

