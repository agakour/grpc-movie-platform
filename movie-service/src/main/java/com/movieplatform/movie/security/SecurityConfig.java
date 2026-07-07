package com.movieplatform.movie.security;

import com.movieplatform.movie.security.user.AppUser;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@SuppressWarnings("unused")
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) {
		http
			.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/", "/register", "/login", "/css/**", "/img/**", "/error", "/favicon.ico", "/favicon.svg", "/actuator/health/**")
					.permitAll()
				.requestMatchers(HttpMethod.GET, "/movies/**")
					.permitAll()
				.requestMatchers(HttpMethod.POST, "/movies/*/vote")
					.authenticated()
				.requestMatchers("/recommendations")
					.authenticated()
				.requestMatchers(HttpMethod.POST, "/api/**")
					.hasRole(AppUser.ROLE_ADMIN)
				.requestMatchers(HttpMethod.PUT, "/api/**")
					.hasRole(AppUser.ROLE_ADMIN)
				.requestMatchers(HttpMethod.DELETE, "/api/**")
					.hasRole(AppUser.ROLE_ADMIN)
			.requestMatchers("/api/**")
				.authenticated()
			.requestMatchers("/v3/api-docs/**", "/scalar/**")
				.authenticated()
			.requestMatchers("/admin/**")
					.hasRole(AppUser.ROLE_ADMIN)
				.anyRequest()
					.denyAll())
			.exceptionHandling(handling -> handling
				.authenticationEntryPoint(new ApiFirstAuthenticationEntryPoint()))
			.httpBasic(Customizer.withDefaults())
			.formLogin(form -> form
				.loginPage("/login")
				.defaultSuccessUrl("/", false))
			.logout(logout -> logout
				.logoutSuccessUrl("/"));

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
