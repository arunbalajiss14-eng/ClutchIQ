package com.mevsme.view;

import com.mevsme.model.MatchFixture;
import lombok.AllArgsConstructor;
import lombok.Data;

/** One upcoming match from a player's own point of view: are they picked, and have they confirmed. */
@Data @AllArgsConstructor
public class PlayerMatchRow {
    private MatchFixture match;
    private boolean selected;
    private boolean confirmed;
}
