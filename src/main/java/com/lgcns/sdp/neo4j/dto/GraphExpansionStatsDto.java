package com.lgcns.sdp.neo4j.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class GraphExpansionStatsDto {
    private List<ExpansionItemDto> relationships;
    private List<ExpansionItemDto> categories;
    private List<ExpansionItemDto> pairs;

    @Data
    @Builder
    public static class ExpansionItemDto {
        private String id;          // 유니크 키 생성용
        private String label;       // 화면 표시 이름 (관계명 또는 노드라벨)
        private String targetLabel; // Pairs용 타겟 노드 라벨
        private long count;         // 개수
        private String direction;   // IN, OUT
    }
}