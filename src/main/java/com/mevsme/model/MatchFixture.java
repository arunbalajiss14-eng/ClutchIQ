package com.mevsme.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "match_fixtures")
@Getter @Setter @NoArgsConstructor
public class MatchFixture {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) private Team team;

    private String opponent;
    private LocalDate matchDate;
    private LocalTime matchTime;
    private String location;
    @Column(length = 1000) private String info;

    /** Minimum age to take part, if the match has one. Null means no requirement. */
    private Integer ageRequirement;
    /** Entry fee as the coach typed it, e.g. "Rs 200" or "Free". Null means not set. */
    private String fee;

    // ---- result, filled in by the coach after the match ----
    private boolean played;
    private Integer ourScore;
    private Integer theirScore;
    private String resultLabel; // WIN, LOSS, DRAW
    @Column(length = 1000) private String wentWell;
    @Column(length = 1000) private String needsImprovement;
    @Column(length = 1000) private String nextTrainingFocus;
}
