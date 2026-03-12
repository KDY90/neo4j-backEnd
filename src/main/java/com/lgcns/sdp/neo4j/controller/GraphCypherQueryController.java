package com.lgcns.sdp.neo4j.controller;

import com.lgcns.sdp.neo4j.dto.GraphCypherQueryDto;
import com.lgcns.sdp.neo4j.dto.GraphSchemaDto;
import com.lgcns.sdp.neo4j.service.GraphCypherQueryService;
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
@RequestMapping("/api/v1/graph-cypher-query")
@RequiredArgsConstructor
public class GraphCypherQueryController {

    private final GraphCypherQueryService graphCypherQueryService;

    @GetMapping
    @Operation(description = "모든 쿼리 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphCypherQueryDto.class), mediaType = "application/json"))
    }
    )

    public ResponseEntity<List<GraphCypherQueryDto>> getAllQueries(
            @RequestParam(required = false) String queryType) {
        return ResponseEntity.ok(graphCypherQueryService.getAllQueries(queryType));
    }

    @GetMapping("/valid")
    @Operation(description = "쿼리 valid")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphCypherQueryDto.class), mediaType = "application/json"))
    }
    )
    public ResponseEntity<List<GraphCypherQueryDto>> getValidQueries(
            @RequestParam(required = false) String queryType) {
        return ResponseEntity.ok(graphCypherQueryService.getValidQueries(queryType));
    }

    @GetMapping("/{id}")
    @Operation(description = "쿼리 디테일 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphCypherQueryDto.class), mediaType = "application/json"))
    }
    )
    public ResponseEntity<GraphCypherQueryDto> getQueryById(@PathVariable Long id) {
        return ResponseEntity.ok(graphCypherQueryService.getQueryById(id));
    }

    @PostMapping
    @Operation(description = "쿼리 생성")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphCypherQueryDto.class), mediaType = "application/json"))
    }
    )
    public ResponseEntity<GraphCypherQueryDto> createQuery(@RequestBody GraphCypherQueryDto dto) {
        return ResponseEntity.ok(graphCypherQueryService.createQuery(dto));
    }

    @PutMapping("/{id}")
    @Operation(description = "쿼리 수정")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphCypherQueryDto.class), mediaType = "application/json"))
    }
    )
    public ResponseEntity<GraphCypherQueryDto> updateQuery(@PathVariable Long id,
            @RequestBody GraphCypherQueryDto dto) {
        return ResponseEntity.ok(graphCypherQueryService.updateQuery(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(description = "쿼리 삭제")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphCypherQueryDto.class), mediaType = "application/json"))
    }
    )
    public ResponseEntity<Void> deleteQuery(@PathVariable Long id) {
        graphCypherQueryService.deleteQuery(id);
        return ResponseEntity.ok().build();
    }
}

