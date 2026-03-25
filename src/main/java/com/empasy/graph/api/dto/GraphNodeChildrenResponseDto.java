package com.empasy.graph.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphNodeChildrenResponseDto {
    @Schema(title = "data", description = "노드 리스트 로우의 하위 데이터")
    private List<Map<String, Object>> data;
}

