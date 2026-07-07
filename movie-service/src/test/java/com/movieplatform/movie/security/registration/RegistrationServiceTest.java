package com.movieplatform.movie.security.registration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.movieplatform.movie.security.user.AppUser;
import com.movieplatform.movie.security.user.AppUserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unused")
class RegistrationServiceTest {

	@Mock
	private AppUserRepository appUserRepository;

	private RegistrationService service;

	@BeforeEach
	void setUp() {
		service = new RegistrationService(appUserRepository, new BCryptPasswordEncoder());
	}

	@Test
	void registerTrimsUsernameAndStoresBcryptHashWithUserRole() {
		when(appUserRepository.existsByUsername("alice")).thenReturn(false);
		when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
			AppUser user = invocation.getArgument(0);
			user.setId(9L);
			return user;
		});

		Long id = service.register("  alice  ", "change-me-password");

		assertThat(id).isEqualTo(9L);
		ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
		verify(appUserRepository).save(captor.capture());
		AppUser saved = captor.getValue();
		assertThat(saved.getUsername()).isEqualTo("alice");
		assertThat(saved.getRole()).isEqualTo("USER");
		assertThat(saved.getPasswordHash()).startsWith("$2a$");
		assertThat(saved.getPasswordHash()).isNotEqualTo("change-me-password");
		assertThat(new BCryptPasswordEncoder().matches("change-me-password", saved.getPasswordHash())).isTrue();
	}

	@Test
	void duplicateUsernameIsRejected() {
		when(appUserRepository.existsByUsername("admin")).thenReturn(true);

		assertThatThrownBy(() -> service.register("admin", "change-me-password"))
				.isInstanceOf(UsernameTakenException.class);
	}

	@Test
	void tooShortUsernameIsRejected() {
		assertThatThrownBy(() -> service.register("ab", "change-me-password"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void tooShortPasswordIsRejected() {
		assertThatThrownBy(() -> service.register("somebody", "short"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void nullPasswordIsRejected() {
		assertThatThrownBy(() -> service.register("somebody", null))
				.isInstanceOf(IllegalArgumentException.class);
	}

}
