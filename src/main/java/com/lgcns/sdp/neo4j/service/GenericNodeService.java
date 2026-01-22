package com.lgcns.sdp.neo4j.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GenericNodeService {

    private final Neo4jClient neo4jClient;

    public Collection<Map<String, Object>> findAllByLabel(String label) {
        // Warning: Label cannot be parameterized in Cypher.
        // Ensure strictly alphanumeric or validate in a real scenario to prevent Cypher
        // injection.
        String query = String.format("MATCH (n:%s) RETURN n{.*, id: elementId(n)} as data", label);
        return neo4jClient.query(query).fetch().all();
    }

    public Map<String, Object> createNode(String label, Map<String, Object> properties) {
        String query = String.format("CREATE (n:%s $props) RETURN n", label);
        return neo4jClient.query(query)
                .bind(properties).to("props")
                .fetch().one().orElse(null);
    }
}
