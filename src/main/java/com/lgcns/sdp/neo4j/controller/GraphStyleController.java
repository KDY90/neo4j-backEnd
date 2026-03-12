package com.lgcns.sdp.neo4j.controller;

import com.lgcns.sdp.neo4j.dto.GraphDetailDto;
import com.lgcns.sdp.neo4j.dto.GraphStyleRequestDto;
import com.lgcns.sdp.neo4j.entity.GraphStyle;
import com.lgcns.sdp.neo4j.service.GraphStyleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/graph/style")
@RequiredArgsConstructor
public class GraphStyleController {

    private final GraphStyleService graphStyleService;

    @PostMapping
    @Operation(description = "라벨 스타일 저장")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphStyleRequestDto.class), mediaType = "application/json"))
    }
    )
    public ResponseEntity<GraphStyle> saveGraphStyle(@RequestBody GraphStyleRequestDto requestDto) {
        GraphStyle savedStyle = graphStyleService.saveStyle(requestDto);
        return ResponseEntity.ok(savedStyle);
    }
}
