package com.empasy.graph.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GetSpecificNodeNeighborsBatchDto {
    private List<GraphExpansionCriteriaDto> criteriaList;
}
