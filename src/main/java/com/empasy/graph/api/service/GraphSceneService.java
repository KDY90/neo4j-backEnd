package com.empasy.graph.api.service;

import com.empasy.graph.api.dto.GraphSceneDto;
import com.empasy.graph.api.entity.GraphScene;
import com.empasy.graph.api.repository.GraphSceneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GraphSceneService {

    private final GraphSceneRepository graphSceneRepository;
    private final Neo4jClient neo4jClient;

    public List<GraphSceneDto> getAllScenes() {
        return graphSceneRepository.findAllByOrderByIdDesc().stream()
                .map(GraphSceneDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GraphSceneDto getSceneById(Long id) {
        GraphScene entity = graphSceneRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("GraphScene not found with id: " + id));

        GraphSceneDto dto = GraphSceneDto.fromEntity(entity);

        // Neo4j에 쿼리를 던져서 상세 개수를 가져오는 로직 실행
        return enrichSceneWithExactCounts(dto);
    }

    @Transactional
    public GraphSceneDto createScene(GraphSceneDto dto) {

        long totalCount = graphSceneRepository.count();

        GraphScene entity = GraphScene.builder()
                .sceneName("Untitled Scene" + totalCount)
                .sceneQuery("")
                .nodeCount(0)
                .relCount(0)
                .sceneConfig(new HashMap<>())
                .build();

        GraphScene savedEntity = graphSceneRepository.save(entity);
        return GraphSceneDto.fromEntity(savedEntity);
    }

    @Transactional
    public GraphSceneDto updateScene(Long id, GraphSceneDto dto) {
        GraphScene entity = graphSceneRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("GraphScene not found with id: " + id));

        entity.update(
                dto.getSceneName(),
                dto.getSceneQuery(),
                dto.getNodeCount(),
                dto.getRelCount(),
                dto.getSceneConfig()
        );

        return GraphSceneDto.fromEntity(entity);
    }

    @Transactional
    public void deleteScene(Long id) {
        graphSceneRepository.deleteById(id);
    }

    @SuppressWarnings("unchecked")
    private GraphSceneDto enrichSceneWithExactCounts(GraphSceneDto dto) {
        Map<String, Object> config = dto.getSceneConfig();
        if (config == null || !config.containsKey("cypherBlocks")) {
            return dto;
        }

        List<Map<String, Object>> blocks = (List<Map<String, Object>>) config.get("cypherBlocks");
        if (blocks == null || blocks.isEmpty()) {
            return dto;
        }

        try {
            StringBuilder matchClause = new StringBuilder("MATCH ");
            Map<String, Object> firstBlock = blocks.get(0);
            String firstLabel = (String) firstBlock.get("label");

            matchClause.append("(n0");
            if (firstLabel != null && !"ANY".equals(firstLabel)) {
                matchClause.append(":`").append(firstLabel).append("`");
            }
            matchClause.append(")");

            for (int i = 1; i < blocks.size(); i += 2) {
                if (i + 1 >= blocks.size()) break;

                Map<String, Object> relBlock = blocks.get(i);
                Map<String, Object> nextNodeBlock = blocks.get(i + 1);

                String relLabel = (String) relBlock.get("label");
                String direction = (String) relBlock.get("direction");
                String nextNodeLabel = (String) nextNodeBlock.get("label");

                String relTypeStr = (relLabel != null && !"ANY".equals(relLabel)) ? ":`" + relLabel + "`" : "";
                String nextNodeLabelStr = (nextNodeLabel != null && !"ANY".equals(nextNodeLabel)) ? ":`" + nextNodeLabel + "`" : "";

                if ("OUT".equalsIgnoreCase(direction)) {
                    matchClause.append("-[r").append(i).append(relTypeStr).append("]->");
                } else if ("IN".equalsIgnoreCase(direction)) {
                    matchClause.append("<-[r").append(i).append(relTypeStr).append("]-");
                } else {
                    matchClause.append("-[r").append(i).append(relTypeStr).append("]-");
                }

                matchClause.append("(n").append(i + 1).append(nextNodeLabelStr).append(")");
            }

            StringBuilder returnClause = new StringBuilder(" RETURN ");
            for (int i = 0; i < blocks.size(); i++) {
                String varName = (i % 2 == 0) ? "n" + i : "r" + i;
                if (i > 0) returnClause.append(", ");
                returnClause.append("count(DISTINCT ").append(varName).append(") AS count_").append(i);
            }

            String finalQuery = matchClause.toString() + returnClause.toString();
            log.info("Scene Count Query Executing: {}", finalQuery);

            Map<String, Object> countResult = neo4jClient.query(finalQuery)
                    .fetch()
                    .one()
                    .orElse(Collections.emptyMap());

            Map<String, Long> nodesMap = new HashMap<>();
            Map<String, Long> relsMap = new HashMap<>();

            for (int i = 0; i < blocks.size(); i++) {
                String label = (String) blocks.get(i).get("label");
                if (label == null || "ANY".equals(label)) {
                    label = (i % 2 == 0) ? "Total Nodes" : "Total Relations";
                }

                long count = 0L;
                Object countObj = countResult.get("count_" + i);
                if (countObj instanceof Number num) {
                    count = num.longValue();
                }

                if (i % 2 == 0) {
                    nodesMap.merge(label, count, Long::sum);
                } else {
                    relsMap.merge(label, count, Long::sum);
                }
            }

            return dto.toBuilder()
                    .nodesCount(nodesMap)
                    .relationsCount(relsMap)
                    .build();

        } catch (Exception e) {
            log.error("Neo4j Scene 카운트 계산 중 에러 발생", e);
            return dto;
        }
    }

}

