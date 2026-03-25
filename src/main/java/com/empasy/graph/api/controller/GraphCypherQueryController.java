package com.empasy.graph.api.controller;

import com.empasy.graph.api.dto.GraphCypherQueryDto;
import com.empasy.graph.api.service.GraphCypherQueryService;
import com.empasy.graph.api.support.BaseResponse;
import com.empasy.graph.api.support.BaseRestControllerV2;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;

@RestController
@RequestMapping("/api/v1/graph-cypher-query")
@RequiredArgsConstructor
public class GraphCypherQueryController extends BaseRestControllerV2 {

    private final GraphCypherQueryService graphCypherQueryService;

    @GetMapping
    @Operation(description = "모든 쿼리 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphCypherQueryDto.class), mediaType = "application/json"))
    }
    )

    public DeferredResult<BaseResponse<List<GraphCypherQueryDto>>> getAllQueries(
            @RequestParam(required = false) String queryType) {
        return deferShortTimeDb(() -> BaseResponse.success(graphCypherQueryService.getAllQueries(queryType)));
    }

    @GetMapping("/valid")
    @Operation(description = "쿼리 valid")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphCypherQueryDto.class), mediaType = "application/json"))
    }
    )
    public DeferredResult<BaseResponse<List<GraphCypherQueryDto>>> getValidQueries(
            @RequestParam(required = false) String queryType) {
        return deferShortTimeDb(() -> BaseResponse.success(graphCypherQueryService.getValidQueries(queryType)));
    }

    @GetMapping("/{id}")
    @Operation(description = "쿼리 디테일 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphCypherQueryDto.class), mediaType = "application/json"))
    }
    )
    public DeferredResult<BaseResponse<GraphCypherQueryDto>> getQueryById(@PathVariable Long id) {
        return deferShortTimeDb(() -> BaseResponse.success(graphCypherQueryService.getQueryById(id)));
    }

    @PostMapping
    @Operation(description = "쿼리 생성")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphCypherQueryDto.class), mediaType = "application/json"))
    }
    )
    public DeferredResult<BaseResponse<GraphCypherQueryDto>> createQuery(@RequestBody GraphCypherQueryDto dto) {
        return deferShortTimeDb(() -> BaseResponse.success(graphCypherQueryService.createQuery(dto)));
    }

    @PutMapping("/{id}")
    @Operation(description = "쿼리 수정")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphCypherQueryDto.class), mediaType = "application/json"))
    }
    )
    public DeferredResult<BaseResponse<GraphCypherQueryDto>> updateQuery(@PathVariable Long id,
            @RequestBody GraphCypherQueryDto dto) {
        return deferShortTimeDb(() -> BaseResponse.success(graphCypherQueryService.updateQuery(id, dto)));
    }

    @DeleteMapping("/{id}")
    @Operation(description = "쿼리 삭제")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphCypherQueryDto.class), mediaType = "application/json"))
    }
    )
    public DeferredResult<BaseResponse<Void>> deleteQuery(@PathVariable Long id) {
        return deferShortTimeDb(() -> {
            graphCypherQueryService.deleteQuery(id);
            return BaseResponse.success();
        });
    }
}

