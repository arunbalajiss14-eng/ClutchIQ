package com.mevsme.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** One logged attempt at a pressure drill the AI Performance Coach assigned, e.g. a late-game scenario. */
@Entity
@Table(name = "pressure_drill_results")
@Getter @Setter @NoArgsConstructor
public class PressureDrillResult {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private PlayerProfile player;

    @Column(nullable = false)
    private LocalDate entryDate;

    private String scenario;
    private int attempts;
    private int successful;

    public PressureDrillResult(PlayerProfile player, LocalDate entryDate, String scenario, int attempts, int successful) {
        this.player = player;
        this.entryDate = entryDate;
        this.scenario = scenario;
        this.attempts = attempts;
        this.successful = successful;
    }
}
