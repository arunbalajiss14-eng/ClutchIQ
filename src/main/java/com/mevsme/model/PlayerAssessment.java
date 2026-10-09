package com.mevsme.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Coach-controlled performance data. Players can read it but never edit it. */
@Entity
@Table(name = "player_assessments")
@Getter @Setter @NoArgsConstructor
public class PlayerAssessment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    private PlayerProfile player;

    @Column(length = 500) private String strength;
    @Column(length = 500) private String weakness;
    @Column(length = 500) private String bestIn;
    @Column(length = 500) private String worstIn;
    @Column(length = 1000) private String recentPerformance;
    @Column(length = 1000) private String historicalPerformance;
    @Column(length = 1000) private String improvementAreas;
    @Column(length = 1000) private String trainingRequirements;
    @Column(length = 1500) private String coachNotes;

    /** 0-100 overall performance rating set by the coach. */
    private int performanceScore = 50;
    /** 0-100 stamina rating set by the coach. */
    private int staminaScore = 50;

    private LocalDateTime updatedAt = LocalDateTime.now();

    public PlayerAssessment(PlayerProfile player) { this.player = player; }
}
