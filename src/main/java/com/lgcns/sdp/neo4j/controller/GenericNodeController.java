package com.lgcns.sdp.neo4j.controller;

import com.lgcns.sdp.neo4j.dto.*;
import com.lgcns.sdp.neo4j.service.GenericNodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/nodes")
@RequiredArgsConstructor
public class GenericNodeController {

    private final GenericNodeService genericNodeService;

    @GetMapping("/{label}")
    public ResponseEntity<Collection<Map<String, Object>>> getNodesByLabel(@PathVariable String label) {
        return ResponseEntity.ok(genericNodeService.findAllByLabel(label));
    }

    @PostMapping("/table")
    public ResponseEntity<GraphLabelNodesResponseDto> getNodesByLabelForTable(@RequestBody GraphLabelNodesRequestDto requestDto) {
        return ResponseEntity.ok(genericNodeService.getNodesByLabelForTable(requestDto));
    }

    @GetMapping("/{elementId}/children")
    public ResponseEntity<GraphNodeChildrenResponseDto> getChildren(@PathVariable String elementId) {
        return ResponseEntity.ok(genericNodeService.getChildrenNodes(elementId));
    }

    @PostMapping("/create")
    public ResponseEntity<GraphCreateNodeResponseDto> createNode(@RequestBody GraphCreateNodeRequestDto requestDto) {
        GraphCreateNodeResponseDto response = genericNodeService.createNode(requestDto);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GraphCreateNodeResponseDto> updateNode(@PathVariable String id,
            @RequestBody GraphUpdateNodeRequestDto requestDto) {
        GraphCreateNodeResponseDto response = genericNodeService.updateNode(id, requestDto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNode(@PathVariable String id) {
        genericNodeService.deleteNode(id);
        return ResponseEntity.noContent().build();
    }
}
