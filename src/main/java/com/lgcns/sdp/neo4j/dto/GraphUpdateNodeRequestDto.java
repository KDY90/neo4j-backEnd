package com.lgcns.sdp.neo4j.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GraphUpdateNodeRequestDto {

    private Map<String, Object> properties;
}
