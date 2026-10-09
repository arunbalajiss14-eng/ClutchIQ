package com.mevsme.web;

import com.mevsme.model.AppUser;
import com.mevsme.model.CoachProfile;
import com.mevsme.model.Role;
import com.mevsme.repo.AppUserRepository;
import com.mevsme.repo.CoachProfileRepository;
import com.mevsme.service.ContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class AuthController {
    private final AppUserRepository users;
    private final CoachProfileRepository coachProfiles;
    private final PasswordEncoder encoder;
    private final ContextService ctx;

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/register")
    public String registerForm() { return "register"; }

    /** Public sign-up creates coach accounts. Coaches create their players' accounts. */
    @PostMapping("/register")
    public String register(@RequestParam String fullName, @RequestParam String email,
                           @RequestParam String password, @RequestParam String sport,
                           Model model) {
        if (password.length() < 6) {
            model.addAttribute("error", "Use a password with at least 6 characters.");
            return "register";
        }
        if (sport == null || sport.isBlank()) {
            model.addAttribute("error", "Choose the sport you coach. To coach a different sport, create a separate account.");
            return "register";
        }
        if (users.existsByEmailIgnoreCase(email.trim())) {
            model.addAttribute("error", "An account with this email already exists. Sign in instead.");
            return "register";
        }
        AppUser u = new AppUser();
        u.setFullName(fullName.trim());
        u.setEmail(email.trim().toLowerCase());
        u.setPassword(encoder.encode(password));
        u.setRole(Role.COACH);
        u = users.save(u);
        CoachProfile cp = new CoachProfile(u);
        cp.setSport(sport.trim());
        coachProfiles.save(cp);
        return "redirect:/login?registered";
    }

    @GetMapping("/")
    public String root() { return "redirect:/home"; }

    @GetMapping("/home")
    public String home(Principal principal) {
        return ctx.user(principal).isCoach() ? "redirect:/coach" : "redirect:/player";
    }
}
