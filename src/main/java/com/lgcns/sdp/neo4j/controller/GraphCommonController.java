package com.lgcns.sdp.neo4j.controller;

import com.lgcns.sdp.neo4j.dto.*;
import com.lgcns.sdp.neo4j.service.GraphCommonService;
import com.lgcns.sdp.neo4j.service.GraphSearchService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/graph")
@RequiredArgsConstructor
public class GraphCommonController {

    private final GraphCommonService graphCommonService;
    private final GraphSearchService graphSearchService;

    @GetMapping("/schema")
    @Operation(description = "노드 스키마 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphSchemaDto.class), mediaType = "application/json"))
    }
    )
    public ResponseEntity<Collection<GraphSchemaDto>> getSchemaInfo() {
        return ResponseEntity.ok(graphCommonService.getSchemaInfo());
    }

    @GetMapping("/labels")
    @Operation(description = "라벨 카운트 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphLabelCountDto.class), mediaType = "application/json"))
    }
    )
    public ResponseEntity<Collection<GraphLabelCountDto>> getLabelCounts() {
        return ResponseEntity.ok(graphCommonService.getLabelCounts());
    }

    @GetMapping("/search-bar")
    @Operation(description = "검색바 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphSearchBarDto.class), mediaType = "application/json"))
    }
    )
    public ResponseEntity<GraphSearchBarDto> getSearchBarData() {
        return ResponseEntity.ok(graphCommonService.getSearchBarData());
    }

    @PostMapping("/query")
    @Operation(description = "싸이퍼 쿼리 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server")
    }
    )
    public ResponseEntity<Collection<Map<String, Object>>> executeQuery(@RequestBody Map<String, String> body) {
        String query = body.get("query");
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(graphCommonService.executeCypher(query));
    }

    @PostMapping("/validate")
    @Operation(description = "싸이퍼 쿼리 validate")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server")
    }
    )
    public ResponseEntity<Map<String, Object>> validateQuery(@RequestBody Map<String, String> body) {
        String query = body.get("query");
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(graphCommonService.validateQuery(query));
    }

    @PostMapping("/search")
    @Operation(description = "검색바 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphSearchResponseDto.class), mediaType = "application/json"))
    }
    )
    public GraphSearchResponseDto searchGraph(@RequestBody GraphSearchRequestDto requestDto) {
        return graphSearchService.searchByCyphers(requestDto);
    }

    @GetMapping("/node/{elementId}/neighbors")
    @Operation(description = "노드의 관련 노드,릴레이션 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphDetailDto.class), mediaType = "application/json"))
    }
    )
    public ResponseEntity<GraphDetailDto> getNodeNeighbors(@PathVariable String elementId) {
        return ResponseEntity.ok(graphCommonService.findNodeAndNeighbors(elementId));
    }

    @GetMapping("/node/{elementId}/neighbors/specific")
    @Operation(description = "노드의 관련 노드,릴레이션 조회 디테일")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphDetailDto.class), mediaType = "application/json"))
    }
    )
    public ResponseEntity<GraphDetailDto> getSpecificNodeNeighbors(
            @PathVariable String elementId,
            @RequestParam(required = false) String relation,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String targetLabel
    ) {
        String safeDirection = (direction == null || direction.isEmpty()) ? "ALL" : direction;

         
        return ResponseEntity.ok(
                graphCommonService.findSpecificNodeNeighbors(elementId, relation, safeDirection, targetLabel)
        );
    }

    @PostMapping("/node/{elementId}/neighbors/batch")
    @Operation(description = "노드의 관련 노드,릴레이션 조회 (모달)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphDetailDto.class), mediaType = "application/json"))
    }
    )
    public ResponseEntity<GraphDetailDto> getSpecificNodeNeighborsBatch(
            @PathVariable String elementId,
            @RequestParam(required = false) Integer limit,
            @RequestBody List<GraphExpansionCriteriaDto> criteriaList
    ) {
        return ResponseEntity.ok(
                graphCommonService.findSpecificNodeNeighborsBatch(elementId, criteriaList, limit)
        );
    }


    @PostMapping("/node/{elementId}/expansion-stats")
    @Operation(description = "노드의 관련 노드,릴레이션 stats 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphExpansionStatsDto.class), mediaType = "application/json"))
    }
    )
    public ResponseEntity<GraphExpansionStatsDto> getNodeExpansionStats(
            @PathVariable String elementId,
            @RequestBody(required = false) List<String> excludeRelIds  
    ) {
        return ResponseEntity.ok(
                graphCommonService.getNodeExpansionStats(elementId, excludeRelIds)
        );
    }

}

