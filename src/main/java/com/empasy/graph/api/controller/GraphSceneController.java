package com.empasy.graph.api.controller;

import com.empasy.graph.api.dto.GraphSceneDto;
import com.empasy.graph.api.service.GraphSceneService;
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
@RequestMapping("/api/v1/graph-scene")
@RequiredArgsConstructor
public class GraphSceneController extends BaseRestControllerV2 {

    private final GraphSceneService graphSceneService;

    @GetMapping
    @Operation(description = "모든 씬 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphSceneDto.class), mediaType = "application/json"))
    }
    )
    public DeferredResult<BaseResponse<List<GraphSceneDto>>> getAllScenes() {
        return deferShortTimeDb(() -> BaseResponse.success(graphSceneService.getAllScenes()));
    }

    @GetMapping("/{id}")
    @Operation(description = "씬 디테일 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphSceneDto.class), mediaType = "application/json"))
    }
    )
    public DeferredResult<BaseResponse<GraphSceneDto>> getSceneById(@PathVariable Long id) {
        return deferShortTimeDb(() -> BaseResponse.success(graphSceneService.getSceneById(id)));
    }

    @PostMapping
    @Operation(description = "씬 생성")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphSceneDto.class), mediaType = "application/json"))
    }
    )
    public DeferredResult<BaseResponse<GraphSceneDto>> createScene(@RequestBody GraphSceneDto dto) {
        return deferShortTimeDb(() -> BaseResponse.success(graphSceneService.createScene(dto)));
    }

    @PutMapping("/{id}")
    @Operation(description = "씬수정")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphSceneDto.class), mediaType = "application/json"))
    }
    )
    public DeferredResult<BaseResponse<GraphSceneDto>> updateScene(@PathVariable Long id, @RequestBody GraphSceneDto dto) {
        return deferShortTimeDb(() -> BaseResponse.success(graphSceneService.updateScene(id, dto)));
    }

    @DeleteMapping("/{id}")
    @Operation(description = "씬 삭제")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server")
    }
    )
    public DeferredResult<BaseResponse<Void>> deleteScene(@PathVariable Long id) {
        return deferShortTimeDb(() -> {
            graphSceneService.deleteScene(id);
            return BaseResponse.success();
        });
    }
}

