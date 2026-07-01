package com.movieplatform.movie.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.movieplatform.movie.catalog.entity.Genre;
import com.movieplatform.movie.catalog.repository.GenreRepository;
import com.movieplatform.movie.catalog.entity.Keyword;
import com.movieplatform.movie.catalog.repository.KeywordRepository;
import com.movieplatform.movie.catalog.entity.Person;
import com.movieplatform.movie.catalog.repository.PersonRepository;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unused")
class VocabularyServiceTest {

	@Mock
	private PersonRepository personRepository;
	@Mock
	private GenreRepository genreRepository;
	@Mock
	private KeywordRepository keywordRepository;

	private VocabularyService service;

	@BeforeEach
	void setUp() {
		service = new VocabularyService(personRepository, genreRepository, keywordRepository);
	}

	@Test
	void resolvePersonsDoesNotLowercaseAndCreatesMissing() {
		Person existing = new Person("Jean RENOIR");
		existing.setId(1);
		when(personRepository.findAllByNameIn(List.of("Jean RENOIR", "Aki"))).thenReturn(List.of(existing));
		when(personRepository.saveAll(any())).thenAnswer(invocation -> {
			List<Person> created = invocation.getArgument(0);
			created.getFirst().setId(2);
			return created;
		});

		List<Person> resolved = service.resolvePersons(List.of("Jean RENOIR", " Aki ", ""));

		assertThat(resolved).hasSize(2);
		assertThat(resolved.getFirst().getName()).isEqualTo("Jean RENOIR");
		assertThat(resolved.get(1).getName()).isEqualTo("Aki");

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Person>> created = ArgumentCaptor.forClass(List.class);
		verify(personRepository).saveAll(created.capture());
		assertThat(created.getValue()).extracting(Person::getName).containsExactly("Aki");
	}

	@Test
	void resolveGenresLowercasesNames() {
		Genre drama = new Genre("drama");
		drama.setId(1);
		when(genreRepository.findAllByNameIn(List.of("drama", "action"))).thenReturn(List.of(drama));
		when(genreRepository.saveAll(any())).thenAnswer(invocation -> {
			List<Genre> created = invocation.getArgument(0);
			created.getFirst().setId(2);
			return created;
		});

		List<Genre> resolved = service.resolveGenres(List.of(" DRAMA ", "ACTION"));

		assertThat(resolved).extracting(Genre::getName).containsExactly("drama", "action");
	}

	@Test
	void resolveKeywordsDropsBlankEntries() {
		Keyword kw = new Keyword("kw one");
		kw.setId(1);
		when(keywordRepository.findAllByNameIn(List.of("kw one"))).thenReturn(List.of(kw));

		List<Keyword> resolved = service.resolveKeywords(List.of("  KW ONE ", "   ", ""));

		assertThat(resolved).extracting(Keyword::getName).containsExactly("kw one");
		verify(keywordRepository, never()).saveAll(any());
	}

	@Test
	void resolvePersonsRejectsDuplicateEntriesAfterNormalization() {
		assertThatThrownBy(() -> service.resolvePersons(List.of("Jean Renoir", " Jean Renoir ")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Duplicate person entry");
		verify(personRepository, never()).findAllByNameIn(any());
	}

	@Test
	void resolveGenresRejectsDuplicateEntriesAfterNormalization() {
		assertThatThrownBy(() -> service.resolveGenres(List.of("Drama", "DRAMA", "action")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Duplicate genre entry: 'drama'");
		verify(genreRepository, never()).findAllByNameIn(any());
	}

	@Test
	void resolveKeywordsRejectsDuplicateEntriesAfterNormalization() {
		assertThatThrownBy(() -> service.resolveKeywords(List.of("kw one", " KW ONE ")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Duplicate keyword entry: 'kw one'");
		verify(keywordRepository, never()).findAllByNameIn(any());
	}

	@Test
	void resolveNullListReturnsEmptyWithoutRepositoryAccess() {
		assertThat(service.resolvePersons(null)).isEmpty();
		assertThat(service.resolveGenres(null)).isEmpty();
		assertThat(service.resolveKeywords(null)).isEmpty();

		verify(personRepository, never()).findAllByNameIn(any());
		verify(genreRepository, never()).findAllByNameIn(any());
		verify(keywordRepository, never()).findAllByNameIn(any());
	}

	@Test
	void normalizePersonKeepsCaseButTrims() {
		assertThat(service.normalizePerson("  Jean Renoir  ")).isEqualTo("Jean Renoir");
	}

	@Test
	void normalizeGenreLowercasesAndTrims() {
		assertThat(service.normalizeGenre("  Sci-Fi  ")).isEqualTo("sci-fi");
	}

	@Test
	void normalizeRejectsCommas() {
		assertThatThrownBy(() -> service.normalizeGenre("drama, comedy"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("comma");
	}

}
