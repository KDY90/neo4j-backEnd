package com.empasy.graph.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

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

    // 🌟 변경된 부분: Map 대신 명확한 DTO 리스트 사용
    @Schema(title = "cypherBlocks", description = "관계 필터링 블록")
    private List<CypherBlockDto> cypherBlocks;

    @Schema(title = "CypherBlock type", description = "CypherBlock type")
    private String type;
}