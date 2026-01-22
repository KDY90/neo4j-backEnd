package com.lgcns.sdp.neo4j.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GraphQueryType {

    // 1. 전체 스키마(라벨+프로퍼티 타입) 조회
    SCHEMA_INFO("""
            CALL db.schema.nodeTypeProperties()
            YIELD nodeType, propertyName, propertyTypes
            WITH replace(nodeType, ':', '') AS label,
                 propertyName,
                 head(propertyTypes) AS type
            RETURN label,
                   collect({name: propertyName, type: type}) AS propsList
            """),

    // 2. 전체 라벨별 데이터 개수 조회 (Bloom용)
    LABEL_COUNTS("""
            MATCH (n)
            UNWIND labels(n) AS label
            RETURN label, count(n) AS count
            ORDER BY count DESC
            """),


    SEARCH_BAR("""
            CALL apoc.meta.data()
            YIELD label, property, type, elementType
            WHERE elementType = 'node'
            WITH label, collect({key: property, type: type}) AS props
            WITH collect({label: label, properties: props}) AS nodeSchema
            CALL db.schema.visualization() YIELD relationships
            UNWIND relationships AS rel
            WITH nodeSchema,
            type(rel) AS relType,
            labels(startNode(rel))[0] AS header,
            labels(endNode(rel))[0] AS tail
            WITH nodeSchema, relType, collect({header: header, tail: tail}) AS connections
            WITH nodeSchema, collect({relationship: relType, list: connections}) AS relSchema
            RETURN {
            nodes: nodeSchema,
            relationships: relSchema
            } AS schema
            """);

    private final String query;
}
