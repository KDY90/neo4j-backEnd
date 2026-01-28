package com.lgcns.sdp.neo4j.controller;

import com.lgcns.sdp.neo4j.dto.GraphStyleRequestDto;
import com.lgcns.sdp.neo4j.entity.GraphStyle;
import com.lgcns.sdp.neo4j.service.GraphStyleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/graph/style")
@RequiredArgsConstructor
public class GraphStyleController {

    private final GraphStyleService graphStyleService;

    @PostMapping
    public ResponseEntity<GraphStyle> saveGraphStyle(@RequestBody GraphStyleRequestDto requestDto) {
        GraphStyle savedStyle = graphStyleService.saveStyle(requestDto);
        return ResponseEntity.ok(savedStyle);
    }
}