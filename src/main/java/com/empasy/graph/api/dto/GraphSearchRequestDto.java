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
public class GraphSearchRequestDto {

    @Schema(title = "검색 cyphers param", description = "검색 cyphers param")
    private List<CypherBlock> cyphers;
    @Schema(title = "검색 limit", description = "검색 limit")
    private int limit;
    @Schema(title = "caseInsensitiveSearch", description = "대소문자 구분 유무")
    private boolean caseInsensitiveSearch;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CypherBlock {
        @Schema(title = "CypherBlock type", description = "CypherBlock type")
        private String type;
        @Schema(title = "CypherBlock label", description = "CypherBlock label")
        private String label;
        @Schema(title = "CypherBlock direction", description = "CypherBlock direction")
        private String direction;
        @Schema(title = "CypherBlock properties", description = "CypherBlock properties")
        private Map<String, Object> properties;
        @Schema(title = "CypherBlock savedQueryContent", description = "CypherBlock savedQueryContent")
        private Map<String, Object> savedQueryContent;
    }
}
