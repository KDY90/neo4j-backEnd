package com.lgcns.sdp.neo4j.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
public class GraphStyleRequestDto {
    private String label;
    private String elementType;
    private Map<String, Object> styleConfig;
}
