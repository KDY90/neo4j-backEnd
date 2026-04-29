package com.empasy.graph.api.repository;

import com.empasy.graph.api.dto.*;
import java.util.Collection;
import java.util.Map;

public interface GenericNodeRepository {
    Collection<Map<String, Object>> findAllByLabel(String label);
    GraphLabelNodesResponseDto getNodesByLabelForTable(GraphLabelNodesRequestDto requestDto);
    GraphNodeChildrenResponseDto getChildrenNodes(String elementId);
    GraphCreateNodeResponseDto createNode(GraphCreateNodeRequestDto requestDto);
    GraphCreateNodeResponseDto updateNode(String elementId, GraphUpdateNodeRequestDto requestDto);
    void deleteNode(String elementId);
}
