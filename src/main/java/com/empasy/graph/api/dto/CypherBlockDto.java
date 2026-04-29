package com.empasy.graph.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CypherBlockDto {
    private String type;
    private String label;
    private String id;
    private String direction;

    private SavedQueryContentDto savedQueryContent;
}