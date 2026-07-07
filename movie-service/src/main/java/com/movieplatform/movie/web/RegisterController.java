package com.movieplatform.movie.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.movieplatform.movie.security.registration.RegistrationService;
import com.movieplatform.movie.security.registration.UsernameTakenException;

import jakarta.validation.Valid;

@Controller
@SuppressWarnings("unused")
public class RegisterController {

	private final RegistrationService registrationService;

	public RegisterController(RegistrationService registrationService) {
		this.registrationService = registrationService;
	}

	@GetMapping("/register")
	public String registerForm(Model model) {
		if (!model.containsAttribute("registerForm")) {
			model.addAttribute("registerForm", new RegisterForm(null, null));
		}
		return "register";
	}

	@PostMapping("/register")
	public String register(@Valid @ModelAttribute("registerForm") RegisterForm form, BindingResult binding,
			RedirectAttributes redirectAttributes) {
		if (binding.hasErrors()) {
			return "register";
		}
		try {
			registrationService.register(form.username(), form.password());
		}
		catch (UsernameTakenException exception) {
			binding.rejectValue("username", "register.username.taken", "Username is already in use.");
			return "register";
		}
		catch (IllegalArgumentException exception) {
			binding.reject("register.invalid", exception.getMessage());
			return "register";
		}
		redirectAttributes.addFlashAttribute("registered", true);
		return "redirect:/login";
	}

}
