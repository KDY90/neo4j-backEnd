package com.lgcns.sdp.neo4j.repository;

import com.lgcns.sdp.neo4j.entity.GraphScene;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GraphSceneRepository extends JpaRepository<GraphScene, Long>{

    Optional<GraphScene> findBySceneName(String sceneName);

    Optional<GraphScene> findTopByOrderByIdDesc();

}
