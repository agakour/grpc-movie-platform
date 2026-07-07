package com.movieplatform.movie.security.registration;

import com.movieplatform.movie.security.user.AppUser;
import com.movieplatform.movie.security.user.AppUserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings("unused")
public class RegistrationService {

	private final AppUserRepository appUserRepository;

	private final PasswordEncoder passwordEncoder;

	public RegistrationService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
		this.appUserRepository = appUserRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public Long register(String rawUsername, String rawPassword) {
		String username = rawUsername == null ? "" : rawUsername.trim();
		if (username.length() < 3 || username.length() > 64) {
			throw new IllegalArgumentException("Username must be 3-64 characters.");
		}
		if (rawPassword == null || rawPassword.length() < 8) {
			throw new IllegalArgumentException("Password must be at least 8 characters.");
		}
		if (appUserRepository.existsByUsername(username)) {
			throw new UsernameTakenException(username);
		}
		AppUser user = new AppUser();
		user.setUsername(username);
		user.setPasswordHash(passwordEncoder.encode(rawPassword));
		user.setRole(AppUser.ROLE_USER);
		return appUserRepository.save(user).getId();
	}

}
