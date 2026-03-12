package com.lgcns.sdp.neo4j.dto;

import com.lgcns.sdp.neo4j.entity.GraphScene;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GraphSceneDto {

    @Schema(title = "scene id", description = "씬 id")
    private Long id;
    @Schema(title = "sceneName", description = "씬 name")
    private String sceneName;
    @Schema(title = "sceneQuery", description = "씬 Query")
    private String sceneQuery;
    @Schema(title = "nodeCount", description = "노드 갯수")
    private Integer nodeCount;
    @Schema(title = "relCount", description = "릴레이션 갯수")
    private Integer relCount;
    @Schema(title = "sceneConfig", description = "sceneConfig JSON")
    private Map<String, Object> sceneConfig;
    @Schema(title = "createTimestamp", description = "생성일자")
    private OffsetDateTime createTimestamp;
    @Schema(title = "updateTimestamp", description = "수정일자")
    private OffsetDateTime updateTimestamp;


    public static GraphSceneDto fromEntity(GraphScene entity) {
        return GraphSceneDto.builder()
                .id(entity.getId())
                .sceneName(entity.getSceneName())
                .sceneQuery(entity.getSceneQuery())
                .nodeCount(entity.getNodeCount())
                .relCount(entity.getRelCount())
                .sceneConfig(entity.getSceneConfig())
                .createTimestamp(entity.getCreateTimestamp())
                .updateTimestamp(entity.getUpdateTimestamp())
                .build();
    }


    public GraphScene toEntity() {
        return GraphScene.builder()
                .id(this.id)
                .sceneName(this.sceneName)
                .sceneQuery(this.sceneQuery)
                .nodeCount(this.nodeCount == null ? 0 : this.nodeCount)
                .relCount(this.relCount == null ? 0 : this.relCount)
                .sceneConfig(this.sceneConfig == null ? new HashMap<>() : this.sceneConfig)
                .build();
    }
}
