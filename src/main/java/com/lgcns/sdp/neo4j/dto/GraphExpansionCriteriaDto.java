package com.lgcns.sdp.neo4j.dto;

import lombok.Data;

@Data
public class GraphExpansionCriteriaDto {
    private String relation;     
    private String direction;    
    private String targetLabel;  
}