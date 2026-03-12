package com.lgcns.sdp.neo4j.controller;

import com.lgcns.sdp.neo4j.dto.*;
import com.lgcns.sdp.neo4j.service.GenericNodeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/nodes")
@RequiredArgsConstructor
public class GenericNodeController {

    private final GenericNodeService genericNodeService;

    @GetMapping("/{label}")
    @Operation(description = "라벨 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server")
    }
    )
    public ResponseEntity<Collection<Map<String, Object>>> getNodesByLabel(@PathVariable String label) {
        return ResponseEntity.ok(genericNodeService.findAllByLabel(label));
    }

    @PostMapping("/table")
    @Operation(description = "라벨 리스트 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphLabelNodesResponseDto.class), mediaType = "application/json"))
    }
    )
    public ResponseEntity<GraphLabelNodesResponseDto> getNodesByLabelForTable(@RequestBody GraphLabelNodesRequestDto requestDto) {
        return ResponseEntity.ok(genericNodeService.getNodesByLabelForTable(requestDto));
    }

    @GetMapping("/{elementId}/children")
    @Operation(description = "라벨 하위 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphNodeChildrenResponseDto.class), mediaType = "application/json"))
    }
    )
    public ResponseEntity<GraphNodeChildrenResponseDto> getChildren(@PathVariable String elementId) {
        return ResponseEntity.ok(genericNodeService.getChildrenNodes(elementId));
    }

    @PostMapping("/create")
    @Operation(description = "노드 생성")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphCreateNodeResponseDto.class), mediaType = "application/json"))
    }
    )
    public ResponseEntity<GraphCreateNodeResponseDto> createNode(@RequestBody GraphCreateNodeRequestDto requestDto) {
        GraphCreateNodeResponseDto response = genericNodeService.createNode(requestDto);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(description = "노드 수정")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphCreateNodeResponseDto.class), mediaType = "application/json"))
    }
    )
    public ResponseEntity<GraphCreateNodeResponseDto> updateNode(@PathVariable String id,
                                                                 @RequestBody GraphUpdateNodeRequestDto requestDto) {
        GraphCreateNodeResponseDto response = genericNodeService.updateNode(id, requestDto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(description = "노드 삭제")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server")
    }
    )
    public ResponseEntity<Void> deleteNode(@PathVariable String id) {
        genericNodeService.deleteNode(id);
        return ResponseEntity.noContent().build();
    }
}

