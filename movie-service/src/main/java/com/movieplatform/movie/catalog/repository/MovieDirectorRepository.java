package com.movieplatform.movie.catalog.repository;
import com.movieplatform.movie.catalog.entity.MovieDirector;
import com.movieplatform.movie.catalog.entity.MovieDirectorId;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieDirectorRepository extends JpaRepository<MovieDirector, MovieDirectorId> {

	List<MovieDirector> findByMovieIdOrderByPosition(Long movieId);

	void deleteByMovieId(Long movieId);

}
