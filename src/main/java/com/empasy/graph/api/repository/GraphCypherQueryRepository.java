package com.empasy.graph.api.repository;

import com.empasy.graph.api.entity.GraphCypherQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GraphCypherQueryRepository extends JpaRepository<GraphCypherQuery, Long> {

    List<GraphCypherQuery> findByTitleContaining(String keyword);

    List<GraphCypherQuery> findByQueryType(String queryType);
}

