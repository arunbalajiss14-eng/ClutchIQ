package com.mevsme.web;

import com.mevsme.model.*;
import com.mevsme.repo.*;
import com.mevsme.service.*;
import com.mevsme.view.AiCoachView;
import com.mevsme.view.CatRow;
import com.mevsme.view.PlayerMatchRow;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Player personal context. Players can edit their own biodata and routine, and only read coach-controlled data. */
@Controller
@RequestMapping("/player")
@RequiredArgsConstructor
public class PlayerController {
    private final ContextService ctx;
    private final AppUserRepository users;
    private final PlayerProfileRepository profiles;
    private final DailyLogRepository logs;
    private final PerformanceEntryRepository performance;
    private final PressureDrillRepository drillResults;
    private final AnnouncementRepository announcements;
    private final PlayerService playerService;
    private final TeamService teamService;
    private final AiCoachService aiCoachService;
    private final HistoryService historyService;
    private final ScoreService scoreService;
    private final MatchRatingRepository matchRatings;
    private final MatchSelectionRepository selections;
    private final MatchFixtureRepository matchFixtures;

    private PlayerProfile me(Principal p) {
        return ctx.playerProfile(ctx.user(p)).orElseThrow(() -> new IllegalStateException("No player profile"));
    }

    @GetMapping
    public String dashboard(Principal p, HttpSession s, Model m) {
        PlayerProfile pp = me(p);
        AiCoachView coach = aiCoachService.build(pp);
        m.addAttribute("pp", pp);
        m.addAttribute("a", playerService.assessmentFor(pp));
        m.addAttribute("coach", coach);
        m.addAttribute("overall", scoreService.overallFor(pp));
        if (pp.getTeam() != null) {
            Team t = pp.getTeam();
            m.addAttribute("nextTraining", teamService.upcomingTraining(t, 1).stream().findFirst().orElse(null));
            MatchFixture next = teamService.upcomingMatches(t, 1).stream().findFirst().orElse(null);
            m.addAttribute("nextMatch", next);
            if (next != null) {
                m.addAttribute("selectedNext", selections.findByMatch(next).stream()
                        .anyMatch(sel -> sel.getPlayer().getId().equals(pp.getId())));
            }
            m.addAttribute("news", announcements.findByTeamOrderByCreatedAtDesc(t).stream().limit(3).toList());
        }
        return "player/dashboard";
    }

    // ------------------------------------------------------------ profile
    @GetMapping("/profile")
    public String profile(Principal p, Model m) {
        m.addAttribute("pp", me(p));
        return "player/profile";
    }

    /** Only biodata fields are bound here. Coach-controlled assessment fields are never accepted. */
    @PostMapping("/profile")
    public String saveProfile(Principal p, RedirectAttributes ra, @RequestParam String fullName,
                              @RequestParam(required = false) Integer age, @RequestParam(required = false) Double heightCm,
                              @RequestParam(required = false) Double weightKg, @RequestParam(required = false) String phone,
                              @RequestParam(required = false) String playingRole,
                              @RequestParam(required = false) Double targetSleepHours,
                              @RequestParam(required = false) String personalGoal) {
        AppUser u = ctx.user(p);
        u.setFullName(fullName.trim());
        users.save(u);
        PlayerProfile pp = me(p);
        pp.setAge(age); pp.setHeightCm(heightCm); pp.setWeightKg(weightKg); pp.setPhone(phone);
        pp.setPlayingRole(playingRole); pp.setPersonalGoal(personalGoal);
        if (targetSleepHours != null && targetSleepHours >= 4 && targetSleepHours <= 12) pp.setTargetSleepHours(targetSleepHours);
        profiles.save(pp);
        ra.addFlashAttribute("flash", "Profile saved.");
        return "redirect:/player/profile";
    }

    // ------------------------------------------------------------ performance (read only)
    @GetMapping("/performance")
    public String performance(Principal p, Model m) {
        PlayerProfile pp = me(p);
        List<PerformanceEntry> history = performance.findByPlayerOrderByEntryDateAsc(pp);
        if (history.size() > 10) history = history.subList(history.size() - 10, history.size());
        m.addAttribute("pp", pp);
        m.addAttribute("a", playerService.assessmentFor(pp));
        m.addAttribute("history", history);
        m.addAttribute("overall", scoreService.overallFor(pp));
        m.addAttribute("matchRatings", matchRatings.findByPlayer(pp).stream()
                .filter(r -> r.getMatch().getMatchDate() != null)
                .sorted(java.util.Comparator.comparing((MatchRating r) -> r.getMatch().getMatchDate()).reversed())
                .toList());
        return "player/performance";
    }

    // ------------------------------------------------------------ daily routine
    @GetMapping("/routine")
    public String routine(Principal p, Model m,
                          @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        PlayerProfile pp = me(p);
        LocalDate today = LocalDate.now();
        LocalDate day = (date == null || date.isAfter(today) || date.isBefore(today.minusDays(2))) ? today : date;
        DailyLog log = logs.findByPlayerAndLogDate(pp, day).orElse(new DailyLog(pp, day));

        List<CatRow> cats = new ArrayList<>();
        cats.add(new CatRow("food", "Food", "Planned meals eaten", v(log.getFoodStatus())));
        cats.add(new CatRow("hydration", "Hydration", "Water through the day", v(log.getHydrationStatus())));
        cats.add(new CatRow("exercise", "Exercise", "Daily movement or conditioning", v(log.getExerciseStatus())));
        cats.add(new CatRow("workout", "Workout", "Gym or strength session", v(log.getWorkoutStatus())));
        cats.add(new CatRow("training", "Team training", "Sport-specific practice", v(log.getTrainingStatus())));
        cats.add(new CatRow("recovery", "Recovery", "Stretching, mobility, rest", v(log.getRecoveryStatus())));

        List<LocalDate> days = List.of(today, today.minusDays(1), today.minusDays(2));
        m.addAttribute("pp", pp);
        m.addAttribute("log", log);
        m.addAttribute("day", day);
        m.addAttribute("days", days);
        m.addAttribute("cats", cats);
        return "player/routine";
    }

    @PostMapping("/routine")
    public String saveRoutine(Principal p, RedirectAttributes ra,
                              @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                              @RequestParam(required = false) Double sleepHours,
                              @RequestParam(required = false) Integer energy,
                              @RequestParam(required = false) String food, @RequestParam(required = false) String hydration,
                              @RequestParam(required = false) String exercise, @RequestParam(required = false) String workout,
                              @RequestParam(required = false) String training, @RequestParam(required = false) String recovery,
                              @RequestParam(required = false) String note) {
        PlayerProfile pp = me(p);
        LocalDate today = LocalDate.now();
        if (date.isAfter(today) || date.isBefore(today.minusDays(2))) date = today;
        DailyLog log = logs.findByPlayerAndLogDate(pp, date).orElse(new DailyLog(pp, date));
        log.setSleepHours(sleepHours == null ? null : Math.max(0, Math.min(16, sleepHours)));
        log.setEnergy(energy == null ? null : Math.max(1, Math.min(10, energy)));
        log.setFoodStatus(status(food)); log.setHydrationStatus(status(hydration));
        log.setExerciseStatus(status(exercise)); log.setWorkoutStatus(status(workout));
        log.setTrainingStatus(status(training)); log.setRecoveryStatus(status(recovery));
        log.setNote(note);
        logs.save(log);
        ra.addFlashAttribute("flash", "Duties saved. Your AI Performance Coach plan has been updated.");
        return "redirect:/player/coach";
    }

    // ------------------------------------------------------------ AI Performance Coach
    @GetMapping("/coach")
    public String aiCoach(Principal p, Model m) {
        PlayerProfile pp = me(p);
        m.addAttribute("pp", pp);
        m.addAttribute("coach", aiCoachService.build(pp));
        return "player/coach";
    }

    /** Player logs the result of a pressure drill the AI Performance Coach assigned. */
    @PostMapping("/coach/drill")
    public String logDrill(Principal p, RedirectAttributes ra, @RequestParam String scenario,
                           @RequestParam int attempts, @RequestParam int successful) {
        PlayerProfile pp = me(p);
        attempts = Math.max(1, Math.min(50, attempts));
        successful = Math.max(0, Math.min(attempts, successful));
        drillResults.save(new PressureDrillResult(pp, LocalDate.now(), scenario, attempts, successful));
        ra.addFlashAttribute("flash", "Drill result logged: " + successful + " of " + attempts + ".");
        return "redirect:/player/coach";
    }

    // ------------------------------------------------------------ routine history (last 28 days)
    @GetMapping("/history")
    public String history(Principal p, Model m) {
        PlayerProfile pp = me(p);
        m.addAttribute("pp", pp);
        m.addAttribute("h", historyService.build(pp));
        return "player/history";
    }

    // ------------------------------------------------------------ upcoming matches
    @GetMapping("/matches")
    public String matches(Principal p, Model m) {
        PlayerProfile pp = me(p);
        List<PlayerMatchRow> rows = new ArrayList<>();
        if (pp.getTeam() != null) {
            for (MatchFixture mf : teamService.upcomingMatches(pp.getTeam(), 50)) {
                var sel = selections.findByMatchAndPlayer(mf, pp);
                rows.add(new PlayerMatchRow(mf, sel.isPresent(), sel.map(MatchSelection::isConfirmed).orElse(false)));
            }
        }
        m.addAttribute("pp", pp);
        m.addAttribute("rows", rows);
        return "player/matches";
    }

    /** A player confirms they will play, once their coach has selected them. */
    @PostMapping("/matches/{id}/confirm")
    public String confirmMatch(Principal p, @PathVariable Long id, RedirectAttributes ra) {
        PlayerProfile pp = me(p);
        MatchFixture mf = matchFixtures.findById(id)
                .filter(m2 -> pp.getTeam() != null && m2.getTeam().getId().equals(pp.getTeam().getId()))
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        MatchSelection sel = selections.findByMatchAndPlayer(mf, pp)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "You have not been selected for this match"));
        sel.setConfirmed(true);
        sel.setConfirmedAt(java.time.LocalDateTime.now());
        selections.save(sel);
        ra.addFlashAttribute("flash", "You are confirmed for vs " + mf.getOpponent() + ".");
        return "redirect:/player/matches";
    }

    private static String v(Status s) { return s == null ? "" : s.name(); }

    private static Status status(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Status.valueOf(s); } catch (IllegalArgumentException e) { return null; }
    }
}

