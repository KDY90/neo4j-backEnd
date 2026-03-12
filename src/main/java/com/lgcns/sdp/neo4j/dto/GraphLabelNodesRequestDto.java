package com.lgcns.sdp.neo4j.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
}

