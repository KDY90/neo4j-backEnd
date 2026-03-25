package com.empasy.graph.api.service;

import com.empasy.graph.api.dto.GraphStyleRequestDto;
import com.empasy.graph.api.entity.GraphStyle;
import com.empasy.graph.api.repository.GraphStyleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GraphStyleService {

    private final GraphStyleRepository graphStyleRepository;

    @Transactional
    public GraphStyle saveStyle(GraphStyleRequestDto requestDto) {
        GraphStyle graphStyle = graphStyleRepository
                .findByLabelAndElementType(requestDto.getLabel(), requestDto.getElementType())
                .orElse(null);

        if (graphStyle != null) {
            graphStyle.updateStyleConfig(requestDto.getStyleConfig());
            return graphStyle;
        } else {
            GraphStyle newStyle = GraphStyle.builder()
                    .label(requestDto.getLabel())
                    .elementType(requestDto.getElementType())
                    .styleConfig(requestDto.getStyleConfig())
                    .build();
            return graphStyleRepository.save(newStyle);
        }
    }
}
