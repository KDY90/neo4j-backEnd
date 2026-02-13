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
        private String id;           
        private String label;        
        private String targetLabel;  
        private long count;          
        private String direction;    
    }
}