package com.movieplatform.movie.catalog.repository;
import com.movieplatform.movie.catalog.entity.Movie;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovieRepository extends JpaRepository<Movie, Long> {

	@Query("""
			select m from Movie m
			where (:genre is null or exists (
			        select mg from MovieGenre mg, Genre g
			        where mg.genreId = g.id and mg.movieId = m.id and g.name = :genre))
			  and (:actor0 is null or exists (
			        select mc from MovieCast mc, Person p
			        where mc.personId = p.id and mc.movieId = m.id
			          and regexp_like(p.name, cast(:actor0 as string), 'i') = true
			          and (:actor1 is null or regexp_like(p.name, cast(:actor1 as string), 'i') = true)
			          and (:actor2 is null or regexp_like(p.name, cast(:actor2 as string), 'i') = true)
			          and (:actor3 is null or regexp_like(p.name, cast(:actor3 as string), 'i') = true)
			          and (:actor4 is null or regexp_like(p.name, cast(:actor4 as string), 'i') = true)))
			  and (:director0 is null or exists (
			        select md from MovieDirector md, Person p
			        where md.personId = p.id and md.movieId = m.id
			          and regexp_like(p.name, cast(:director0 as string), 'i') = true
			          and (:director1 is null or regexp_like(p.name, cast(:director1 as string), 'i') = true)
			          and (:director2 is null or regexp_like(p.name, cast(:director2 as string), 'i') = true)
			          and (:director3 is null or regexp_like(p.name, cast(:director3 as string), 'i') = true)
			          and (:director4 is null or regexp_like(p.name, cast(:director4 as string), 'i') = true)))
			  and (:keyword is null or exists (
			        select mk from MovieKeyword mk, Keyword k
			        where mk.keywordId = k.id and mk.movieId = m.id and k.name = :keyword))
			  and (:year is null or m.releaseYear = :year)
			  and (:qPattern is null
			        or (regexp_like(m.title, cast(:qPattern as string), 'i') = true
			            or regexp_like(m.originalTitle, cast(:qPattern as string), 'i') = true))
			""")
	Page<Movie> findFiltered(@Param("genre") String genre,
			@Param("actor0") String actor0,
			@Param("actor1") String actor1,
			@Param("actor2") String actor2,
			@Param("actor3") String actor3,
			@Param("actor4") String actor4,
			@Param("director0") String director0,
			@Param("director1") String director1,
			@Param("director2") String director2,
			@Param("director3") String director3,
			@Param("director4") String director4,
			@Param("keyword") String keyword,
			@Param("year") Integer year,
			@Param("qPattern") String qPattern,
			Pageable pageable);

	List<Movie> findByTmdbIdIn(Collection<Long> tmdbIds);

}
