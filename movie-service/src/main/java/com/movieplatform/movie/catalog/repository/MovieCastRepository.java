package com.movieplatform.movie.catalog.repository;
import com.movieplatform.movie.catalog.entity.MovieCast;
import com.movieplatform.movie.catalog.entity.MovieCastId;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieCastRepository extends JpaRepository<MovieCast, MovieCastId> {

	List<MovieCast> findByMovieIdOrderByPosition(Long movieId);

	void deleteByMovieId(Long movieId);

}
