package com.lgcns.sdp.neo4j.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class GraphSearchRequestDto {

    // "토큰" 대신 "싸이퍼스(cyphers)"로 변경!
    private List<CypherBlock> cyphers;
    private int limit;
    private boolean caseInsensitiveSearch;

    @Data
    @NoArgsConstructor
    public static class CypherBlock {
        private String type;        // "NODE", "RELATIONSHIP"
        private String label;       // "Person", "WROTE", "ANY"
        private String direction;   // "OUT", "IN", "BOTH"
        private Map<String, Object> properties;
        private Map<String, Object> savedQueryContent;
    }
}