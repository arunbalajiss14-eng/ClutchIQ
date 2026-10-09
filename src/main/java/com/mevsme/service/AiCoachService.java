package com.mevsme.service;

import com.mevsme.model.*;
import com.mevsme.repo.DailyLogRepository;
import com.mevsme.repo.MatchFixtureRepository;
import com.mevsme.repo.PressureDrillRepository;
import com.mevsme.view.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

/**
 * AI Performance Coach: turns the coach's weakness note into a daily training plan, tracks
 * duty completion (reusing the player's daily routine check-ins), scores match readiness,
 * and runs pressure drills built from the player's weakest ClutchAI situation.
 *
 * This is a rule-based recommendation engine, not a medical or diagnostic tool.
 */
@Service
@RequiredArgsConstructor
public class AiCoachService {
    private final DailyLogRepository logs;
    private final MatchFixtureRepository matches;
    private final PressureDrillRepository drills;
    private final PlayerService players;
    private final ClutchService clutchService;

    public AiCoachView build(PlayerProfile p) {
        AiCoachView v = new AiCoachView();
        PlayerAssessment a = players.assessmentFor(p);
        LocalDate today = LocalDate.now();

        // ---- focus + priorities, built from the coach's weakness note ----
        String weakness = a.getWeakness() == null ? "" : a.getWeakness().toLowerCase();
        Map.Entry<String, List<Priority>> plan = planFor(weakness, a);
        v.setFocusArea(plan.getKey());
        v.setFocusWhy(a.getWeakness() == null || a.getWeakness().isBlank()
                ? "Your coach has not flagged a weakness yet, so this is a balanced general plan."
                : "Your coach flagged \"" + a.getWeakness() + "\" as an area to work on.");

        DailyLog todayLog = logs.findByPlayerAndLogDate(p, today).orElse(null);
        for (Priority pr : plan.getValue()) {
            pr.setDone(todayLog != null && isDone(todayLog, pr.getDutyKey()));
        }
        v.setPriorities(plan.getValue());

        // ---- last 7 days of duty completion, for readiness and the trend chart ----
        LocalDate weekStart = today.minusDays(6);
        Map<LocalDate, DailyLog> byDate = new HashMap<>();
        for (DailyLog l : logs.findByPlayerAndLogDateBetweenOrderByLogDateAsc(p, weekStart, today)) byDate.put(l.getLogDate(), l);

        double trainingSum = 0, recoverySum = 0, sleepSum = 0, energySum = 0;
        int trainingN = 0, recoveryN = 0, sleepN = 0, energyN = 0;
        for (LocalDate d = weekStart; !d.isAfter(today); d = d.plusDays(1)) {
            DailyLog l = byDate.get(d);
            String dayLabel = d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            if (l == null) { v.getWeek().add(new DayCell(dayLabel, -1)); continue; }
            int total = 0, statusPts = 0;
            Status[] all = {l.getFoodStatus(), l.getHydrationStatus(), l.getExerciseStatus(), l.getWorkoutStatus(), l.getTrainingStatus(), l.getRecoveryStatus()};
            for (Status s : all) { total++; if (s != null) { statusPts += s == Status.DONE ? 2 : s == Status.PARTIAL ? 1 : 0; } }
            v.getWeek().add(new DayCell(dayLabel, total == 0 ? -1 : (int) Math.round(statusPts / (total * 2.0) * 100)));
            if (l.getTrainingStatus() != null) { trainingSum += pts(l.getTrainingStatus()); trainingN++; }
            if (l.getRecoveryStatus() != null) { recoverySum += pts(l.getRecoveryStatus()); recoveryN++; }
            if (l.getSleepHours() != null) { sleepSum += Math.min(1, l.getSleepHours() / (p.getTargetSleepHours() == null ? 8 : p.getTargetSleepHours())); sleepN++; }
            if (l.getEnergy() != null) { energySum += l.getEnergy() / 10.0; energyN++; }
        }

        // Only average the signals that actually have data this week. A missing signal is left out
        // rather than defaulted to a guess, so a brand-new player does not get a misleadingly specific number.
        double sum = 0; int signals = 0;
        if (trainingN > 0) { int pct = (int) Math.round(trainingSum / trainingN * 100); v.getReadinessBreakdown().add(new Metric("Training", pct)); sum += pct; signals++; }
        if (recoveryN > 0) { int pct = (int) Math.round(recoverySum / recoveryN * 100); v.getReadinessBreakdown().add(new Metric("Recovery", pct)); sum += pct; signals++; }
        if (sleepN > 0) { int pct = (int) Math.round(Math.min(1, sleepSum / sleepN) * 100); v.getReadinessBreakdown().add(new Metric("Sleep", pct)); sum += pct; signals++; }
        if (energyN > 0) { int pct = (int) Math.round(energySum / energyN * 100); v.getReadinessBreakdown().add(new Metric("Energy", pct)); sum += pct; signals++; }
        int formPct = a.getPerformanceScore();
        v.getReadinessBreakdown().add(new Metric("Recent form", formPct));
        sum += formPct; signals++;

        int overall = (int) Math.round(sum / signals);
        v.setReadinessOverall(overall);
        v.setReadinessSignalCount(signals);
        v.setReadinessHasFullData(signals >= 5);

        if (signals <= 1) {
            v.setReadinessNote("Based on your coach's rating only. Log a few days of training and recovery for a fuller reading.");
        } else {
            String weakestArea = Collections.min(v.getReadinessBreakdown(), Comparator.comparingInt(Metric::getValue)).getLabel();
            String base = overall >= 80
                    ? "You are well prepared. Keep the same routine going into the match."
                    : "Your training consistency looks " + (trainingN > 0 && trainingSum / trainingN >= 0.7 ? "strong" : "uneven") + ", but " + weakestArea.toLowerCase() + " needs attention before the next match.";
            v.setReadinessNote(signals < 5 ? base + " (Based on " + signals + " of 5 signals; log more days for a fuller picture.)" : base);
        }

        // ---- upcoming match ----
        if (p.getTeam() != null) {
            matches.findByTeamOrderByMatchDateAsc(p.getTeam()).stream()
                    .filter(m -> !m.isPlayed() && m.getMatchDate() != null && !m.getMatchDate().isBefore(today))
                    .findFirst()
                    .ifPresent(m -> {
                        v.setDaysToMatch((int) java.time.temporal.ChronoUnit.DAYS.between(today, m.getMatchDate()));
                        v.setMatchOpponent(m.getOpponent());
                    });
        }

        // ---- yesterday's review ----
        DailyLog y = logs.findByPlayerAndLogDate(p, today.minusDays(1)).orElse(null);
        if (y != null) {
            v.setHasYesterday(true);
            v.getYesterdayLines().add(new ReviewLine("Training", stateOf(y.getTrainingStatus())));
            v.getYesterdayLines().add(new ReviewLine("Workout", stateOf(y.getWorkoutStatus())));
            v.getYesterdayLines().add(new ReviewLine("Recovery", stateOf(y.getRecoveryStatus())));
            v.getYesterdayLines().add(new ReviewLine("Nutrition", stateOf(y.getFoodStatus())));
            v.getYesterdayLines().add(new ReviewLine("Sleep", y.getSleepHours() == null ? "PENDING" : "DONE"));
            int done = 0;
            for (ReviewLine rl : v.getYesterdayLines()) if (rl.getState().equals("DONE")) done++;
            v.setYesterdayCompletion((int) Math.round(done * 100.0 / v.getYesterdayLines().size()));
            String missed = v.getYesterdayLines().stream().filter(rl -> rl.getState().equals("MISSED"))
                    .map(ReviewLine::getLabel).findFirst().orElse(null);
            v.setReviewFeedback(v.getYesterdayCompletion() >= 80
                    ? "You completed most of yesterday's plan. Keep the same routine today."
                    : missed != null
                        ? "You completed some of yesterday's plan, but " + missed.toLowerCase() + " was missed, so it is prioritised again today."
                        : "Yesterday's duties were only partly logged. Fill in today's plan to keep the picture accurate.");
        }

        // ---- ClutchAI + pressure drill ----
        ClutchView cv = clutchService.build(p);
        v.setClutchInsights(cv.getInsights());
        v.setDrill(buildDrill(p, cv));

        return v;
    }

    private DrillView buildDrill(PlayerProfile p, ClutchView cv) {
        DrillView d = new DrillView();
        String scenario = (cv.isEnough() && cv.getWorst() != null && cv.getWorst().getDeltaPct() < 0)
                ? cv.getWorst().getSituation() : "Final period";
        d.setScenario(scenario);
        d.setReason(cv.isEnough() && cv.getWorst() != null && cv.getWorst().getDeltaPct() < 0
                ? "ClutchAI found this is where your numbers drop most against your own average. Deliberate reps here are the fastest way to close that gap."
                : "ClutchAI needs at least 4 match entries with their situation before it can pinpoint your toughest moment. Until then, practise this general scenario.");
        List<PressureDrillResult> history = drills.findByPlayerOrderByEntryDateDesc(p);
        d.setHasHistory(!history.isEmpty());
        int lastAttempts = 10, lastSuccess = 6;
        for (PressureDrillResult r : history) if (r.getScenario().equals(scenario)) { lastAttempts = r.getAttempts(); lastSuccess = r.getSuccessful(); break; }
        d.setAttempts(lastAttempts);
        d.setNextTarget(Math.min(lastAttempts, lastSuccess + 1));
        for (int i = 0; i < Math.min(6, history.size()); i++) {
            PressureDrillResult r = history.get(i);
            d.getHistory().add(new DrillPoint(r.getSuccessful(), r.getAttempts()));
        }
        Collections.reverse(d.getHistory());
        return d;
    }

    /** Weakness keyword to icon, title, detail, why, and which DailyLog field marks it done. */
    private Map.Entry<String, List<Priority>> planFor(String weakness, PlayerAssessment a) {
        List<Priority> list = new ArrayList<>();
        String focus;
        if (weakness.contains("stamina") || weakness.contains("endurance") || weakness.contains("fitness")) {
            focus = "Stamina";
            list.add(new Priority("\uD83D\uDD25", "Endurance training", "20 minute steady run or continuous drill work", "Builds the aerobic base your coach flagged as low.", "training", false));
            list.add(new Priority("\uD83D\uDCAA", "Core and leg workout", "15 minutes, bodyweight is fine", "Stronger core and legs support longer efforts.", "workout", false));
            list.add(new Priority("\uD83C\uDFAF", "Skill drill under fatigue", "30 reps in the last five minutes of training", "Keeps technique sharp even when tired.", "training", false));
        } else if (weakness.contains("speed") || weakness.contains("sprint") || weakness.contains("pace")) {
            focus = "Speed";
            list.add(new Priority("\uD83D\uDD25", "Sprint intervals", "6 x 30 metres, full recovery between reps", "Short, fully rested sprints build top speed without fatigue masking form.", "training", false));
            list.add(new Priority("\uD83D\uDCAA", "Hip and ankle mobility", "10 minutes before training", "Mobility work protects speed gains from injury.", "workout", false));
        } else if (weakness.contains("field") || weakness.contains("agility") || weakness.contains("reaction")) {
            focus = "Agility and fielding";
            list.add(new Priority("\uD83C\uDFAF", "Reaction drills", "15 minutes of ladder or cone work", "Sharpens the first-step reaction your coach noted.", "training", false));
            list.add(new Priority("\uD83D\uDD25", "Fielding repetitions", "30 catches or stops", "Repetition under game-like pace builds reliability.", "training", false));
        } else if (weakness.contains("batting") || weakness.contains("consistency") || weakness.contains("technique")) {
            focus = "Batting consistency";
            list.add(new Priority("\uD83C\uDFAF", "Technical net session", "30 minutes against varied pace", "Targets the consistency gap your coach flagged.", "training", false));
            list.add(new Priority("\uD83D\uDCAA", "Balance and footwork drill", "15 minutes", "Good footwork is the base of consistent technique.", "workout", false));
        } else if (weakness.contains("pressure") || weakness.contains("nerve") || weakness.contains("focus") || weakness.contains("concentration")) {
            focus = "Performing under pressure";
            list.add(new Priority("\uD83C\uDFAF", "Simulated pressure reps", "10 attempts with a scoreboard or countdown", "Practising under manufactured pressure builds composure.", "training", false));
            list.add(new Priority("\uD83D\uDE34", "Wind-down routine", "10 minutes of breathing or a short walk before bed", "Better sleep quality improves decision-making under pressure.", "recovery", false));
        } else {
            focus = "All-round development";
            list.add(new Priority("\uD83D\uDD25", "Balanced training session", "As set by your coach for today", "No single weakness is flagged, so keep working all-round.", "training", false));
            list.add(new Priority("\uD83D\uDCAA", "General conditioning", "15 minutes", "Keeps fitness on track between focus blocks.", "workout", false));
        }
        list.add(new Priority("\uD83E\uDD57", "Nutrition routine", "Three balanced meals, hydrate through the day", "Fuels the training load above.", "food", false));
        list.add(new Priority("\uD83D\uDE34", "Sleep target", (a.getPlayer() != null && a.getPlayer().getTargetSleepHours() != null ? a.getPlayer().getTargetSleepHours() : 8) + " hours tonight", "Recovery is when the training above actually pays off.", "sleep", false));
        return Map.entry(focus, list);
    }

    private static boolean isDone(DailyLog l, String key) {
        Status s = switch (key) {
            case "training" -> l.getTrainingStatus();
            case "workout" -> l.getWorkoutStatus();
            case "food" -> l.getFoodStatus();
            case "recovery" -> l.getRecoveryStatus();
            case "sleep" -> null;
            default -> null;
        };
        if ("sleep".equals(key)) return l.getSleepHours() != null;
        return s == Status.DONE;
    }

    private static String stateOf(Status s) {
        return s == null ? "PENDING" : s.name();
    }

    private static double pts(Status s) {
        return s == Status.DONE ? 1 : s == Status.PARTIAL ? 0.5 : 0;
    }
}
