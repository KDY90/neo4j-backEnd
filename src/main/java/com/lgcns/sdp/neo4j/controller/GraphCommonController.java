package com.lgcns.sdp.neo4j.controller;

import com.lgcns.sdp.neo4j.dto.*;
import com.lgcns.sdp.neo4j.service.GraphCommonService;
import com.lgcns.sdp.neo4j.service.GraphSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

@RestController
@RequestMapping("/api/graph")
@RequiredArgsConstructor
public class GraphCommonController {

    private final GraphCommonService graphCommonService;
    private final GraphSearchService graphSearchService;

    @GetMapping("/schema")
    public ResponseEntity<Collection<GraphSchemaDto>> getSchemaInfo() {
        return ResponseEntity.ok(graphCommonService.getSchemaInfo());
    }

    @GetMapping("/labels")
    public ResponseEntity<Collection<GraphLabelCountDto>> getLabelCounts() {
        return ResponseEntity.ok(graphCommonService.getLabelCounts());
    }

    @GetMapping("/search-bar")
    public ResponseEntity<GraphSearchBarDto> getSearchBarData() {
        return ResponseEntity.ok(graphCommonService.getSearchBarData());
    }

    @PostMapping("/query")
    public ResponseEntity<Collection<Map<String, Object>>> executeQuery(@RequestBody Map<String, String> body) {
        String query = body.get("query");
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(graphCommonService.executeCypher(query));
    }

    @PostMapping("/search")
    public GraphSearchResponseDto searchGraph(@RequestBody GraphSearchRequestDto requestDto) {
        // 결과는 Neo4j Path 객체들이 포함된 Map 리스트로 반환됩니다.
        // 프론트엔드에서는 이 결과(p)를 파싱하여 시각화하면 됩니다.
        return graphSearchService.searchByCyphers(requestDto.getCyphers());
    }


    @GetMapping("/node/{elementId}/neighbors")
    public ResponseEntity<GraphDetailDto> getNodeNeighbors(@PathVariable String elementId) {
        return ResponseEntity.ok(graphCommonService.findNodeAndNeighbors(elementId));
    }

}
