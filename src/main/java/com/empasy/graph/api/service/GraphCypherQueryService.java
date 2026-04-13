package com.empasy.graph.api.service;

import com.empasy.graph.api.dto.GraphCypherQueryDto;
import com.empasy.graph.api.entity.GraphCypherQuery;
import com.empasy.graph.api.repository.GraphCypherQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GraphCypherQueryService {

    private final GraphCypherQueryRepository graphCypherQueryRepository;
    private final com.empasy.graph.api.repository.GraphCommonRepository graphCommonRepository;

    private static final Pattern MUTATION_PATTERN = Pattern.compile(
            "\\b(DELETE|DETACH|CREATE|SET|MERGE|REMOVE|DROP|CALL)\\b",
            Pattern.CASE_INSENSITIVE
    );

    public List<GraphCypherQueryDto> getAllQueries(String queryType) {
        List<GraphCypherQuery> queries;
        if (queryType != null && !queryType.isEmpty()) {
            queries = graphCypherQueryRepository.findByQueryType(queryType);
        } else {
            queries = graphCypherQueryRepository.findAll();
        }

        return queries.stream()
                .map(GraphCypherQueryDto::from)
                .collect(Collectors.toList());
    }

    public List<GraphCypherQueryDto> getValidQueries(String queryType) {
        List<GraphCypherQuery> queries;
        if (queryType != null && !queryType.isEmpty()) {
            queries = graphCypherQueryRepository.findByQueryType(queryType);
        } else {
            queries = graphCypherQueryRepository.findAll();
        }

        return queries.stream()
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
        validateReadOnlyQuery(dto.getCypherQuery());

        GraphCypherQuery entity = GraphCypherQuery.builder()
                .title(dto.getTitle())
                .cypherQuery(dto.getCypherQuery())
                .description(dto.getDescription())
                .queryType(dto.getQueryType())
                .build();

        GraphCypherQuery savedEntity = graphCypherQueryRepository.save(entity);
        return GraphCypherQueryDto.from(savedEntity);
    }

    @Transactional
    public GraphCypherQueryDto updateQuery(Long id, GraphCypherQueryDto dto) {
        validateReadOnlyQuery(dto.getCypherQuery());

        GraphCypherQuery entity = graphCypherQueryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("GraphCypherQuery not found with id: " + id));

        entity.update(dto.getTitle(), dto.getCypherQuery(), dto.getDescription(), dto.getQueryType());

        return GraphCypherQueryDto.from(entity);
    }

    @Transactional
    public void deleteQuery(Long id) {
        graphCypherQueryRepository.deleteById(id);
    }

    private void validateReadOnlyQuery(String cypherQuery) {
        if (cypherQuery == null || cypherQuery.trim().isEmpty()) {
            return;
        }

        Matcher matcher = MUTATION_PATTERN.matcher(cypherQuery);

        if (matcher.find()) {
            String matchedWord = matcher.group(1).toUpperCase();
            throw new IllegalArgumentException(
                    "보안 경고: 데이터 변경 쿼리(" + matchedWord + ")는 저장하거나 실행할 수 없습니다."
            );
        }
    }
}

