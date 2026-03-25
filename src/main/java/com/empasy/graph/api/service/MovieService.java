package com.empasy.graph.api.service;

import com.empasy.graph.api.entity.Movie;
import com.empasy.graph.api.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;

    public List<Movie> getAllMovies() {
        return movieRepository.findAll();
    }

    public Optional<Movie> getMovieByTitle(String title) {
        return movieRepository.findOneByTitle(title);
    }

    public Movie createMovie(Movie movie) {
        return movieRepository.save(movie);
    }
}
