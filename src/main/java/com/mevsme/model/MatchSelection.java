package com.mevsme.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One player picked into the squad for one upcoming match. */
@Entity
@Table(name = "match_selections", uniqueConstraints = @UniqueConstraint(columnNames = {"match_id", "player_id"}))
@Getter @Setter @NoArgsConstructor
public class MatchSelection {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private MatchFixture match;

    @ManyToOne(optional = false)
    private PlayerProfile player;

    private boolean confirmed;
    private java.time.LocalDateTime confirmedAt;

    public MatchSelection(MatchFixture match, PlayerProfile player) {
        this.match = match;
        this.player = player;
    }
}
