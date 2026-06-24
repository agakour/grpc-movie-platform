package com.movieplatform.movie.catalog.repository;
import com.movieplatform.movie.catalog.entity.Genre;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GenreRepository extends JpaRepository<Genre, Integer> {

	List<Genre> findAllByNameIn(Collection<String> names);

}
