package com.lgcns.sdp.neo4j.dto;

import com.lgcns.sdp.neo4j.entity.GraphScene;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GraphSceneDto {

    private Long id;
    private String sceneName;
    private String sceneQuery;
    private Integer nodeCount;
    private Integer relCount;
    private LocalDateTime createTimestamp;
    private LocalDateTime updateTimestamp;

    public static GraphSceneDto fromEntity(GraphScene entity) {
        return GraphSceneDto.builder()
                .id(entity.getId())
                .sceneName(entity.getSceneName())
                .sceneQuery(entity.getSceneQuery())
                .nodeCount(entity.getNodeCount())
                .relCount(entity.getRelCount())
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
                .build();
    }
}
