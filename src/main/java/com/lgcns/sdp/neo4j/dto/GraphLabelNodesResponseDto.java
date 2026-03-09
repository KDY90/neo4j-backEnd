package com.lgcns.sdp.neo4j.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphLabelNodesResponseDto {
    private List<Map<String, Object>> data;
    private long rowCount;
}
