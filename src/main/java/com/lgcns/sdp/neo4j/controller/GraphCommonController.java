package com.lgcns.sdp.neo4j.controller;

import com.lgcns.sdp.neo4j.dto.*;
import com.lgcns.sdp.neo4j.service.GraphCommonService;
import com.lgcns.sdp.neo4j.service.GraphSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
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

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateQuery(@RequestBody Map<String, String> body) {
        String query = body.get("query");
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(graphCommonService.validateQuery(query));
    }

    @PostMapping("/search")
    public GraphSearchResponseDto searchGraph(@RequestBody GraphSearchRequestDto requestDto) {
        // 결과는 Neo4j Path 객체들이 포함된 Map 리스트로 반환됩니다.
        // 프론트엔드에서는 이 결과(p)를 파싱하여 시각화하면 됩니다.
        return graphSearchService.searchByCyphers(requestDto);
    }

    @GetMapping("/node/{elementId}/neighbors")
    public ResponseEntity<GraphDetailDto> getNodeNeighbors(@PathVariable String elementId) {
        return ResponseEntity.ok(graphCommonService.findNodeAndNeighbors(elementId));
    }

    @GetMapping("/node/{elementId}/neighbors/specific")
    public ResponseEntity<GraphDetailDto> getSpecificNodeNeighbors(
            @PathVariable String elementId,
            @RequestParam(required = false) String relation,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String targetLabel
    ) {
        String safeDirection = (direction == null || direction.isEmpty()) ? "ALL" : direction;

        // Service 메서드 호출 시 targetLabel 추가 전달
        return ResponseEntity.ok(
                graphCommonService.findSpecificNodeNeighbors(elementId, relation, safeDirection, targetLabel)
        );
    }

    @PostMapping("/node/{elementId}/neighbors/batch")
    public ResponseEntity<GraphDetailDto> getSpecificNodeNeighborsBatch(
            @PathVariable String elementId,
            @RequestBody List<GraphExpansionCriteriaDto> criteriaList // [핵심] 리스트로 받음
    ) {
        return ResponseEntity.ok(
                graphCommonService.findSpecificNodeNeighborsBatch(elementId, criteriaList)
        );
    }


    @PostMapping("/node/{elementId}/expansion-stats") // GET -> POST로 변경 (List Body 전송 위해)
    public ResponseEntity<GraphExpansionStatsDto> getNodeExpansionStats(
            @PathVariable String elementId,
            @RequestBody(required = false) List<String> excludeRelIds // 제외할 관계 ID 목록
    ) {
        return ResponseEntity.ok(
                graphCommonService.getNodeExpansionStats(elementId, excludeRelIds)
        );
    }

}
