package com.mevsme.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "discussion_posts")
@Getter @Setter @NoArgsConstructor
public class DiscussionPost {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) private Team team;
    @ManyToOne(optional = false) private AppUser author;
    @Column(length = 1000) private String body;
    /** True when the player is asking the coach directly. */
    private boolean askCoach;
    private LocalDateTime createdAt = LocalDateTime.now();

    /** True once a coach has deleted this post. The row is kept, but the body is hidden. */
    private boolean removed;
}
