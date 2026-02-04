package com.lgcns.sdp.neo4j.dto;

import com.lgcns.sdp.neo4j.entity.GraphCypherQuery;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class GraphCypherQueryDto {
    private Long id;
    private String title;
    private String cypherQuery;
    private String description;
    private LocalDateTime createTimestamp;
    private LocalDateTime updateTimestamp;

    public static GraphCypherQueryDto from(GraphCypherQuery entity) {
        return GraphCypherQueryDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .cypherQuery(entity.getCypherQuery())
                .description(entity.getDescription())
                .createTimestamp(entity.getCreateTimestamp())
                .updateTimestamp(entity.getUpdateTimestamp())
                .build();
    }
}
