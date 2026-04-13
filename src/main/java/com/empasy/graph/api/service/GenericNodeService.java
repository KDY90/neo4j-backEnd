package com.empasy.graph.api.service;

import com.empasy.graph.api.annotation.Neo4jTransactional;
import com.empasy.graph.api.dto.*;
import com.empasy.graph.api.util.GraphUtil;

import lombok.RequiredArgsConstructor;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.cypherdsl.core.renderer.Configuration;
import org.neo4j.cypherdsl.core.renderer.Dialect;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.neo4j.driver.types.Node;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
@Neo4jTransactional(readOnly = true)
public class GenericNodeService {

    private final Neo4jClient neo4jClient;
    private final GraphUtil graphUtil;

    public Collection<Map<String, Object>> findAllByLabel(String label) {
        String query = String.format("MATCH (n:%s) RETURN n{.*, id: elementId(n)} as data", label);
        return neo4jClient.query(query).fetch().all();
    }

    public GraphLabelNodesResponseDto getNodesByLabelForTable(GraphLabelNodesRequestDto requestDto) {
        String label = requestDto.getLabel();
        if (label == null || label.trim().isEmpty()) {
            throw new IllegalArgumentException("Label cannot be empty");
        }

        int pageIndex = requestDto.getPageIndex();
        int pageSize = requestDto.getPageSize();
        int skip = pageIndex * pageSize;

        List<Map<String, Object>> blocks = requestDto.getCypherBlocks();

        String countQuery;
        String dataQuery;

        // 1. cypherBlocks 배열이 전달되었을 때 (1개인 경우, 3개인 경우 모두 커버)
        if (blocks != null && !blocks.isEmpty()) {
            StringBuilder matchClause = new StringBuilder("MATCH ");
            String targetVar = "n0"; // 기본 리턴 변수

            for (int i = 0; i < blocks.size(); i++) {
                Map<String, Object> block = blocks.get(i);
                String type = (String) block.get("type");
                String blockLabel = (String) block.get("label");

                if ("NODE".equals(type) || i % 2 == 0) {
                    matchClause.append("(n").append(i);
                    if (blockLabel != null && !"ANY".equals(blockLabel)) {
                        matchClause.append(":`").append(blockLabel).append("`");
                    }
                    matchClause.append(")");

                    // 현재 루프의 노드 라벨이 테이블에서 조회하려는 주 라벨과 같으면 리턴 타겟으로 지정
                    if (label.equals(blockLabel)) {
                        targetVar = "n" + i;
                    }
                } else if ("RELATIONSHIP".equals(type) || i % 2 != 0) {
                    String direction = (String) block.get("direction");
                    String relTypeStr = (blockLabel != null && !"ANY".equals(blockLabel)) ? ":`" + blockLabel + "`" : "";

                    if ("OUT".equalsIgnoreCase(direction)) {
                        matchClause.append("-[r").append(i).append(relTypeStr).append("]->");
                    } else if ("IN".equalsIgnoreCase(direction)) {
                        matchClause.append("<-[r").append(i).append(relTypeStr).append("]-");
                    } else {
                        matchClause.append("-[r").append(i).append(relTypeStr).append("]-");
                    }
                }
            }

            countQuery = matchClause + " RETURN count(DISTINCT " + targetVar + ") AS total";

            dataQuery = matchClause +
                    " WITH DISTINCT " + targetVar + " AS root " +
                    " RETURN root, exists((root)<-[]-()) AS hasChildren " +
                    " SKIP " + skip + " LIMIT " + pageSize;

        } else {
            countQuery = "MATCH (n:`" + label + "`) RETURN count(n) AS total";
            dataQuery = "MATCH (root:`" + label + "`) RETURN root, exists((root)<-[]-()) AS hasChildren SKIP " + skip + " LIMIT " + pageSize;
        }

        // 카운트 실행
        Long rowCount = neo4jClient.query(countQuery).fetchAs(Long.class).one().orElse(0L);

        // 데이터 실행
        Collection<Map<String, Object>> rawResults = neo4jClient.query(dataQuery).fetch().all();

        List<Map<String, Object>> finalData = new ArrayList<>();

        for (Map<String, Object> row : rawResults) {
            Node rootNode = (Node) row.get("root");
            Boolean hasChildren = (Boolean) row.get("hasChildren");

            Map<String, Object> nodeData = new HashMap<>(rootNode.asMap());
            nodeData.put("id", rootNode.elementId());
            nodeData.put("label", rootNode.labels().iterator().hasNext() ? rootNode.labels().iterator().next() : "");
            nodeData.put("hasChildren", hasChildren);

            finalData.add(nodeData);
        }

        return GraphLabelNodesResponseDto.builder()
                .data(finalData)
                .rowCount(rowCount)
                .build();
    }

    public GraphNodeChildrenResponseDto getChildrenNodes(String elementId) {
        if (elementId == null || elementId.trim().isEmpty()) {
            throw new IllegalArgumentException("elementId cannot be empty");
        }

        org.neo4j.cypherdsl.core.Node parent = Cypher.anyNode("parent");
        org.neo4j.cypherdsl.core.Node child = Cypher.anyNode("child");

        Statement statement = Cypher.match(parent.relationshipFrom(child))
                .where(parent.elementId().isEqualTo(Cypher.parameter("elementId")))
                .returning(
                        child.asExpression(),
                        Cypher.exists(child.relationshipFrom(Cypher.anyNode())).as("hasChildren")
                )
                .build();

        String query = Renderer.getRenderer(Configuration.newConfig().withDialect(Dialect.NEO4J_5).build()).render(statement);

        Collection<Map<String, Object>> rawResults = neo4jClient.query(query)
                .bind(elementId).to("elementId")
                .fetch().all();

        List<Map<String, Object>> finalData = new ArrayList<>();
        for (Map<String, Object> row : rawResults) {
            Node childNode = (Node) row.get("child");
            Boolean hasChildren = (Boolean) row.get("hasChildren");

            Map<String, Object> nodeData = new HashMap<>(childNode.asMap());
            nodeData.put("id", childNode.elementId());
            nodeData.put("label", childNode.labels().iterator().hasNext() ? childNode.labels().iterator().next() : "");
            nodeData.put("hasChildren", hasChildren);

            finalData.add(nodeData);
        }

        return GraphNodeChildrenResponseDto.builder()
                .data(finalData)
                .build();
    }

    @Neo4jTransactional
    public GraphCreateNodeResponseDto createNode(GraphCreateNodeRequestDto requestDto) {
        String label = requestDto.getLabel();
        Map<String, Object> properties = requestDto.getProperties();

         
        if (label == null || label.trim().isEmpty()) {
            throw new IllegalArgumentException("Label cannot be empty");
        }

         
        String query = String.format("CREATE (n:`%s` $props) RETURN n", label);

         
        return neo4jClient.query(query)
                .bind(properties).to("props")
                .fetchAs(GraphCreateNodeResponseDto.class)
                .mappedBy((typeSystem, record) -> {
                     
                    Node node = record.get("n").asNode();
                    String nodeLabel = node.labels().iterator().next();

                    Map<String, Object> styleConfig = graphUtil.getStyleConfig(
                            nodeLabel,
                            "NODE",
                            new HashMap<>());

                     
                    return GraphCreateNodeResponseDto.builder()
                            .elementId(node.elementId())
                            .label(nodeLabel)
                            .properties(node.asMap())
                            .style(styleConfig)
                            .build();
                })
                .one()
                .orElseThrow(() -> new RuntimeException("Failed to create node"));
    }

    @Neo4jTransactional
    public GraphCreateNodeResponseDto updateNode(String elementId, GraphUpdateNodeRequestDto requestDto) {

        requestDto.getProperties().remove("labels");

        String query = "MATCH (n) WHERE elementId(n) = $elementId SET n += $props RETURN n";

        return neo4jClient.query(query)
                .bind(elementId).to("elementId")
                .bind(requestDto.getProperties()).to("props")
                .fetchAs(GraphCreateNodeResponseDto.class)
                .mappedBy((typeSystem, record) -> {
                    Node node = record.get("n").asNode();
                    String nodeLabel = node.labels().iterator().next();

                    Map<String, Object> styleConfig = graphUtil.getStyleConfig(
                            nodeLabel,
                            "NODE",
                            new HashMap<>());

                    return GraphCreateNodeResponseDto.builder()
                            .elementId(node.elementId())
                            .label(nodeLabel)
                            .properties(node.asMap())
                            .style(styleConfig)
                            .build();
                })
                .one()
                .orElseThrow(() -> new RuntimeException("Failed to update node with id: " + elementId));
    }

    @Neo4jTransactional
    public void deleteNode(String elementId) {
        String query = "MATCH (n) WHERE elementId(n) = $elementId DETACH DELETE n";
        neo4jClient.query(query)
                .bind(elementId).to("elementId")
                .run();
    }
}

