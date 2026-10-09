package com.mevsme.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "coach_profiles")
@Getter @Setter @NoArgsConstructor
public class CoachProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    private AppUser user;

    private String phone;
    private String city;
    private String sport;
    private String qualification;
    private String certifications;
    private Integer experienceYears;

    @Column(length = 1500)
    private String bio;

    public CoachProfile(AppUser user) { this.user = user; }
}
