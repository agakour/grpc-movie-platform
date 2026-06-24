package com.movieplatform.movie.catalog.repository;
import com.movieplatform.movie.catalog.entity.MovieKeyword;
import com.movieplatform.movie.catalog.entity.MovieKeywordId;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieKeywordRepository extends JpaRepository<MovieKeyword, MovieKeywordId> {

	List<MovieKeyword> findByMovieIdOrderByPosition(Long movieId);

	void deleteByMovieId(Long movieId);

}
