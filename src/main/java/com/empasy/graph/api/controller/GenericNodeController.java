package com.empasy.graph.api.controller;

import com.empasy.graph.api.dto.*;
import com.empasy.graph.api.service.GenericNodeService;
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

import java.util.Collection;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/nodes")
@RequiredArgsConstructor
public class GenericNodeController extends BaseRestControllerV2 {

    private final GenericNodeService genericNodeService;

    @GetMapping("/{label}")
    @Operation(description = "라벨 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server")
    }
    )
    public DeferredResult<BaseResponse<Collection<Map<String, Object>>>> getNodesByLabel(@PathVariable String label) {
        return deferShortTimeDb(() -> BaseResponse.success(genericNodeService.findAllByLabel(label)));
    }

    @PostMapping("/table")
    @Operation(description = "라벨 리스트 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphLabelNodesResponseDto.class), mediaType = "application/json"))
    }
    )
    public DeferredResult<BaseResponse<GraphLabelNodesResponseDto>> getNodesByLabelForTable(@RequestBody GraphLabelNodesRequestDto requestDto) {
        return deferShortTimeDb(() -> BaseResponse.success(genericNodeService.getNodesByLabelForTable(requestDto)));
    }

    @GetMapping("/{elementId}/children")
    @Operation(description = "라벨 하위 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphNodeChildrenResponseDto.class), mediaType = "application/json"))
    }
    )
    public DeferredResult<BaseResponse<GraphNodeChildrenResponseDto>> getChildren(@PathVariable String elementId) {
        return deferShortTimeDb(() -> BaseResponse.success(genericNodeService.getChildrenNodes(elementId)));
    }

    @PostMapping("/create")
    @Operation(description = "노드 생성")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphCreateNodeResponseDto.class), mediaType = "application/json"))
    }
    )
    public DeferredResult<BaseResponse<GraphCreateNodeResponseDto>> createNode(@RequestBody GraphCreateNodeRequestDto requestDto) {
        return deferShortTimeDb(() -> BaseResponse.success(genericNodeService.createNode(requestDto)));
    }

    @PutMapping("/{id}")
    @Operation(description = "노드 수정")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphCreateNodeResponseDto.class), mediaType = "application/json"))
    }
    )
    public DeferredResult<BaseResponse<GraphCreateNodeResponseDto>> updateNode(@PathVariable String id,
                                                                 @RequestBody GraphUpdateNodeRequestDto requestDto) {
        return deferShortTimeDb(() -> BaseResponse.success(genericNodeService.updateNode(id, requestDto)));
    }

    @DeleteMapping("/{id}")
    @Operation(description = "노드 삭제")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server")
    }
    )
    public DeferredResult<BaseResponse<Void>> deleteNode(@PathVariable String id) {
        return deferShortTimeDb(() -> {
            genericNodeService.deleteNode(id);
            return BaseResponse.success();
        });
    }
}

