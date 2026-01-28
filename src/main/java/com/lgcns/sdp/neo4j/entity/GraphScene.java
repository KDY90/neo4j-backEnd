package com.lgcns.sdp.neo4j.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_graph_scene", schema = "vcisodb")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Comment("그래프 씬 테이블")
public class GraphScene {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("PK")
    @Column(name = "id")
    private Long id;

    @Comment("씬 이름")
    @Column(name = "scene_name", nullable = false)
    private String sceneName;

    @Lob
    @Comment("씬을 생성한 Cypher 쿼리")
    @Column(name = "scene_query", nullable = false, columnDefinition = "TEXT")
    private String sceneQuery;

    @Comment("총 노드 개수")
    @Column(name = "node_count", nullable = false, columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer nodeCount = 0;

    @Comment("총 릴레이션 개수")
    @Column(name = "rel_count", nullable = false, columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer relCount = 0;

    @Comment("생성일시")
    @CreationTimestamp
    @Column(name = "create_timestamp", updatable = false)
    private LocalDateTime createTimestamp;

    @Comment("수정일시")
    @UpdateTimestamp
    @Column(name = "update_timestamp")
    private LocalDateTime updateTimestamp;
}