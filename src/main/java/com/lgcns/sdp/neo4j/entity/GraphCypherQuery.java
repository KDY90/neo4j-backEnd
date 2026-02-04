package com.lgcns.sdp.neo4j.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_graph_cypher_query", schema = "vcisodb")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Comment("자주 쓰는 Cypher 쿼리 테이블")
public class GraphCypherQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("PK")
    @Column(name = "id")
    private Long id;

    @Comment("싸이퍼 이름(제목)")
    @Column(name = "title", nullable = false)
    private String title;

    @Lob
    @Comment("실행할 Cypher 쿼리 본문")
    @Column(name = "cypher_query", nullable = false, columnDefinition = "TEXT")
    private String cypherQuery;

    @Lob
    @Comment("싸이퍼에 대한 설명")
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Comment("생성일시")
    @CreationTimestamp
    @Column(name = "create_timestamp", updatable = false)
    private LocalDateTime createTimestamp;

    @Comment("수정일시")
    @UpdateTimestamp
    @Column(name = "update_timestamp")
    private LocalDateTime updateTimestamp;

    public void update(String title, String cypherQuery, String description) {
        this.title = title;
        this.cypherQuery = cypherQuery;
        this.description = description;
    }
}