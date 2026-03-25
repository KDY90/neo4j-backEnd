package com.empasy.graph.api.dto;

import com.empasy.graph.api.entity.GraphCypherQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class GraphCypherQueryDto {
    @Schema(title = "쿼리 ID", description = "쿼리 ID")
    private Long id;
    @Schema(title = "쿼리 title", description = "쿼리 title")
    private String title;
    @Schema(title = "싸이퍼 쿼리", description = "싸이퍼 쿼리")
    private String cypherQuery;
    @Schema(title = "설명", description = "설명")
    private String description;
    @Schema(title = "쿼리타입", description = "쿼리타입")
    private String queryType;
    @Schema(title = "쿼리 생성일시", description = "쿼리 생성일시")
    private OffsetDateTime createTimestamp;
    @Schema(title = "쿼리 수정일시", description = "쿼리 수정일시")
    private OffsetDateTime updateTimestamp;

    public static GraphCypherQueryDto from(GraphCypherQuery entity) {
        return GraphCypherQueryDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .cypherQuery(entity.getCypherQuery())
                .description(entity.getDescription())
                .queryType(entity.getQueryType())
                .createTimestamp(entity.getCreateTimestamp())
                .updateTimestamp(entity.getUpdateTimestamp())
                .build();
    }
}

