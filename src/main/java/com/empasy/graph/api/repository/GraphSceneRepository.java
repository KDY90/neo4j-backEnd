package com.empasy.graph.api.repository;

import com.empasy.graph.api.entity.GraphScene;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GraphSceneRepository extends JpaRepository<GraphScene, Long>{

    List<GraphScene> findAllByOrderByIdDesc();

    Optional<GraphScene> findTopByOrderByIdDesc();

}

