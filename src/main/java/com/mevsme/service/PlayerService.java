package com.mevsme.service;

import com.mevsme.model.*;
import com.mevsme.repo.*;
import com.mevsme.view.PlayerRow;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerService {
    private final AppUserRepository users;
    private final PlayerProfileRepository profiles;
    private final AssessmentRepository assessments;
    private final PerformanceEntryRepository performance;
    private final PasswordEncoder encoder;

    public PlayerAssessment assessmentFor(PlayerProfile p) {
        return assessments.findByPlayer(p).orElseGet(() -> assessments.save(new PlayerAssessment(p)));
    }

    public List<PlayerRow> rows(Team team) {
        return profiles.findByTeamAndUser_ActiveTrue(team).stream()
                .sorted(Comparator.comparing(p -> p.getUser().getFullName().toLowerCase()))
                .map(p -> new PlayerRow(p, assessmentFor(p)))
                .toList();
    }

    @Transactional
    public PlayerProfile createPlayer(Team team, String fullName, String email, String password,
                                      Integer age, Double heightCm, Double weightKg, String phone, String playingRole) {
        if (users.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("An account with " + email + " already exists.");
        }
        AppUser u = new AppUser();
        u.setFullName(fullName.trim());
        u.setEmail(email.trim().toLowerCase());
        u.setPassword(encoder.encode(password));
        u.setRole(Role.PLAYER);
        u = users.save(u);

        PlayerProfile p = new PlayerProfile();
        p.setUser(u);
        p.setTeam(team);
        p.setSport(team.getSport());
        p.setAge(age);
        p.setHeightCm(heightCm);
        p.setWeightKg(weightKg);
        p.setPhone(phone);
        p.setPlayingRole(playingRole);
        p = profiles.save(p);

        assessmentFor(p);
        return p;
    }

    /** Saves the coach's rating and keeps a dated history point for the progress chart. */
    @Transactional
    public void recordScore(PlayerProfile p, int score) {
        PerformanceEntry e = performance.findByPlayerAndEntryDate(p, LocalDate.now())
                .orElseGet(() -> new PerformanceEntry(p, LocalDate.now(), score));
        e.setScore(score);
        performance.save(e);
    }
}
