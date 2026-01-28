package com.lgcns.sdp.neo4j.util;

import com.lgcns.sdp.neo4j.entity.GraphStyle;
import com.lgcns.sdp.neo4j.repository.GraphStyleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GraphUtil {

    private final GraphStyleRepository graphStyleRepository;

    /**
     * 스타일 설정 공통 조회 메서드
     */
    public Map<String, Object> getStyleConfig(String label, String elementType, Map<String, Map<String, Object>> cache) {
        if (label == null || label.isEmpty()) return null;

        String cacheKey = label + "_" + elementType;

        // 1. 캐시에 있으면 반환
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }

        // 2. DB 조회
        Optional<GraphStyle> styleOpt = graphStyleRepository.findByLabelAndElementType(label, elementType);
        Map<String, Object> result = styleOpt.map(GraphStyle::getStyleConfig).orElse(null);

        // 3. 캐시 저장
        cache.put(cacheKey, result);

        return result;
    }
}