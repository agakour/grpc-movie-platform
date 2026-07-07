package com.movieplatform.movie.security.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

	java.util.Optional<AppUser> findByUsername(String username);

	boolean existsByUsername(String username);

}
