package com.lgcns.sdp.neo4j.controller;

import com.lgcns.sdp.neo4j.dto.GraphSceneDto;
import com.lgcns.sdp.neo4j.dto.GraphStyleRequestDto;
import com.lgcns.sdp.neo4j.service.GraphSceneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/graph-scene")
@RequiredArgsConstructor
public class GraphSceneController {

    private final GraphSceneService graphSceneService;

    @GetMapping
    @Operation(description = "모든 씬 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphSceneDto.class), mediaType = "application/json"))
    }
    )
    public ResponseEntity<List<GraphSceneDto>> getAllScenes() {
        return ResponseEntity.ok(graphSceneService.getAllScenes());
    }

    @GetMapping("/{id}")
    @Operation(description = "씬 디테일 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphSceneDto.class), mediaType = "application/json"))
    }
    )
    public ResponseEntity<GraphSceneDto> getSceneById(@PathVariable Long id) {
        return ResponseEntity.ok(graphSceneService.getSceneById(id));
    }

    @PostMapping
    @Operation(description = "씬 생성")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphSceneDto.class), mediaType = "application/json"))
    }
    )
    public ResponseEntity<GraphSceneDto> createScene(@RequestBody GraphSceneDto dto) {
        return ResponseEntity.ok(graphSceneService.createScene(dto));
    }

    @PutMapping("/{id}")
    @Operation(description = "씬수정")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphSceneDto.class), mediaType = "application/json"))
    }
    )
    public ResponseEntity<GraphSceneDto> updateScene(@PathVariable Long id, @RequestBody GraphSceneDto dto) {
        return ResponseEntity.ok(graphSceneService.updateScene(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(description = "씬 삭제")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server")
    }
    )
    public ResponseEntity<Void> deleteScene(@PathVariable Long id) {
        graphSceneService.deleteScene(id);
        return ResponseEntity.ok().build();
    }
}

