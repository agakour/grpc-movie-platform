package com.movieplatform.movie.catalog.repository;
import com.movieplatform.movie.catalog.entity.Keyword;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface KeywordRepository extends JpaRepository<Keyword, Integer> {

	List<Keyword> findAllByNameIn(Collection<String> names);

}
