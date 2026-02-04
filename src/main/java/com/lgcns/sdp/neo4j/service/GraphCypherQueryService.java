package com.lgcns.sdp.neo4j.service;

import com.lgcns.sdp.neo4j.dto.GraphCypherQueryDto;
import com.lgcns.sdp.neo4j.entity.GraphCypherQuery;
import com.lgcns.sdp.neo4j.repository.GraphCypherQueryRepository;
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
public class GraphCypherQueryService {

    private final GraphCypherQueryRepository graphCypherQueryRepository;
    private final com.lgcns.sdp.neo4j.repository.GraphCommonRepository graphCommonRepository;

    public List<GraphCypherQueryDto> getAllQueries() {
        return graphCypherQueryRepository.findAll().stream()
                .map(GraphCypherQueryDto::from)
                .collect(Collectors.toList());
    }

    public List<GraphCypherQueryDto> getValidQueries() {
        return graphCypherQueryRepository.findAll().stream()
                .filter(entity -> {
                    java.util.Map<String, Object> validationResult = graphCommonRepository
                            .validateCypher(entity.getCypherQuery());
                    return Boolean.TRUE.equals(validationResult.get("valid"));
                })
                .map(GraphCypherQueryDto::from)
                .collect(Collectors.toList());
    }

    public GraphCypherQueryDto getQueryById(Long id) {
        return graphCypherQueryRepository.findById(id)
                .map(GraphCypherQueryDto::from)
                .orElseThrow(() -> new IllegalArgumentException("GraphCypherQuery not found with id: " + id));
    }

    @Transactional
    public GraphCypherQueryDto createQuery(GraphCypherQueryDto dto) {
        GraphCypherQuery entity = GraphCypherQuery.builder()
                .title(dto.getTitle())
                .cypherQuery(dto.getCypherQuery())
                .description(dto.getDescription())
                .build();

        GraphCypherQuery savedEntity = graphCypherQueryRepository.save(entity);
        return GraphCypherQueryDto.from(savedEntity);
    }

    @Transactional
    public GraphCypherQueryDto updateQuery(Long id, GraphCypherQueryDto dto) {
        GraphCypherQuery entity = graphCypherQueryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("GraphCypherQuery not found with id: " + id));

        entity.update(dto.getTitle(), dto.getCypherQuery(), dto.getDescription());

        return GraphCypherQueryDto.from(entity);
    }

    @Transactional
    public void deleteQuery(Long id) {
        graphCypherQueryRepository.deleteById(id);
    }
}
