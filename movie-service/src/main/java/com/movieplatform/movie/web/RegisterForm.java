package com.movieplatform.movie.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterForm(

		@NotBlank
		@Size(min = 3, max = 64)
		String username,

		@NotBlank
		@Size(min = 8, max = 128)
		String password) {

}
