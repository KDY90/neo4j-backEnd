package com.lgcns.sdp.neo4j.dto;

import lombok.Builder;
import java.util.Map;

@Builder
public record GraphSchemaDto(
                String label,
                Map<String, String> properties) {
}