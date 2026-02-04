package com.lgcns.sdp.neo4j.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.Map;

@Getter
@Setter
public class GraphCreateNodeRequestDto {
    private String label;
    private Map<String, Object> properties;
}
