package com.mevsme.web;

import com.mevsme.model.AppUser;
import com.mevsme.service.ContextService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

/** Puts the signed-in user, active team and current context into every page for the layout. */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {
    private final ContextService ctx;

    @ModelAttribute
    public void common(Model model, Principal principal, HttpSession session, HttpServletRequest req) {
        if (principal == null) return;
        AppUser me = ctx.user(principal);
        String uri = req.getRequestURI();
        model.addAttribute("me", me);
        model.addAttribute("activeTeam", ctx.activeTeam(me, session));
        model.addAttribute("uri", uri);
        model.addAttribute("inTeam", uri.startsWith("/team"));
    }
}
