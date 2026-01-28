package com.lgcns.sdp.neo4j.repository;

import com.lgcns.sdp.neo4j.constant.GraphQueryType;
import com.lgcns.sdp.neo4j.dto.GraphLabelCountDto;
import com.lgcns.sdp.neo4j.dto.GraphSchemaDto;
import com.lgcns.sdp.neo4j.dto.GraphSearchBarDto;
import com.lgcns.sdp.neo4j.util.GraphUtil;
import lombok.RequiredArgsConstructor;
import org.neo4j.driver.Value;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class GraphCommonRepository {

    private final Neo4jClient neo4jClient;
    private final GraphUtil graphUtil;


    @Transactional(readOnly = true)
    public Collection<GraphSchemaDto> findSchemaInfo() {
        return neo4jClient.query(GraphQueryType.SCHEMA_INFO.getQuery()) // Enum 호출
                .fetchAs(GraphSchemaDto.class)
                .mappedBy((typeSystem, record) -> {
                    String label = record.get("label").asString();
                    List<Value> propsList = record.get("propsList").asList(v -> v);

                    Map<String, String> propertiesMap = new HashMap<>();
                    for (Value val : propsList) {
                        propertiesMap.put(val.get("name").asString(), val.get("type").asString());
                    }
                    return new GraphSchemaDto(label, propertiesMap);
                })
                .all();
    }

    @Transactional(readOnly = true)
    public Collection<GraphLabelCountDto> getLabelCounts() {
        return neo4jClient.query(GraphQueryType.LABEL_COUNTS.getQuery()) // Enum 호출
                .fetchAs(GraphLabelCountDto.class)
                .mappedBy((typeSystem, record) -> new GraphLabelCountDto(
                        record.get("label").asString(),
                        record.get("count").asLong()))
                .all();
    }

    @Transactional(readOnly = true)
    public GraphSearchBarDto findSearchBarSchema() {
        return neo4jClient.query(GraphQueryType.SEARCH_BAR.getQuery())
                .fetchAs(GraphSearchBarDto.class)
                .mappedBy((typeSystem, record) -> {

                    // [1] 스타일 조회를 위한 로컬 캐시 생성 (요청 1번당 1개)
                    Map<String, Map<String, Object>> styleCache = new HashMap<>();

                    Value schema = record.get("schema");

                    // --- 1. 노드 처리 ---
                    List<Value> nodesList = schema.get("nodes").asList(v -> v);
                    List<GraphSearchBarDto.NodeSchema> nodeSchemas = nodesList.stream()
                            .map(nodeVal -> {
                                String label = nodeVal.get("label").asString();

                                List<Value> propsVal = nodeVal.get("properties").asList(v -> v);
                                List<GraphSearchBarDto.PropertySchema> propertySchemas = propsVal
                                        .stream()
                                        .map(p -> new GraphSearchBarDto.PropertySchema(
                                                p.get("key").asString(),
                                                p.get("type").asString()))
                                        .toList();

                                // [2] 노드 스타일 조회 (캐시 사용)
                                Map<String, Object> style = graphUtil.getStyleConfig(label, "NODE", styleCache);

                                // 생성자에 style Map 전달 -> DTO 내부 @JsonAnyGetter가 처리
                                return new GraphSearchBarDto.NodeSchema(label, propertySchemas, style);
                            }).toList();

                    // --- 2. 관계(Relationship) 처리 ---
                    List<Value> relsList = schema.get("relationships").asList(v -> v);
                    List<GraphSearchBarDto.RelationshipSchema> relSchemas = relsList.stream()
                            .map(relVal -> {
                                String relationshipName = relVal.get("relationship").asString();

                                List<Value> connectionVals = relVal.get("list").asList(v -> v);
                                List<GraphSearchBarDto.ConnectionSchema> connections = connectionVals.stream()
                                        .map(conn -> new GraphSearchBarDto.ConnectionSchema(
                                                conn.get("header").asString(),
                                                conn.get("tail").asString()
                                        ))
                                        .toList();

                                // [3] 관계 스타일 조회 (캐시 사용)
                                Map<String, Object> style = graphUtil.getStyleConfig(relationshipName, "RELATIONSHIP", styleCache);

                                // 생성자에 style Map 전달
                                return new GraphSearchBarDto.RelationshipSchema(relationshipName, connections, style);
                            })
                            .toList();

                    return new GraphSearchBarDto(nodeSchemas, relSchemas);
                })
                .one()
                .orElse(new GraphSearchBarDto(Collections.emptyList(), Collections.emptyList()));
    }

    // [3] 외부 쿼리 실행기 (보안 강화 버전)
    // readOnly = true: DB 드라이버 차원에서 쓰기 시도 시 예외 발생시킴
    @Transactional(readOnly = true)
    public Collection<Map<String, Object>> executeRawCypher(String cypherQuery) {

        // 1차 방어: 금칙어 검사 (대소문자 무시)
        String upperQuery = cypherQuery.toUpperCase().trim();

        // 위험한 키워드가 있으면 즉시 차단
        if (upperQuery.contains("DELETE") ||
                upperQuery.contains("DETACH") ||
                upperQuery.contains("CREATE") ||
                upperQuery.contains("SET") ||
                upperQuery.contains("MERGE") ||
                upperQuery.contains("REMOVE")) {
            throw new IllegalArgumentException("보안 경고: 데이터 변경 쿼리(DELETE, CREATE 등)는 실행할 수 없습니다.");
        }

        // 2차 방어: @Transactional(readOnly=true)에 의해 쓰기 작업 시 롤백/예외 발생
        return neo4jClient.query(cypherQuery)
                .fetch()
                .all();
    }

}