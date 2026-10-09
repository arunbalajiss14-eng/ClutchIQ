package com.mevsme.web;

import com.mevsme.model.*;
import com.mevsme.repo.*;
import com.mevsme.service.*;
import com.mevsme.view.PlayerRow;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** Coach personal context: profile, teams, players, and coach-controlled performance data. */
@Controller
@RequestMapping("/coach")
@RequiredArgsConstructor
public class CoachController {
    private final ContextService ctx;
    private final CoachProfileRepository coachProfiles;
    private final AppUserRepository users;
    private final TeamRepository teams;
    private final PlayerProfileRepository profiles;
    private final AssessmentRepository assessments;
    private final PerformanceEntryRepository performance;
    private final ClutchEntryRepository clutchEntries;
    private final PlayerService playerService;
    private final TeamService teamService;
    private final ClutchService clutch;
    private final AiCoachService aiCoachService;
    private final ScoreService scores;
    private final MatchFixtureRepository matchFixtures;
    private final MatchSelectionRepository matchSelections;
    private final MatchRatingRepository matchRatings;
    private final AnnouncementRepository announcements;
    private final TournamentRepository tournaments;
    private final TrainingSessionRepository trainings;
    private final DiscussionPostRepository discussionPosts;

    @GetMapping
    public String dashboard(Principal p, HttpSession session, Model m) {
        AppUser me = ctx.user(p);
        Team team = ctx.activeTeam(me, session);
        m.addAttribute("teams", ctx.teamsOf(me));
        if (team != null) {
            List<PlayerRow> rows = playerService.rows(team);
            m.addAttribute("rows", rows);
            m.addAttribute("teamAvg", (int) Math.round(rows.stream()
                    .mapToInt(r -> r.getAssessment().getPerformanceScore()).average().orElse(0)));
            m.addAttribute("needs", rows.stream()
                    .sorted(Comparator.comparingInt(r -> r.getAssessment().getPerformanceScore()))
                    .limit(4).toList());
            m.addAttribute("matches", teamService.upcomingMatches(team, 3));
            m.addAttribute("tournaments", teamService.upcomingTournaments(team, 3));
            m.addAttribute("trainings", teamService.upcomingTraining(team, 3));
            m.addAttribute("lastResult", teamService.lastResult(team));
            m.addAttribute("insights", clutch.teamInsights(rows));
        }
        return "coach/dashboard";
    }

    // ------------------------------------------------------------- profile
    @GetMapping("/profile")
    public String profile(Principal p, Model m) {
        AppUser me = ctx.user(p);
        m.addAttribute("cp", coachProfiles.findByUser(me).orElseGet(() -> new CoachProfile(me)));
        return "coach/profile";
    }

    @PostMapping("/profile")
    public String saveProfile(Principal p, @RequestParam String fullName,
                              @RequestParam(required = false) String phone, @RequestParam(required = false) String city,
                              @RequestParam(required = false) String sport, @RequestParam(required = false) String qualification,
                              @RequestParam(required = false) String certifications,
                              @RequestParam(required = false) Integer experienceYears,
                              @RequestParam(required = false) String bio, RedirectAttributes ra) {
        AppUser me = ctx.user(p);
        me.setFullName(fullName.trim());
        users.save(me);
        CoachProfile cp = coachProfiles.findByUser(me).orElseGet(() -> new CoachProfile(me));
        cp.setPhone(phone); cp.setCity(city);
        if ((cp.getSport() == null || cp.getSport().isBlank()) && sport != null && !sport.isBlank()) cp.setSport(sport.trim());
        cp.setQualification(qualification); cp.setCertifications(certifications);
        cp.setExperienceYears(experienceYears); cp.setBio(bio);
        coachProfiles.save(cp);
        ra.addFlashAttribute("flash", "Profile saved.");
        return "redirect:/coach/profile";
    }

    // ------------------------------------------------------------- teams
    @GetMapping("/teams")
    public String teamsPage(Principal p, Model m) {
        AppUser me = ctx.user(p);
        m.addAttribute("teams", ctx.teamsOf(me));
        m.addAttribute("coachSport", coachSport(me));
        return "coach/teams";
    }

    @PostMapping("/teams")
    public String createTeam(Principal p, HttpSession session, @RequestParam String name,
                             @RequestParam(required = false) String description, RedirectAttributes ra) {
        AppUser me = ctx.user(p);
        String sport = coachSport(me);
        if (sport == null) {
            ra.addFlashAttribute("error", "Set your sport in My profile first. It applies to every team you create.");
            return "redirect:/coach/profile";
        }
        Team t = new Team();
        t.setName(name.trim());
        t.setSport(sport);
        t.setDescription(description);
        t.setCoach(me);
        t = teams.save(t);
        session.setAttribute(ContextService.ACTIVE_TEAM, t.getId());
        ra.addFlashAttribute("flash", "Team " + t.getName() + " created. Add your first players next.");
        return "redirect:/coach/players";
    }

    /** Every team a coach runs shares the one sport set on their coach profile. */
    private String coachSport(AppUser coach) {
        String fromProfile = coachProfiles.findByUser(coach).map(CoachProfile::getSport)
                .filter(s -> s != null && !s.isBlank()).orElse(null);
        if (fromProfile != null) return fromProfile;
        // older accounts: fall back to the sport of the first team they created
        return teams.findByCoachOrderByCreatedAtAsc(coach).stream().map(Team::getSport).findFirst().orElse(null);
    }

    @PostMapping("/teams/{id}/rename")
    public String renameTeam(Principal p, @PathVariable Long id, @RequestParam String name, RedirectAttributes ra) {
        AppUser me = ctx.user(p);
        Team t = teams.findById(id).filter(x -> x.getCoach().getId().equals(me.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        t.setName(name.trim());
        teams.save(t);
        ra.addFlashAttribute("flash", "Team renamed.");
        return "redirect:/coach/teams";
    }

    /** Deleting a team is blocked while it still has active players, so nobody's data disappears by accident. */
    @PostMapping("/teams/{id}/delete")
    public String deleteTeam(Principal p, HttpSession session, @PathVariable Long id, RedirectAttributes ra) {
        AppUser me = ctx.user(p);
        Team t = teams.findById(id).filter(x -> x.getCoach().getId().equals(me.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!profiles.findByTeamAndUser_ActiveTrue(t).isEmpty()) {
            ra.addFlashAttribute("error", "Move or remove every player from " + t.getName() + " before deleting it.");
            return "redirect:/coach/teams";
        }
        for (MatchFixture mf : matchFixtures.findByTeamOrderByMatchDateAsc(t)) {
            matchSelections.deleteByMatch(mf);
            matchRatings.deleteByMatch(mf);
        }
        matchFixtures.deleteAll(matchFixtures.findByTeamOrderByMatchDateAsc(t));
        announcements.deleteByTeam(t);
        tournaments.deleteByTeam(t);
        trainings.deleteByTeam(t);
        discussionPosts.deleteByTeam(t);
        // players removed earlier are still linked to this team by id; detach them so the delete doesn't fail
        for (PlayerProfile pp : profiles.findByTeamAndUser_ActiveFalse(t)) { pp.setTeam(null); profiles.save(pp); }
        teams.delete(t);
        if (session.getAttribute(ContextService.ACTIVE_TEAM) != null && session.getAttribute(ContextService.ACTIVE_TEAM).equals(id)) {
            session.removeAttribute(ContextService.ACTIVE_TEAM);
        }
        ra.addFlashAttribute("flash", t.getName() + " deleted.");
        return "redirect:/coach/teams";
    }

    @PostMapping("/teams/{id}/activate")
    public String activate(Principal p, HttpSession session, @PathVariable Long id, RedirectAttributes ra) {
        AppUser me = ctx.user(p);
        Team t = teams.findById(id).filter(x -> x.getCoach().getId().equals(me.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        session.setAttribute(ContextService.ACTIVE_TEAM, t.getId());
        ra.addFlashAttribute("flash", t.getName() + " is now your active team.");
        return "redirect:/coach/teams";
    }

    // ------------------------------------------------------------- players
    @GetMapping("/players")
    public String players(Principal p, HttpSession session, Model m) {
        Team team = ctx.activeTeam(ctx.user(p), session);
        if (team == null) return "redirect:/coach/teams";
        m.addAttribute("rows", scores.ranking(team));
        m.addAttribute("shareUrl", team.getShareToken() == null ? null
                : org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/share/" + team.getShareToken()).build().toUriString());
        m.addAttribute("deletedCount", profiles.findByTeamAndUser_ActiveFalse(team).size());
        return "coach/players";
    }

    /** Removed players, kept here (not deleted) so their routine and performance history is never lost. */
    @GetMapping("/players/deleted")
    public String deletedPlayers(Principal p, HttpSession session, Model m) {
        Team team = ctx.activeTeam(ctx.user(p), session);
        if (team == null) return "redirect:/coach/teams";
        m.addAttribute("team", team);
        m.addAttribute("rows", profiles.findByTeamAndUser_ActiveFalse(team));
        return "coach/deleted-players";
    }

    /** Disables the player's login. Their routine, performance and match history are kept, not deleted. */
    @PostMapping("/players/{id}/remove")
    public String removePlayer(Principal p, @PathVariable Long id, RedirectAttributes ra) {
        PlayerProfile pp = owned(p, id);
        AppUser u = pp.getUser();
        u.setActive(false);
        u.setRemovedAt(java.time.LocalDateTime.now());
        users.save(u);
        ra.addFlashAttribute("flash", pp.getUser().getFullName() + " was removed and can no longer sign in. Their history is kept under Deleted players.");
        return "redirect:/coach/players";
    }

    @PostMapping("/players/{id}/restore")
    public String restorePlayer(Principal p, @PathVariable Long id, RedirectAttributes ra) {
        PlayerProfile pp = owned(p, id);
        AppUser u = pp.getUser();
        u.setActive(true);
        u.setRemovedAt(null);
        users.save(u);
        ra.addFlashAttribute("flash", pp.getUser().getFullName() + " can sign in again.");
        return "redirect:/coach/players";
    }

    @PostMapping("/players")
    public String addPlayer(Principal p, HttpSession session, RedirectAttributes ra,
                            @RequestParam String fullName, @RequestParam String email, @RequestParam String password,
                            @RequestParam(required = false) Integer age, @RequestParam(required = false) Double heightCm,
                            @RequestParam(required = false) Double weightKg, @RequestParam(required = false) String phone,
                            @RequestParam(required = false) String playingRole) {
        Team team = ctx.activeTeam(ctx.user(p), session);
        if (team == null) return "redirect:/coach/teams";
        if (password.length() < 6) {
            ra.addFlashAttribute("error", "Use a temporary password with at least 6 characters.");
            return "redirect:/coach/players";
        }
        try {
            playerService.createPlayer(team, fullName, email, password, age, heightCm, weightKg, phone, playingRole);
            ra.addFlashAttribute("flash", fullName + " added to " + team.getName() + ". Share the email and temporary password with them.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/coach/players";
    }

    @PostMapping("/share/enable")
    public String enableShare(Principal p, HttpSession session, RedirectAttributes ra) {
        Team t = ctx.activeTeam(ctx.user(p), session);
        if (t == null) return "redirect:/coach/teams";
        t.setShareToken(java.util.UUID.randomUUID().toString().replace("-", ""));
        teams.save(t);
        ra.addFlashAttribute("flash", "Share link is on. Anyone who has the link can see player names and percentages.");
        return "redirect:/coach/players";
    }

    @PostMapping("/share/disable")
    public String disableShare(Principal p, HttpSession session, RedirectAttributes ra) {
        Team t = ctx.activeTeam(ctx.user(p), session);
        if (t == null) return "redirect:/coach/teams";
        t.setShareToken(null);
        teams.save(t);
        ra.addFlashAttribute("flash", "Share link turned off. The old link no longer works.");
        return "redirect:/coach/players";
    }

    @GetMapping("/players/{id}")
    public String playerDetail(Principal p, @PathVariable Long id, Model m) {
        PlayerProfile pp = owned(p, id);
        List<PerformanceEntry> history = performance.findByPlayerOrderByEntryDateAsc(pp);
        if (history.size() > 10) history = history.subList(history.size() - 10, history.size());
        m.addAttribute("pp", pp);
        m.addAttribute("a", playerService.assessmentFor(pp));
        m.addAttribute("history", history);
        m.addAttribute("clutch", clutch.build(pp));
        m.addAttribute("clutchEntries", clutchEntries.findByPlayerOrderByEntryDateDesc(pp).stream().limit(8).toList());
        m.addAttribute("situations", ClutchService.SITUATIONS);
        m.addAttribute("coach", aiCoachService.build(pp));
        m.addAttribute("today", LocalDate.now());
        m.addAttribute("otherTeams", ctx.teamsOf(ctx.user(p)).stream()
                .filter(t -> pp.getTeam() == null || !t.getId().equals(pp.getTeam().getId())).toList());
        return "coach/player-detail";
    }

    /** Moves a player to another team run by the same coach. Both teams share the coach's one sport. */
    @PostMapping("/players/{id}/move-team")
    public String moveTeam(Principal p, @PathVariable Long id, @RequestParam Long teamId, RedirectAttributes ra) {
        AppUser me = ctx.user(p);
        PlayerProfile pp = owned(p, id);
        Team target = teams.findById(teamId).filter(t -> t.getCoach().getId().equals(me.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        pp.setTeam(target);
        profiles.save(pp);
        ra.addFlashAttribute("flash", pp.getUser().getFullName() + " moved to " + target.getName() + ".");
        return "redirect:/coach/players/" + id;
    }

    /** Only the coach can write these fields. There is no player endpoint that touches them. */
    @PostMapping("/players/{id}/assessment")
    public String saveAssessment(Principal p, @PathVariable Long id, RedirectAttributes ra,
                                 @RequestParam(required = false) String strength, @RequestParam(required = false) String weakness,
                                 @RequestParam(required = false) String bestIn, @RequestParam(required = false) String worstIn,
                                 @RequestParam(required = false) String recentPerformance,
                                 @RequestParam(required = false) String historicalPerformance,
                                 @RequestParam(required = false) String improvementAreas,
                                 @RequestParam(required = false) String trainingRequirements,
                                 @RequestParam(required = false) String coachNotes,
                                 @RequestParam int performanceScore, @RequestParam int staminaScore) {
        PlayerProfile pp = owned(p, id);
        PlayerAssessment a = playerService.assessmentFor(pp);
        a.setStrength(blank(strength)); a.setWeakness(blank(weakness)); a.setBestIn(blank(bestIn)); a.setWorstIn(blank(worstIn));
        a.setRecentPerformance(blank(recentPerformance)); a.setHistoricalPerformance(blank(historicalPerformance));
        a.setImprovementAreas(blank(improvementAreas)); a.setTrainingRequirements(blank(trainingRequirements));
        a.setCoachNotes(blank(coachNotes));
        a.setPerformanceScore(clamp(performanceScore));
        a.setStaminaScore(clamp(staminaScore));
        a.setUpdatedAt(java.time.LocalDateTime.now());
        assessments.save(a);
        playerService.recordScore(pp, a.getPerformanceScore());
        ra.addFlashAttribute("flash", "Assessment saved for " + pp.getUser().getFullName() + ".");
        return "redirect:/coach/players/" + id;
    }

    @PostMapping("/players/{id}/clutch")
    public String addClutch(Principal p, @PathVariable Long id, RedirectAttributes ra,
                            @RequestParam(required = false) String matchLabel, @RequestParam String situation,
                            @RequestParam double value,
                            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate entryDate) {
        PlayerProfile pp = owned(p, id);
        clutchEntries.save(new ClutchEntry(pp, entryDate, matchLabel, situation, value));
        ra.addFlashAttribute("flash", "Match entry added to ClutchAI.");
        return "redirect:/coach/players/" + id;
    }

    private PlayerProfile owned(Principal p, Long id) {
        AppUser me = ctx.user(p);
        return profiles.findById(id)
                .filter(pp -> pp.getTeam() != null && pp.getTeam().getCoach().getId().equals(me.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private static String blank(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    private static int clamp(int v) { return Math.max(0, Math.min(100, v)); }
}
