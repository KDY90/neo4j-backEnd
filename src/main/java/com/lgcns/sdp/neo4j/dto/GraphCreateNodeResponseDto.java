package com.lgcns.sdp.neo4j.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.Map;

@Getter
@Builder
public class GraphCreateNodeResponseDto {
    private String elementId;
    private String label;
    private Map<String, Object> properties;
    private Map<String, Object> style;
}
