package com.lgcns.sdp.neo4j.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class GraphSearchResponseDto {
    private String group; // "nodes" 또는 "edges"
    private List<Map<String, Object>> data; // 노드/엣지 데이터 리스트
}