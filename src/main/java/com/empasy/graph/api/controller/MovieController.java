package com.empasy.graph.api.controller;

import com.empasy.graph.api.entity.Movie;
import com.empasy.graph.api.service.MovieService;
import com.empasy.graph.api.support.BaseResponse;
import com.empasy.graph.api.support.BaseRestControllerV2;
import com.empasy.graph.api.support.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;

@RestController
@RequestMapping("/movies")
@RequiredArgsConstructor
public class MovieController extends BaseRestControllerV2 {

    private final MovieService movieService;

    @GetMapping
    public DeferredResult<BaseResponse<List<Movie>>> getAllMovies() {
        return deferShortTimeDb(() -> BaseResponse.success(movieService.getAllMovies()));
    }

    @GetMapping("/{title}")
    public DeferredResult<BaseResponse<Movie>> getMovieByTitle(@PathVariable String title) {
        return deferShortTimeDb(() -> movieService.getMovieByTitle(title)
                .map(BaseResponse::success)
                .orElse(BaseResponse.of(ResultCode.NO_DATA)));
    }

    @PostMapping
    public DeferredResult<BaseResponse<Movie>> createMovie(@RequestBody Movie movie) {
        return deferShortTimeDb(() -> BaseResponse.success(movieService.createMovie(movie)));
    }
}
