package com.empasy.graph.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphLabelNodesResponseDto {
    @Schema(title = "data", description = "노드 리스트 data")
    private List<GraphNodeDto> data;
    @Schema(title = "rowCount", description = "로우 갯수")
    private long rowCount;
}

