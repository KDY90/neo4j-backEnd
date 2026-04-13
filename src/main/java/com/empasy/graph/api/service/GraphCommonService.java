package com.empasy.graph.api.service;

import com.empasy.graph.api.annotation.Neo4jTransactional;
import com.empasy.graph.api.dto.*;
import com.empasy.graph.api.repository.GraphCommonRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Neo4jTransactional(readOnly = true)
public class GraphCommonService {

    private final GraphCommonRepository graphCommonRepository;

    public Collection<GraphSchemaDto> getSchemaInfo() {

        return graphCommonRepository.findSchemaInfo();
    }

    public Collection<GraphLabelCountDto> getLabelCounts() {

        return graphCommonRepository.getLabelCounts();
    }

    public GraphSearchBarDto getSearchBarData() {

        return graphCommonRepository.findSearchBarSchema();
    }

    public Collection<Map<String, Object>> executeCypher(String query) {

        return graphCommonRepository.executeRawCypher(query);
    }

    public Map<String, Object> validateQuery(String query) {
        return graphCommonRepository.validateCypher(query);
    }

    public GraphDetailDto findNodeAndNeighbors(String elementId) {

        return graphCommonRepository.findNodeAndNeighbors(elementId);
    }

    public GraphDetailDto findSpecificNodeNeighbors(String elementId, String relation, String direction, String targetLabel) {
        return graphCommonRepository.findSpecificNodeNeighbors(elementId, relation, direction,targetLabel);
    }

    public GraphDetailDto findSpecificNodeNeighborsBatch(String elementId, List<GraphExpansionCriteriaDto> criteriaList, Integer limit) {
        return graphCommonRepository.findSpecificNodeNeighborsBatch(elementId, criteriaList, limit);
    }



    public GraphExpansionStatsDto getNodeExpansionStats(String elementId , List<String> excludeRelIds) {
        return graphCommonRepository.getNodeExpansionStats(elementId ,excludeRelIds);
    }

}

