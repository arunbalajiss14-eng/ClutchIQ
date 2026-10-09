package com.mevsme.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "teams")
@Getter @Setter @NoArgsConstructor
public class Team {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String sport;

    @Column(length = 500)
    private String description;

    /** Set when the coach turns the public ranking link on; null means the link is off. */
    @Column(unique = true)
    private String shareToken;

    @ManyToOne(optional = false)
    private AppUser coach;

    private LocalDateTime createdAt = LocalDateTime.now();
}
