package com.mevsme.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** One recorded performance value (runs, points, rating...) together with the situation it happened in. */
@Entity
@Table(name = "clutch_entries")
@Getter @Setter @NoArgsConstructor
public class ClutchEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private PlayerProfile player;

    @Column(nullable = false)
    private LocalDate entryDate;

    private String matchLabel;

    @Column(nullable = false)
    private String situation;

    @Column(name = "metric_value")
    private double value;

    public ClutchEntry(PlayerProfile player, LocalDate entryDate, String matchLabel, String situation, double value) {
        this.player = player;
        this.entryDate = entryDate;
        this.matchLabel = matchLabel;
        this.situation = situation;
        this.value = value;
    }
}
