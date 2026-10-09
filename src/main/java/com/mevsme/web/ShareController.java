package com.mevsme.web;

import com.mevsme.model.Team;
import com.mevsme.repo.TeamRepository;
import com.mevsme.service.ScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

/** Public, read-only ranking page. Works only while the coach keeps the share link switched on. */
@Controller
@RequiredArgsConstructor
public class ShareController {
    private final TeamRepository teams;
    private final ScoreService scores;

    @GetMapping("/share/{token}")
    public String view(@PathVariable String token, Model m) {
        Team t = teams.findByShareToken(token).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        m.addAttribute("team", t);
        m.addAttribute("ranking", scores.ranking(t));
        return "share";
    }
}
