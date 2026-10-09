package com.mevsme.web;

import com.mevsme.model.*;
import com.mevsme.repo.*;
import com.mevsme.service.ContextService;
import com.mevsme.service.PlayerService;
import com.mevsme.service.ScoreService;
import com.mevsme.service.TeamService;
import com.mevsme.view.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

/** Shared team context. Read access for the coach and every player on the team. */
@Controller
@RequestMapping("/team")
@RequiredArgsConstructor
public class TeamController {
    private final ContextService ctx;
    private final TeamService teamService;
    private final PlayerService playerService;
    private final AnnouncementRepository announcements;
    private final DiscussionPostRepository posts;
    private final MatchRatingRepository ratings;
    private final MatchSelectionRepository selections;
    private final ScoreService scores;

    /** Returns the active team or null when there is none (the caller redirects). */
    private Team team(Principal p, HttpSession s) {
        return ctx.activeTeam(ctx.user(p), s);
    }

    @GetMapping
    public String overview(Principal p, HttpSession s, Model m) {
        Team t = team(p, s);
        if (t == null) return "redirect:/coach/teams";
        m.addAttribute("team", t);
        m.addAttribute("roster", playerService.rows(t));
        m.addAttribute("nextMatch", teamService.upcomingMatches(t, 1).stream().findFirst().orElse(null));
        m.addAttribute("nextTraining", teamService.upcomingTraining(t, 1).stream().findFirst().orElse(null));
        m.addAttribute("nextTournament", teamService.upcomingTournaments(t, 1).stream().findFirst().orElse(null));
        m.addAttribute("lastResult", teamService.lastResult(t));
        m.addAttribute("news", announcements.findByTeamOrderByCreatedAtDesc(t).stream().limit(4).toList());
        return "team/overview";
    }

    @GetMapping("/schedule")
    public String schedule(Principal p, HttpSession s, Model m) {
        Team t = team(p, s);
        if (t == null) return "redirect:/coach/teams";
        m.addAttribute("team", t);
        m.addAttribute("tournaments", teamService.upcomingTournaments(t, 50));
        m.addAttribute("trainings", teamService.upcomingTraining(t, 50));
        return "team/schedule";
    }

    @GetMapping("/results")
    public String results(Principal p, HttpSession s, Model m) {
        Team t = team(p, s);
        if (t == null) return "redirect:/coach/teams";
        m.addAttribute("team", t);
        List<MatchResultView> played = teamService.playedMatches(t).stream()
                .map(mf -> new MatchResultView(mf, ratings.findByMatch(mf)))
                .toList();
        m.addAttribute("results", played);
        m.addAttribute("awaiting", teamService.unplayedMatches(t));
        m.addAttribute("roster", playerService.rows(t));
        return "team/results";
    }

    @GetMapping("/discussion")
    public String discussion(Principal p, HttpSession s, Model m) {
        Team t = team(p, s);
        if (t == null) return "redirect:/coach/teams";
        m.addAttribute("team", t);
        m.addAttribute("posts", posts.findByTeamOrderByCreatedAtDesc(t));
        return "team/discussion";
    }

    @PostMapping("/discussion")
    public String post(Principal p, HttpSession s, RedirectAttributes ra, @RequestParam String body,
                       @RequestParam(defaultValue = "false") boolean askCoach) {
        Team t = team(p, s);
        if (t == null || body.isBlank()) return "redirect:/team/discussion";
        DiscussionPost d = new DiscussionPost();
        d.setTeam(t);
        d.setAuthor(ctx.user(p));
        d.setBody(body.trim());
        d.setAskCoach(askCoach);
        posts.save(d);
        return "redirect:/team/discussion";
    }
}
