package com.mevsme.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "daily_logs", uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "log_date"}))
@Getter @Setter @NoArgsConstructor
public class DailyLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private PlayerProfile player;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    private Double sleepHours;
    private Integer energy;

    @Enumerated(EnumType.STRING) private Status foodStatus;
    @Enumerated(EnumType.STRING) private Status exerciseStatus;
    @Enumerated(EnumType.STRING) private Status workoutStatus;
    @Enumerated(EnumType.STRING) private Status trainingStatus;
    @Enumerated(EnumType.STRING) private Status recoveryStatus;
    @Enumerated(EnumType.STRING) private Status hydrationStatus;

    @Column(length = 500)
    private String note;

    public DailyLog(PlayerProfile player, LocalDate logDate) {
        this.player = player;
        this.logDate = logDate;
    }
}
