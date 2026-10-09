package com.mevsme.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "performance_entries")
@Getter @Setter @NoArgsConstructor
public class PerformanceEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private PlayerProfile player;

    @Column(nullable = false)
    private LocalDate entryDate;

    private int score;

    public PerformanceEntry(PlayerProfile player, LocalDate entryDate, int score) {
        this.player = player;
        this.entryDate = entryDate;
        this.score = score;
    }
}
