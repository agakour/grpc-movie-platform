package com.movieplatform.movie.catalog.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.movieplatform.movie.catalog.entity.Genre;
import com.movieplatform.movie.catalog.repository.GenreRepository;
import com.movieplatform.movie.catalog.entity.Keyword;
import com.movieplatform.movie.catalog.repository.KeywordRepository;
import com.movieplatform.movie.catalog.entity.Person;
import com.movieplatform.movie.catalog.repository.PersonRepository;

@Service
@Transactional
@SuppressWarnings("unused")
public class VocabularyService {

	private final PersonRepository personRepository;
	private final GenreRepository genreRepository;
	private final KeywordRepository keywordRepository;

	public VocabularyService(PersonRepository personRepository,
			GenreRepository genreRepository,
			KeywordRepository keywordRepository) {
		this.personRepository = personRepository;
		this.genreRepository = genreRepository;
		this.keywordRepository = keywordRepository;
	}

	public List<Person> resolvePersons(List<String> rawNames) {
		if (rawNames == null || rawNames.isEmpty()) {
			return List.of();
		}
		List<String> names = rawNames.stream()
				.map(this::normalizePerson)
				.filter(name -> !name.isEmpty())
				.toList();
		rejectDuplicates(names, "person");

		Map<String, Person> existing = new HashMap<>();
		for (Person person : personRepository.findAllByNameIn(names)) {
			existing.put(person.getName(), person);
		}
		List<Person> result = new ArrayList<>(names.size());
		List<Person> toCreate = new ArrayList<>();
		for (String name : names) {
			Person person = existing.get(name);
			if (person == null) {
				person = new Person(name);
				toCreate.add(person);
			}
			result.add(person);
		}
		if (!toCreate.isEmpty()) {
			personRepository.saveAll(toCreate);
		}
		return result;
	}

	public List<Genre> resolveGenres(List<String> rawNames) {
		if (rawNames == null || rawNames.isEmpty()) {
			return List.of();
		}
		List<String> names = rawNames.stream()
				.map(this::normalizeGenre)
				.filter(name -> !name.isEmpty())
				.toList();
		rejectDuplicates(names, "genre");

		Map<String, Genre> existing = new HashMap<>();
		for (Genre genre : genreRepository.findAllByNameIn(names)) {
			existing.put(genre.getName(), genre);
		}
		List<Genre> result = new ArrayList<>(names.size());
		List<Genre> toCreate = new ArrayList<>();
		for (String name : names) {
			Genre genre = existing.get(name);
			if (genre == null) {
				genre = new Genre(name);
				toCreate.add(genre);
			}
			result.add(genre);
		}
		if (!toCreate.isEmpty()) {
			genreRepository.saveAll(toCreate);
		}
		return result;
	}

	public List<Keyword> resolveKeywords(List<String> rawNames) {
		if (rawNames == null || rawNames.isEmpty()) {
			return List.of();
		}
		List<String> names = rawNames.stream()
				.map(this::normalizeKeyword)
				.filter(name -> !name.isEmpty())
				.toList();
		rejectDuplicates(names, "keyword");

		Map<String, Keyword> existing = new HashMap<>();
		for (Keyword keyword : keywordRepository.findAllByNameIn(names)) {
			existing.put(keyword.getName(), keyword);
		}
		List<Keyword> result = new ArrayList<>(names.size());
		List<Keyword> toCreate = new ArrayList<>();
		for (String name : names) {
			Keyword keyword = existing.get(name);
			if (keyword == null) {
				keyword = new Keyword(name);
				toCreate.add(keyword);
			}
			result.add(keyword);
		}
		if (!toCreate.isEmpty()) {
			keywordRepository.saveAll(toCreate);
		}
		return result;
	}

	public String normalizePerson(String raw) {
		return normalize(raw, false);
	}

	public String normalizeGenre(String raw) {
		return normalize(raw, true);
	}

	public String normalizeKeyword(String raw) {
		return normalize(raw, true);
	}

	private String normalize(String raw, boolean lowercase) {
		String trimmed = raw == null ? "" : raw.trim();
		if (trimmed.contains(",")) {
			throw new IllegalArgumentException("Entry must not contain a comma: '" + trimmed + "'");
		}
		return lowercase ? trimmed.toLowerCase(Locale.ROOT) : trimmed;
	}

	private void rejectDuplicates(List<String> names, String field) {
		Set<String> seen = new HashSet<>();
		for (String name : names) {
			if (!seen.add(name)) {
				throw new IllegalArgumentException(
						"Duplicate " + field + " entry: '" + name + "'");
			}
		}
	}

}
