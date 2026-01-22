package com.lgcns.sdp.neo4j.service;

import com.lgcns.sdp.neo4j.dto.GraphLabelCountDto;
import com.lgcns.sdp.neo4j.dto.GraphSchemaDto;
import com.lgcns.sdp.neo4j.dto.GraphSearchBarDto;
import com.lgcns.sdp.neo4j.repository.GraphCommonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;

@Service
@RequiredArgsConstructor
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
}
