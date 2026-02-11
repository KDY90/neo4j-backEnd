package com.lgcns.sdp.neo4j.dto;

import lombok.Data;

@Data
public class GraphExpansionCriteriaDto {
    private String relation;    // 관계 타입 (null이면 전체)
    private String direction;   // IN, OUT, ALL
    private String targetLabel; // 타겟 노드 라벨 (null이면 전체)
}