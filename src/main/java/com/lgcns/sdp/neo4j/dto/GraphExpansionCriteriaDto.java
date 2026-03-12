package com.lgcns.sdp.neo4j.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GraphExpansionCriteriaDto {
    @Schema(title = "릴레이션", description = "릴레이션")
    private String relation;
    @Schema(title = "다이렉션", description = "방향")
    private String direction;
    @Schema(title = "타켓 라벨", description = "타켓 라벨")
    private String targetLabel;  
}
