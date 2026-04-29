package com.empasy.graph.api.service;

import com.empasy.graph.api.annotation.Neo4jTransactional;
import com.empasy.graph.api.dto.*;
import com.empasy.graph.api.repository.GenericNodeRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Neo4jTransactional(readOnly = true)
public class GenericNodeService {

    private final GenericNodeRepository genericNodeRepository;

    public Collection<Map<String, Object>> findAllByLabel(String label) {
        return genericNodeRepository.findAllByLabel(label);
    }

    public GraphLabelNodesResponseDto getNodesByLabelForTable(GraphLabelNodesRequestDto requestDto) {
        return genericNodeRepository.getNodesByLabelForTable(requestDto);
    }

    public GraphNodeChildrenResponseDto getChildrenNodes(String elementId) {
        return genericNodeRepository.getChildrenNodes(elementId);
    }

    @Neo4jTransactional
    public GraphCreateNodeResponseDto createNode(GraphCreateNodeRequestDto requestDto) {
        return genericNodeRepository.createNode(requestDto);
    }

    @Neo4jTransactional
    public GraphCreateNodeResponseDto updateNode(String elementId, GraphUpdateNodeRequestDto requestDto) {
        return genericNodeRepository.updateNode(elementId, requestDto);
    }

    @Neo4jTransactional
    public void deleteNode(String elementId) {
        genericNodeRepository.deleteNode(elementId);
    }
}