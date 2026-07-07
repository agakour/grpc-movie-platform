package com.movieplatform.movie.testutil;

import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;

public final class MovieMvcSecurityTestCustomizer implements MockMvcBuilderCustomizer {

	@Override
	public void customize(ConfigurableMockMvcBuilder<?> builder) {
		builder.apply(SecurityMockMvcConfigurers.springSecurity());
	}

}
