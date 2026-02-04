package com.lgcns.sdp.neo4j.service;

import com.lgcns.sdp.neo4j.dto.GraphCreateNodeRequestDto;
import com.lgcns.sdp.neo4j.dto.GraphCreateNodeResponseDto;
import com.lgcns.sdp.neo4j.util.GraphUtil;
import lombok.RequiredArgsConstructor;
import org.neo4j.driver.types.Node;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GenericNodeService {

    private final Neo4jClient neo4jClient;
    private final GraphUtil graphUtil;

    public Collection<Map<String, Object>> findAllByLabel(String label) {
        String query = String.format("MATCH (n:%s) RETURN n{.*, id: elementId(n)} as data", label);
        return neo4jClient.query(query).fetch().all();
    }

    public GraphCreateNodeResponseDto createNode(GraphCreateNodeRequestDto requestDto) {
        String label = requestDto.getLabel();
        Map<String, Object> properties = requestDto.getProperties();

        // 1. 라벨 유효성 검사
        if (label == null || label.trim().isEmpty()) {
            throw new IllegalArgumentException("Label cannot be empty");
        }

        // 2. 쿼리 생성 (특수문자 방지 백틱 추가)
        String query = String.format("CREATE (n:`%s` $props) RETURN n", label);

        // 3. 실행 및 DTO 매핑
        return neo4jClient.query(query)
                .bind(properties).to("props")
                .fetchAs(GraphCreateNodeResponseDto.class)
                .mappedBy((typeSystem, record) -> {
                    // 3-1. 생성된 노드 가져오기
                    Node node = record.get("n").asNode();
                    String nodeLabel = node.labels().iterator().next();

                    Map<String, Object> styleConfig = graphUtil.getStyleConfig(
                            nodeLabel,
                            "NODE",
                            new HashMap<>()
                    );

                    // 3-3. DTO 빌드 (style 포함)
                    return GraphCreateNodeResponseDto.builder()
                            .elementId(node.elementId())
                            .label(nodeLabel)
                            .properties(node.asMap())
                            .style(styleConfig) // [추가] 스타일 설정
                            .build();
                })
                .one()
                .orElseThrow(() -> new RuntimeException("Failed to create node"));
    }
}
