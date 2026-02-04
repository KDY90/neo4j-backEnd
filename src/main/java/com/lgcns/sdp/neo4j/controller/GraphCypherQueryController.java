package com.lgcns.sdp.neo4j.controller;

import com.lgcns.sdp.neo4j.dto.GraphCypherQueryDto;
import com.lgcns.sdp.neo4j.service.GraphCypherQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/graph-cypher-query")
@RequiredArgsConstructor
public class GraphCypherQueryController {

    private final GraphCypherQueryService graphCypherQueryService;

    @GetMapping
    public ResponseEntity<List<GraphCypherQueryDto>> getAllQueries() {
        return ResponseEntity.ok(graphCypherQueryService.getAllQueries());
    }

    @GetMapping("/valid")
    public ResponseEntity<List<GraphCypherQueryDto>> getValidQueries() {
        return ResponseEntity.ok(graphCypherQueryService.getValidQueries());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GraphCypherQueryDto> getQueryById(@PathVariable Long id) {
        return ResponseEntity.ok(graphCypherQueryService.getQueryById(id));
    }

    @PostMapping
    public ResponseEntity<GraphCypherQueryDto> createQuery(@RequestBody GraphCypherQueryDto dto) {
        return ResponseEntity.ok(graphCypherQueryService.createQuery(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GraphCypherQueryDto> updateQuery(@PathVariable Long id,
            @RequestBody GraphCypherQueryDto dto) {
        return ResponseEntity.ok(graphCypherQueryService.updateQuery(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuery(@PathVariable Long id) {
        graphCypherQueryService.deleteQuery(id);
        return ResponseEntity.ok().build();
    }
}
