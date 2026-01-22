package com.lgcns.sdp.neo4j.repository;

import com.lgcns.sdp.neo4j.entity.Movie;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MovieRepository extends Neo4jRepository<Movie, Long> {
    Optional<Movie> findOneByTitle(String title);
}
