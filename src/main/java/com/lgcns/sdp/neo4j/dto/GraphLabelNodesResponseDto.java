package com.lgcns.sdp.neo4j.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphLabelNodesResponseDto {
    private List<Map<String, Object>> data;
    private long rowCount;
}
