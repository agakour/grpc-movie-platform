package com.movieplatform.movie.catalog.repository;
import com.movieplatform.movie.catalog.entity.MovieGenre;
import com.movieplatform.movie.catalog.entity.MovieGenreId;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieGenreRepository extends JpaRepository<MovieGenre, MovieGenreId> {

	List<MovieGenre> findByMovieIdOrderByPosition(Long movieId);

	void deleteByMovieId(Long movieId);

}
