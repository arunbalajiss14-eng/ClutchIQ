package com.mevsme.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** The coach's rating of one player's performance in one specific match, shown to that player. */
@Entity
@Table(name = "match_ratings", uniqueConstraints = @UniqueConstraint(columnNames = {"match_id", "player_id"}))
@Getter @Setter @NoArgsConstructor
public class MatchRating {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private MatchFixture match;

    @ManyToOne(optional = false)
    private PlayerProfile player;

    /** 0 to 100. */
    private int rating;

    @Column(length = 500)
    private String note;

    public MatchRating(MatchFixture match, PlayerProfile player, int rating, String note) {
        this.match = match;
        this.player = player;
        this.rating = rating;
        this.note = note;
    }
}
