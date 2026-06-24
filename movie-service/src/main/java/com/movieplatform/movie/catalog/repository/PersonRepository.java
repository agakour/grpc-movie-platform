package com.movieplatform.movie.catalog.repository;
import com.movieplatform.movie.catalog.entity.Person;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, Integer> {

	List<Person> findAllByNameIn(Collection<String> names);

}
