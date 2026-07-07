package com.movieplatform.movie.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@SuppressWarnings("unused")
public class LoginController {

	@GetMapping("/login")
	public String login() {
		return "login";
	}

}
