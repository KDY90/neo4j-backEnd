package com.empasy.graph.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedQueryContentDto {
    private Long id;
    private String title;
    private String cypherQuery;
    private String description;
    private String queryType;
    private String inputValue;
    private String createTimestamp;
    private String updateTimestamp;
}