package com.lgcns.sdp.neo4j.controller;

import com.lgcns.sdp.neo4j.dto.GraphCreateNodeRequestDto;
import com.lgcns.sdp.neo4j.dto.GraphCreateNodeResponseDto;
import com.lgcns.sdp.neo4j.service.GenericNodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

@RestController
@RequestMapping("/api/nodes")
@RequiredArgsConstructor
public class GenericNodeController {

    private final GenericNodeService genericNodeService;

    @GetMapping("/{label}")
    public ResponseEntity<Collection<Map<String, Object>>> getNodesByLabel(@PathVariable String label) {
        return ResponseEntity.ok(genericNodeService.findAllByLabel(label));
    }

    @PostMapping("/create")
    public ResponseEntity<GraphCreateNodeResponseDto> createNode(@RequestBody GraphCreateNodeRequestDto requestDto) {
        GraphCreateNodeResponseDto response = genericNodeService.createNode(requestDto);
        return ResponseEntity.ok(response);
    }
}
