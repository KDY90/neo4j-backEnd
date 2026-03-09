package com.lgcns.sdp.neo4j.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GraphLabelNodesRequestDto {
    private String label;
    private int pageIndex = 0;
    private int pageSize = 10;
}
