package com.mevsme.web;

import com.mevsme.model.*;
import com.mevsme.repo.*;
import com.mevsme.service.ContextService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;

/** Coach-only actions that publish content into the shared Team context. Locked to COACH by SecurityConfig. */
@Controller
@RequestMapping("/coach/team")
@RequiredArgsConstructor
public class CoachTeamController {
    private static final String ISO_DATE = "yyyy-MM-dd";

    private final ContextService ctx;
    private final AnnouncementRepository announcements;
    private final TournamentRepository tournaments;
    private final MatchFixtureRepository matches;
    private final TrainingSessionRepository trainings;
    private final PlayerProfileRepository profiles;
    private final MatchRatingRepository ratings;
    private final DiscussionPostRepository posts;
    private final ClutchEntryRepository clutchEntries;

    /** Hides a message for everyone. The row is kept so the thread stays readable, but the text is gone. */
    @PostMapping("/discussion/{id}/delete")
    public String deletePost(Principal p, HttpSession s, RedirectAttributes ra, @PathVariable Long id) {
        Team t = team(p, s);
        DiscussionPost post = posts.findById(id).filter(x -> x.getTeam().getId().equals(t.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        post.setRemoved(true);
        post.setBody(null);
        posts.save(post);
        ra.addFlashAttribute("flash", "Message deleted.");
        return "redirect:/team/discussion";
    }

    private Team team(Principal p, HttpSession s) {
        Team t = ctx.activeTeam(ctx.user(p), s);
        if (t == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Create a team first");
        return t;
    }

    @PostMapping("/announcements")
    public String announce(Principal p, HttpSession s, RedirectAttributes ra,
                           @RequestParam String title, @RequestParam(required = false) String body) {
        Announcement a = new Announcement();
        a.setTeam(team(p, s)); a.setTitle(title); a.setBody(body);
        announcements.save(a);
        ra.addFlashAttribute("flash", "Announcement posted.");
        return "redirect:/team";
    }

    @PostMapping("/tournaments")
    public String tournament(Principal p, HttpSession s, RedirectAttributes ra,
                             @RequestParam String name,
                             @RequestParam @DateTimeFormat(pattern = ISO_DATE) LocalDate startDate,
                             @RequestParam(required = false) String location,
                             @RequestParam(required = false) String info) {
        Tournament t = new Tournament();
        t.setTeam(team(p, s)); t.setName(name); t.setStartDate(startDate); t.setLocation(location); t.setInfo(info);
        tournaments.save(t);
        ra.addFlashAttribute("flash", "Tournament published.");
        return "redirect:/team/schedule";
    }

    @PostMapping("/matches/{id}/result")
    public String result(Principal p, HttpSession s, RedirectAttributes ra, @PathVariable Long id,
                         @RequestParam int ourScore, @RequestParam int theirScore,
                         @RequestParam(required = false) String wentWell,
                         @RequestParam(required = false) String needsImprovement,
                         @RequestParam(required = false) String nextTrainingFocus) {
        Team t = team(p, s);
        MatchFixture m = matches.findById(id).filter(x -> x.getTeam().getId().equals(t.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        m.setPlayed(true);
        m.setOurScore(ourScore); m.setTheirScore(theirScore);
        m.setResultLabel(ourScore > theirScore ? "WIN" : ourScore < theirScore ? "LOSS" : "DRAW");
        m.setWentWell(wentWell); m.setNeedsImprovement(needsImprovement); m.setNextTrainingFocus(nextTrainingFocus);
        matches.save(m);
        ra.addFlashAttribute("flash", "Result saved: " + m.getResultLabel() + " against " + m.getOpponent() + ".");
        return "redirect:/team/results";
    }

    @PostMapping("/tournaments/{id}/delete")
    public String deleteTournament(Principal p, HttpSession s, RedirectAttributes ra, @PathVariable Long id) {
        Team t = team(p, s);
        Tournament tn = tournaments.findById(id).filter(x -> x.getTeam().getId().equals(t.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        tournaments.delete(tn);
        ra.addFlashAttribute("flash", tn.getName() + " deleted.");
        return "redirect:/team/schedule";
    }

    @PostMapping("/training")
    public String training(Principal p, HttpSession s, RedirectAttributes ra,
                           @RequestParam @DateTimeFormat(pattern = ISO_DATE) LocalDate trainingDate,
                           @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime trainingTime,
                           @RequestParam(required = false) String location,
                           @RequestParam(required = false) String focus,
                           @RequestParam(required = false) String instructions) {
        TrainingSession t = new TrainingSession();
        t.setTeam(team(p, s)); t.setTrainingDate(trainingDate); t.setTrainingTime(trainingTime);
        t.setLocation(location); t.setFocus(focus); t.setInstructions(instructions);
        trainings.save(t);
        ra.addFlashAttribute("flash", "Training session published.");
        return "redirect:/team/schedule";
    }

    @PostMapping("/training/{id}/delete")
    public String deleteTraining(Principal p, HttpSession s, RedirectAttributes ra, @PathVariable Long id) {
        Team t = team(p, s);
        TrainingSession ts = trainings.findById(id).filter(x -> x.getTeam().getId().equals(t.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        trainings.delete(ts);
        ra.addFlashAttribute("flash", "Training session deleted.");
        return "redirect:/team/schedule";
    }

    /** The coach rates one player's performance in one specific match. Shown on that player's own page. */
    @PostMapping("/matches/{id}/ratings")
    public String rate(Principal p, HttpSession s, RedirectAttributes ra, @PathVariable Long id,
                       @RequestParam Long playerId, @RequestParam int rating, @RequestParam(required = false) String note) {
        Team t = team(p, s);
        MatchFixture m = matches.findById(id).filter(x -> x.getTeam().getId().equals(t.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        PlayerProfile pp = profiles.findById(playerId)
                .filter(x -> x.getTeam() != null && x.getTeam().getId().equals(t.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        MatchRating mr = ratings.findByMatchAndPlayer(m, pp).orElseGet(() -> new MatchRating(m, pp, 0, null));
        mr.setRating(Math.max(0, Math.min(100, rating)));
        mr.setNote(note);
        ratings.save(mr);
        // Feeds ClutchAI without a separate manual-entry form: every match rating becomes one data point.
        clutchEntries.save(new ClutchEntry(pp, m.getMatchDate() != null ? m.getMatchDate() : LocalDate.now(),
                "vs " + m.getOpponent(), "Normal play", mr.getRating()));
        ra.addFlashAttribute("flash", "Rating saved for " + pp.getUser().getFullName() + ".");
        return "redirect:/team/results";
    }

}
