package com.lgcns.sdp.neo4j.service;

import com.lgcns.sdp.neo4j.dto.GraphSceneDto;
import com.lgcns.sdp.neo4j.entity.GraphScene;
import com.lgcns.sdp.neo4j.repository.GraphSceneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GraphSceneService {

    private final GraphSceneRepository graphSceneRepository;

    public List<GraphSceneDto> getAllScenes() {
        return graphSceneRepository.findAll().stream()
                .map(GraphSceneDto::fromEntity)
                .collect(Collectors.toList());
    }

    public GraphSceneDto getSceneById(Long id) {
        return graphSceneRepository.findById(id)
                .map(GraphSceneDto::fromEntity)
                .orElseThrow(() -> new IllegalArgumentException("GraphScene not found with id: " + id));
    }

    @Transactional
    public GraphSceneDto createScene(GraphSceneDto dto) {
        GraphScene entity = GraphScene.builder()
                .sceneName(dto.getSceneName())
                .sceneQuery(dto.getSceneQuery())
                .nodeCount(dto.getNodeCount())
                .relCount(dto.getRelCount())
                .build();

        GraphScene savedEntity = graphSceneRepository.save(entity);
        return GraphSceneDto.fromEntity(savedEntity);
    }

    @Transactional
    public GraphSceneDto updateScene(Long id, GraphSceneDto dto) {
        GraphScene entity = graphSceneRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("GraphScene not found with id: " + id));

        entity.update(dto.getSceneName(), dto.getSceneQuery(), dto.getNodeCount(), dto.getRelCount());

        return GraphSceneDto.fromEntity(entity);
    }

    @Transactional
    public void deleteScene(Long id) {
        graphSceneRepository.deleteById(id);
    }
}
