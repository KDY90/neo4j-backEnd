package com.lgcns.sdp.neo4j.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class GraphSearchResponseDto {
    private List<Map<String, Object>> nodes;
    private List<Map<String, Object>> relationships;
}