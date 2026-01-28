package com.lgcns.sdp.neo4j.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "tb_graph_style",  schema = "vcisodb", uniqueConstraints = {
        @UniqueConstraint(name = "uq_graph_style", columnNames = {"label", "element_type"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Comment("그래프 시각화 스타일 설정 (노드 및 엣지) 테이블")
public class GraphStyle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("PK")
    @Column(name = "id")
    private Long id;

    @Comment("라벨")
    @Column(name = "label", nullable = false)
    private String label;

    @Comment("타입")
    @Column(name = "element_type", nullable = false, length = 50)
    private String elementType;

    @Comment("스타일 설정 JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "style_config", nullable = false, columnDefinition = "longtext")
    private Map<String, Object> styleConfig;

    @Comment("생성일시")
    @CreationTimestamp
    @Column(name = "create_timestamp", updatable = false)
    private LocalDateTime createTimestamp;

    @Comment("수정일시")
    @UpdateTimestamp
    @Column(name = "update_timestamp")
    private LocalDateTime updateTimestamp;
}