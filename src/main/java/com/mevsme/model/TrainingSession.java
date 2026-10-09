package com.mevsme.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "training_sessions")
@Getter @Setter @NoArgsConstructor
public class TrainingSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) private Team team;
    private LocalDate trainingDate;
    private LocalTime trainingTime;
    private String location;
    private String focus;
    @Column(length = 1000) private String instructions;
}
