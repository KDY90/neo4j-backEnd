package com.empasy.graph.api.controller;

import com.empasy.graph.api.dto.GraphStyleRequestDto;
import com.empasy.graph.api.entity.GraphStyle;
import com.empasy.graph.api.service.GraphStyleService;
import com.empasy.graph.api.support.BaseResponse;
import com.empasy.graph.api.support.BaseRestControllerV2;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

@RestController
@RequestMapping("/api/v1/graph/style")
@RequiredArgsConstructor
public class GraphStyleController extends BaseRestControllerV2 {

    private final GraphStyleService graphStyleService;

    @PostMapping
    @Operation(description = "라벨 스타일 저장")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get results from server",
                    content = @Content(schema = @Schema(implementation = GraphStyleRequestDto.class), mediaType = "application/json"))
    }
    )
    public DeferredResult<BaseResponse<GraphStyle>> saveGraphStyle(@RequestBody GraphStyleRequestDto requestDto) {
        return deferShortTimeDb(() -> BaseResponse.success(graphStyleService.saveStyle(requestDto)));
    }
}
