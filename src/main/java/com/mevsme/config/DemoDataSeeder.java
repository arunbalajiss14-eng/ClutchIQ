package com.mevsme.config;

import com.mevsme.model.*;
import com.mevsme.repo.*;
import com.mevsme.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Creates a demo coach, team, players, matches and a week of routine data on the first run.
 * Turn off with app.seed-demo=false.
 *
 * Logins (password for all: demo123):
 *   coach@demo.com   (coach)
 *   rahul@demo.com   (player, has a visible ClutchIQ gap)
 *   arjun@demo.com   (player, on track)
 */
@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {
    private final AppUserRepository users;
    private final CoachProfileRepository coachProfiles;
    private final TeamRepository teams;
    private final PlayerProfileRepository profiles;
    private final AssessmentRepository assessments;
    private final PerformanceEntryRepository performance;
    private final ClutchEntryRepository clutch;
    private final PressureDrillRepository drills;
    private final DailyLogRepository logs;
    private final AnnouncementRepository announcements;
    private final TournamentRepository tournaments;
    private final MatchFixtureRepository matches;
    private final TrainingSessionRepository trainings;
    private final MatchRatingRepository matchRatings;
    private final MatchSelectionRepository selections;
    private final DiscussionPostRepository posts;
    private final PlayerService playerService;
    private final PasswordEncoder encoder;

    @Value("${app.seed-demo:false}")
    private boolean seed;

    @Override
    public void run(String... args) {
        if (!seed || users.count() > 0) return;
        LocalDate today = LocalDate.now();

        AppUser coach = new AppUser();
        coach.setFullName("Suresh Kumar");
        coach.setEmail("coach@demo.com");
        coach.setPassword(encoder.encode("demo123"));
        coach.setRole(Role.COACH);
        coach = users.save(coach);
        CoachProfile cp = new CoachProfile(coach);
        cp.setSport("Cricket"); cp.setCity("Chennai"); cp.setPhone("+91 90000 11111");
        cp.setQualification("NCA Level 2 Coaching Certificate");
        cp.setCertifications("First aid for sport, Strength and conditioning basics");
        cp.setExperienceYears(11);
        cp.setBio("Former state-level all-rounder. Builds teams around fitness, fielding and match awareness.");
        coachProfiles.save(cp);

        Team team = new Team();
        team.setName("Chennai Strikers");
        team.setSport("Cricket");
        team.setDescription("College-level cricket squad training four days a week.");
        team.setCoach(coach);
        team = teams.save(team);

        PlayerProfile rahul = player(team, "Rahul Verma", "rahul@demo.com", 19, 176.0, 68.0, "Opening batter", 10);
        PlayerProfile arjun = player(team, "Arjun Nair", "arjun@demo.com", 20, 181.0, 74.0, "Fast bowler", 10);
        PlayerProfile karthik = player(team, "Karthik Iyer", "karthik@demo.com", 18, 172.0, 63.0, "Wicketkeeper", 10);
        PlayerProfile vikram = player(team, "Vikram Rao", "vikram@demo.com", 21, 179.0, 71.0, "Spin all-rounder", 10);

        assess(rahul, "Fast running between the wickets", "Low stamina", "Short-distance sprint",
                "Long-duration performance", "Good", "Improving over the last three months",
                "Improve stamina and endurance", "Two aerobic sessions a week plus interval work", 68, 55);
        assess(arjun, "Pace and consistent line", "Fielding in the deep", "New-ball spells",
                "Long spells in heat", "Strong", "Steady", "Deep fielding and throwing accuracy", "Throwing drills twice a week", 78, 72);
        assess(karthik, "Sharp reflexes behind the stumps", "Batting consistency", "Standing up to spin",
                "Chasing under pressure", "Good", "Improving", "Batting against short-pitched bowling", "Machine sessions", 64, 66);
        assess(vikram, "Flight and variation", "Running between wickets", "Middle overs",
                "Powerplay bowling", "Average", "Up and down", "Fitness and calling for runs", "Interval running", 58, 60);

        int[] rahulScores = {58, 60, 61, 63, 64, 66, 68};
        for (int i = 0; i < rahulScores.length; i++) {
            performance.save(new PerformanceEntry(rahul, today.minusDays((rahulScores.length - 1 - i) * 6L), rahulScores[i]));
        }
        performance.save(new PerformanceEntry(arjun, today.minusDays(12), 74));
        performance.save(new PerformanceEntry(arjun, today, 78));

        // ClutchAI data for Rahul: normal about 18, better in close games and the final overs
        clutch(rahul, today, "vs Royals", "Normal play", 17);
        clutch(rahul, today, "vs Titans", "Normal play", 19);
        clutch(rahul, today, "vs Lions", "Normal play", 16);
        clutch(rahul, today, "vs Hawks", "Close game", 27);
        clutch(rahul, today, "vs Wolves", "Close game", 26);
        clutch(rahul, today, "vs Kings", "Final period", 31);
        clutch(rahul, today, "vs Sharks", "Final period", 29);
        clutch(rahul, today, "vs Eagles", "Away", 12);
        clutch(rahul, today, "vs Bulls", "Away", 14);

        // Rahul's weakest situation is "Away", so this is the pressure drill his AI Performance Coach assigns.
        drills.save(new PressureDrillResult(rahul, today.minusDays(9), "Away", 10, 5));
        drills.save(new PressureDrillResult(rahul, today.minusDays(5), "Away", 10, 6));
        drills.save(new PressureDrillResult(rahul, today.minusDays(2), "Away", 10, 7));

        // Rahul: a week with real misses, so the AI Performance Coach has something to react to
        rahul.setCreatedAt(today.minusDays(12).atStartOfDay());
        profiles.save(rahul);
        //            sleep energy food  hyd   exer  work  train recov
        log(rahul, today.minusDays(6), 7.5, 7, "DONE", "DONE", "DONE", "DONE", "DONE", "DONE");
        log(rahul, today.minusDays(5), 6.0, 6, "DONE", "PARTIAL", "DONE", "MISSED", "DONE", "MISSED");
        log(rahul, today.minusDays(4), 5.5, 5, "PARTIAL", "MISSED", "MISSED", "MISSED", "DONE", "MISSED");
        log(rahul, today.minusDays(3), 7.0, 6, "DONE", "DONE", "DONE", "PARTIAL", "DONE", "PARTIAL");
        log(rahul, today.minusDays(2), 6.0, 5, "DONE", "PARTIAL", "MISSED", "MISSED", "MISSED", "MISSED");
        log(rahul, today.minusDays(1), 7.0, 6, "DONE", "DONE", "DONE", "MISSED", "DONE", "PARTIAL");

        // Arjun: consistent
        arjun.setCreatedAt(today.minusDays(12).atStartOfDay());
        profiles.save(arjun);
        for (int d = 6; d >= 1; d--) {
            log(arjun, today.minusDays(d), 8.0, 8, "DONE", "DONE", "DONE", d % 3 == 0 ? "PARTIAL" : "DONE", "DONE", "DONE");
        }

        // Team content
        Announcement a = new Announcement();
        a.setTeam(team); a.setTitle("Fitness test on Friday");
        a.setBody("Bring running shoes. We record 2 km times for everyone, so your baseline is set before the tournament.");
        announcements.save(a);

        TrainingSession ts = new TrainingSession();
        ts.setTeam(team); ts.setTrainingDate(today.plusDays(3)); ts.setTrainingTime(LocalTime.of(17, 0));
        ts.setLocation("College Ground"); ts.setFocus("Stamina, fielding, batting");
        ts.setInstructions("Arrive 15 minutes early for warm-up. Bring your own water bottle.");
        trainings.save(ts);
        TrainingSession ts2 = new TrainingSession();
        ts2.setTeam(team); ts2.setTrainingDate(today.plusDays(6)); ts2.setTrainingTime(LocalTime.of(6, 30));
        ts2.setLocation("Nets, Block B"); ts2.setFocus("Pressure batting in the final overs");
        trainings.save(ts2);

        Tournament tn = new Tournament();
        tn.setTeam(team); tn.setName("Inter-College T20 Cup"); tn.setStartDate(today.plusDays(21));
        tn.setLocation("Chepauk Practice Grounds"); tn.setInfo("Group stage first. Squad list is due one week before the start.");
        tournaments.save(tn);

        MatchFixture next = new MatchFixture();
        next.setTeam(team); next.setOpponent("Royal Engineers"); next.setMatchDate(today.plusDays(8));
        next.setMatchTime(LocalTime.of(10, 0)); next.setLocation("Marina Ground");
        next.setInfo("Reporting time 8:30 AM. Whites are not required.");
        matches.save(next);
        selections.save(new MatchSelection(next, rahul));
        selections.save(new MatchSelection(next, arjun));

        MatchFixture prev = new MatchFixture();
        prev.setTeam(team); prev.setOpponent("City Lions"); prev.setMatchDate(today.minusDays(5));
        prev.setMatchTime(LocalTime.of(14, 0)); prev.setLocation("Nehru Stadium");
        prev.setPlayed(true); prev.setOurScore(145); prev.setTheirScore(138); prev.setResultLabel("WIN");
        prev.setWentWell("Batting in the powerplay and the last five overs.");
        prev.setNeedsImprovement("Fielding and bowling consistency in the middle overs.");
        prev.setNextTrainingFocus("Fielding drills and yorker practice.");
        matches.save(prev);
        matchRatings.save(new MatchRating(prev, rahul, 72, "Strong start, faded late in the innings"));
        matchRatings.save(new MatchRating(prev, arjun, 81, "Excellent new-ball spell"));
        matchRatings.save(new MatchRating(prev, karthik, 63, "Tidy keeping, loose shots"));
        matchRatings.save(new MatchRating(prev, vikram, 58, "Middle overs too expensive"));


        post(team, rahul.getUser(), "Coach, should I rest before Friday's fitness test, or keep the normal workouts?", true);
        post(team, coach, "Keep the light sessions and skip heavy leg work two days before. Sleep matters more than another workout.", false);
        post(team, arjun.getUser(), "Anyone free for extra fielding practice on Sunday morning?", false);

        System.out.println("Demo data loaded. Coach login: coach@demo.com / demo123, player login: rahul@demo.com / demo123");
    }

    private PlayerProfile player(Team team, String name, String email, int age, double h, double w, String role, int unused) {
        return playerService.createPlayer(team, name, email, "demo123", age, h, w, "+91 98400 0000" + age, role);
    }

    private void assess(PlayerProfile p, String strength, String weakness, String best, String worst,
                        String recent, String historical, String improve, String training, int score, int stamina) {
        PlayerAssessment a = playerService.assessmentFor(p);
        a.setStrength(strength); a.setWeakness(weakness); a.setBestIn(best); a.setWorstIn(worst);
        a.setRecentPerformance(recent); a.setHistoricalPerformance(historical);
        a.setImprovementAreas(improve); a.setTrainingRequirements(training);
        a.setCoachNotes("Reviewed after the last match.");
        a.setPerformanceScore(score); a.setStaminaScore(stamina);
        assessments.save(a);
    }

    private void clutch(PlayerProfile p, LocalDate base, String label, String situation, double value) {
        long n = clutch.count();
        clutch.save(new ClutchEntry(p, base.minusDays(3 + n * 4), label, situation, value));
    }

    private void log(PlayerProfile p, LocalDate d, double sleep, int energy,
                     String food, String hydration, String exercise, String workout, String training, String recovery) {
        DailyLog l = new DailyLog(p, d);
        l.setSleepHours(sleep); l.setEnergy(energy);
        l.setFoodStatus(Status.valueOf(food)); l.setHydrationStatus(Status.valueOf(hydration));
        l.setExerciseStatus(Status.valueOf(exercise)); l.setWorkoutStatus(Status.valueOf(workout));
        l.setTrainingStatus(Status.valueOf(training)); l.setRecoveryStatus(Status.valueOf(recovery));
        logs.save(l);
    }

    private void post(Team t, AppUser author, String body, boolean ask) {
        DiscussionPost d = new DiscussionPost();
        d.setTeam(t); d.setAuthor(author); d.setBody(body); d.setAskCoach(ask);
        posts.save(d);
    }
}
