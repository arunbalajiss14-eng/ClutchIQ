package com.mevsme.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Player-editable biodata. Coach-controlled performance lives in {@link PlayerAssessment}. */
@Entity
@Table(name = "player_profiles")
@Getter @Setter @NoArgsConstructor
public class PlayerProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    private AppUser user;

    @ManyToOne
    private Team team;

    private Integer age;
    private Double heightCm;
    private Double weightKg;
    private String phone;
    private String sport;
    private String playingRole;

    /** Nightly sleep target in hours, used by the sleep check and the avatar. */
    private Double targetSleepHours = 8.0;

    @Column(length = 500)
    private String personalGoal;

    private LocalDateTime createdAt = LocalDateTime.now();
}
