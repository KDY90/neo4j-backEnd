package com.lgcns.sdp.neo4j.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphLabelNodesRequestDto {
    private String label;
    private int pageIndex = 0;
    private int pageSize = 10;
}
