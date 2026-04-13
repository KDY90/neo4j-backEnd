package com.empasy.graph.api.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GraphQueryType {

     
    SCHEMA_INFO("""
            CALL db.schema.nodeTypeProperties()
            YIELD nodeType, propertyName, propertyTypes
            WITH replace(nodeType, ':', '') AS label,
                 propertyName,
                 head(propertyTypes) AS type
            RETURN label,
                   collect({name: propertyName, type: type}) AS propsList
            """),

     
    LABEL_COUNTS("""
            MATCH (n)
            UNWIND labels(n) AS label
            RETURN label, count(n) AS count
            ORDER BY count DESC
            """),


    SEARCH_BAR("""
        CALL apoc.meta.nodeTypeProperties()
        YIELD nodeLabels, propertyName, propertyTypes
        UNWIND nodeLabels AS label
        WITH DISTINCT label, propertyName AS property, propertyTypes[0] AS type
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

