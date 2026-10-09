package com.mevsme.service;

import com.mevsme.model.*;
import com.mevsme.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamService {
    private final MatchFixtureRepository matches;
    private final TournamentRepository tournaments;
    private final TrainingSessionRepository trainings;

    public List<MatchFixture> upcomingMatches(Team team, int limit) {
        LocalDate today = LocalDate.now();
        return matches.findByTeamOrderByMatchDateAsc(team).stream()
                .filter(m -> !m.isPlayed() && m.getMatchDate() != null && !m.getMatchDate().isBefore(today))
                .limit(limit).toList();
    }

    /** Every match without a result yet, including past ones the coach still has to record. */
    public List<MatchFixture> unplayedMatches(Team team) {
        return matches.findByTeamOrderByMatchDateAsc(team).stream()
                .filter(m -> !m.isPlayed())
                .toList();
    }

    public List<MatchFixture> playedMatches(Team team) {
        return matches.findByTeamOrderByMatchDateAsc(team).stream()
                .filter(MatchFixture::isPlayed)
                .sorted(Comparator.comparing(MatchFixture::getMatchDate).reversed())
                .toList();
    }

    public MatchFixture lastResult(Team team) {
        List<MatchFixture> played = playedMatches(team);
        return played.isEmpty() ? null : played.get(0);
    }

    public List<Tournament> upcomingTournaments(Team team, int limit) {
        LocalDate today = LocalDate.now();
        return tournaments.findByTeamOrderByStartDateAsc(team).stream()
                .filter(t -> t.getStartDate() != null && !t.getStartDate().isBefore(today))
                .limit(limit).toList();
    }

    public List<TrainingSession> upcomingTraining(Team team, int limit) {
        LocalDate today = LocalDate.now();
        return trainings.findByTeamOrderByTrainingDateAsc(team).stream()
                .filter(t -> t.getTrainingDate() != null && !t.getTrainingDate().isBefore(today))
                .limit(limit).toList();
    }
}
