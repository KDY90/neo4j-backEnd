package com.lgcns.sdp.neo4j.controller;

import com.lgcns.sdp.neo4j.dto.*;
import com.lgcns.sdp.neo4j.service.GraphCommonService;
import com.lgcns.sdp.neo4j.service.GraphSearchService;
import com.lgcns.sdp.neo4j.support.BaseResponse;
import com.lgcns.sdp.neo4j.support.BaseRestControllerV2;
import com.lgcns.sdp.neo4j.support.ResultCode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/graph")
@RequiredArgsConstructor
public class GraphCommonController extends BaseRestControllerV2 {

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
    public DeferredResult<BaseResponse<Collection<GraphSchemaDto>>> getSchemaInfo() {
        return deferShortTimeDb(() -> BaseResponse.success(graphCommonService.getSchemaInfo()));
    }

    @GetMapping("/labels")
    @Operation(description = "라벨 카운트 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphLabelCountDto.class), mediaType = "application/json"))
    }
    )
    public DeferredResult<BaseResponse<Collection<GraphLabelCountDto>>> getLabelCounts() {
        return deferShortTimeDb(() -> BaseResponse.success(graphCommonService.getLabelCounts()));
    }

    @GetMapping("/search-bar")
    @Operation(description = "검색바 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphSearchBarDto.class), mediaType = "application/json"))
    }
    )
    public DeferredResult<BaseResponse<GraphSearchBarDto>> getSearchBarData() {
        return deferShortTimeDb(() -> BaseResponse.success(graphCommonService.getSearchBarData()));
    }

    @PostMapping("/query")
    @Operation(description = "싸이퍼 쿼리 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server")
    }
    )
    public DeferredResult<BaseResponse<Collection<Map<String, Object>>>> executeQuery(@RequestBody Map<String, String> body) {
        String query = body.get("query");
        if (query == null || query.trim().isEmpty()) {
            return deferShortTimeDb(() -> BaseResponse.of(ResultCode.INVALID_PARAMETER));
        }
        return deferShortTimeDb(() -> BaseResponse.success(graphCommonService.executeCypher(query)));
    }

    @PostMapping("/validate")
    @Operation(description = "싸이퍼 쿼리 validate")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server")
    }
    )
    public DeferredResult<BaseResponse<Map<String, Object>>> validateQuery(@RequestBody Map<String, String> body) {
        String query = body.get("query");
        if (query == null || query.trim().isEmpty()) {
            return deferShortTimeDb(() -> BaseResponse.of(ResultCode.INVALID_PARAMETER));
        }
        return deferShortTimeDb(() -> BaseResponse.success(graphCommonService.validateQuery(query)));
    }

    @PostMapping("/search")
    @Operation(description = "검색바 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphSearchResponseDto.class), mediaType = "application/json"))
    }
    )
    public DeferredResult<BaseResponse<GraphSearchResponseDto>> searchGraph(@RequestBody GraphSearchRequestDto requestDto) {
        return deferShortTimeDb(() -> BaseResponse.success(graphSearchService.searchByCyphers(requestDto)));
    }

    @GetMapping("/node/{elementId}/neighbors")
    @Operation(description = "노드의 관련 노드,릴레이션 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphDetailDto.class), mediaType = "application/json"))
    }
    )
    public DeferredResult<BaseResponse<GraphDetailDto>> getNodeNeighbors(@PathVariable String elementId) {
        return deferShortTimeDb(() -> BaseResponse.success(graphCommonService.findNodeAndNeighbors(elementId)));
    }

    @GetMapping("/node/{elementId}/neighbors/specific")
    @Operation(description = "노드의 관련 노드,릴레이션 조회 디테일")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphDetailDto.class), mediaType = "application/json"))
    }
    )
    public DeferredResult<BaseResponse<GraphDetailDto>> getSpecificNodeNeighbors(
            @PathVariable String elementId,
            @RequestParam(required = false) String relation,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String targetLabel
    ) {
        String safeDirection = (direction == null || direction.isEmpty()) ? "ALL" : direction;
        return deferShortTimeDb(() -> BaseResponse.success(
                graphCommonService.findSpecificNodeNeighbors(elementId, relation, safeDirection, targetLabel)
        ));
    }

    @PostMapping("/node/{elementId}/neighbors/batch")
    @Operation(description = "노드의 관련 노드,릴레이션 조회 (모달)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphDetailDto.class), mediaType = "application/json"))
    }
    )
    public DeferredResult<BaseResponse<GraphDetailDto>> getSpecificNodeNeighborsBatch(
            @PathVariable String elementId,
            @RequestParam(required = false) Integer limit,
            @RequestBody List<GraphExpansionCriteriaDto> criteriaList
    ) {
        return deferShortTimeDb(() -> BaseResponse.success(
                graphCommonService.findSpecificNodeNeighborsBatch(elementId, criteriaList, limit)
        ));
    }


    @PostMapping("/node/{elementId}/expansion-stats")
    @Operation(description = "노드의 관련 노드,릴레이션 stats 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphExpansionStatsDto.class), mediaType = "application/json"))
    }
    )
    public DeferredResult<BaseResponse<GraphExpansionStatsDto>> getNodeExpansionStats(
            @PathVariable String elementId,
            @RequestBody(required = false) List<String> excludeRelIds  
    ) {
        return deferShortTimeDb(() -> BaseResponse.success(
                graphCommonService.getNodeExpansionStats(elementId, excludeRelIds)
        ));
    }

}

