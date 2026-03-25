package com.empasy.graph.api.util;

import com.empasy.graph.api.entity.GraphStyle;
import com.empasy.graph.api.repository.GraphStyleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GraphUtil {

    private final GraphStyleRepository graphStyleRepository;

     
    public Map<String, Object> getStyleConfig(String label, String elementType, Map<String, Map<String, Object>> cache) {
        if (label == null || label.isEmpty()) return null;

        String cacheKey = label + "_" + elementType;

         
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }

         
        Optional<GraphStyle> styleOpt = graphStyleRepository.findByLabelAndElementType(label, elementType);
        Map<String, Object> result = styleOpt.map(GraphStyle::getStyleConfig).orElse(null);

         
        cache.put(cacheKey, result);

        return result;
    }
}
