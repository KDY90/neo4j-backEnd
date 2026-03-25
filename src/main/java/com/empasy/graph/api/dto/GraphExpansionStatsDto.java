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
public class GraphExpansionStatsDto {
    @Schema(title = "릴레이션", description = "릴레이션")
    private List<ExpansionItemDto> relationships;
    @Schema(title = "카테고리", description = "카테고리")
    private List<ExpansionItemDto> categories;
    @Schema(title = "페어", description = "방향")
    private List<ExpansionItemDto> pairs;

    @Getter
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpansionItemDto {
        @Schema(title = "id", description = "id")
        private String id;
        @Schema(title = "라벨", description = "라벨")
        private String label;
        @Schema(title = "타겟 라벨", description = "타겟 라벨")
        private String targetLabel;
        @Schema(title = "갯수", description = "갯수")
        private long count;
        @Schema(title = "디렉션", description = "방향")
        private String direction;

    }
}
