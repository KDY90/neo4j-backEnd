package com.lgcns.sdp.neo4j.controller;

import com.lgcns.sdp.neo4j.dto.GraphSceneDto;
import com.lgcns.sdp.neo4j.service.GraphSceneService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/graph-scene")
@RequiredArgsConstructor
public class GraphSceneController {

    private final GraphSceneService graphSceneService;

    @GetMapping
    public ResponseEntity<List<GraphSceneDto>> getAllScenes() {
        return ResponseEntity.ok(graphSceneService.getAllScenes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GraphSceneDto> getSceneById(@PathVariable Long id) {
        return ResponseEntity.ok(graphSceneService.getSceneById(id));
    }

    @PostMapping
    public ResponseEntity<GraphSceneDto> createScene(@RequestBody GraphSceneDto dto) {
        return ResponseEntity.ok(graphSceneService.createScene(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GraphSceneDto> updateScene(@PathVariable Long id, @RequestBody GraphSceneDto dto) {
        return ResponseEntity.ok(graphSceneService.updateScene(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScene(@PathVariable Long id) {
        graphSceneService.deleteScene(id);
        return ResponseEntity.ok().build();
    }
}
