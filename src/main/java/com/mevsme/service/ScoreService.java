package com.mevsme.service;

import com.mevsme.model.*;
import com.mevsme.repo.DailyLogRepository;
import com.mevsme.repo.MatchRatingRepository;
import com.mevsme.repo.PlayerProfileRepository;
import com.mevsme.view.OverallScore;
import com.mevsme.view.RankRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Overall percentage used for squad selection and the shareable ranking.
 * Weights: coach performance rating 40, daily routine over the last 7 days 30, most recent match rating 30.
 * A part with no data yet is left out and the remaining weights are rescaled, so a new player is not punished.
 */
@Service
@RequiredArgsConstructor
public class ScoreService {
    private final DailyLogRepository logs;
    private final MatchRatingRepository ratings;
    private final PlayerProfileRepository profiles;
    private final PlayerService players;

    public OverallScore overallFor(PlayerProfile p) {
        int performance = players.assessmentFor(p).getPerformanceScore();

        LocalDate today = LocalDate.now();
        List<DailyLog> week = logs.findByPlayerAndLogDateBetweenOrderByLogDateAsc(p, today.minusDays(6), today);
        Integer routine = week.isEmpty() ? null
                : (int) Math.round(week.stream().mapToInt(ScoreService::dayCompletion).average().orElse(0));

        Integer lastMatch = ratings.findByPlayer(p).stream()
                .filter(r -> r.getMatch().getMatchDate() != null)
                .max(Comparator.comparing((MatchRating r) -> r.getMatch().getMatchDate()))
                .map(MatchRating::getRating).orElse(null);

        double sum = performance * 40.0, weight = 40.0;
        if (routine != null) { sum += routine * 30.0; weight += 30.0; }
        if (lastMatch != null) { sum += lastMatch * 30.0; weight += 30.0; }
        return new OverallScore(performance, routine, lastMatch, (int) Math.round(sum / weight));
    }

    /** Every player on the team, best overall percentage first. */
    public List<RankRow> ranking(Team team) {
        return profiles.findByTeamAndUser_ActiveTrue(team).stream()
                .map(p -> new RankRow(p, players.assessmentFor(p), overallFor(p)))
                .sorted(Comparator.comparingInt((RankRow r) -> r.getScore().getOverall()).reversed())
                .toList();
    }

    /** 0 to 100 for one day's six routine items. Done counts 1, partly 0.5, missed or not logged 0. */
    public static int dayCompletion(DailyLog l) {
        Status[] all = {l.getFoodStatus(), l.getHydrationStatus(), l.getExerciseStatus(),
                l.getWorkoutStatus(), l.getTrainingStatus(), l.getRecoveryStatus()};
        double pts = 0;
        for (Status s : all) pts += s == Status.DONE ? 1 : s == Status.PARTIAL ? 0.5 : 0;
        return (int) Math.round(pts / all.length * 100);
    }
}
