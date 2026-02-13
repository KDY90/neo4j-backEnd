package com.lgcns.sdp.neo4j.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class GraphSearchRequestDto {

     
    private List<CypherBlock> cyphers;
    private int limit;
    private boolean caseInsensitiveSearch;

    @Data
    @NoArgsConstructor
    public static class CypherBlock {
        private String type;         
        private String label;        
        private String direction;    
        private Map<String, Object> properties;
        private Map<String, Object> savedQueryContent;
    }
}