package com.empasy.graph.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class GraphDetailDto {
    @Schema(title = "센터노드", description = "센터노드")
    private Map<String, Object> centerNode;
    @Schema(title = "노드", description = "노드")
    private List<Map<String, Object>> nodes;
    @Schema(title = "릴레이션", description = "릴레이션")
    private List<Map<String, Object>> relationships;
    @Schema(title = "노드 총 갯수", description = "노드 총 갯수")
    private Map<String, Long> nodeCount;
    @Schema(title = "릴레이션 총 갯수", description = "릴레이션 총 갯수")
    private Map<String, Long> relationCount;
}
