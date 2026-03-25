package com.empasy.graph.api.repository;

import com.empasy.graph.api.entity.GraphStyle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GraphStyleRepository extends JpaRepository<GraphStyle, Long>{

    Optional<GraphStyle> findByLabelAndElementType(String label, String elementType);
}

