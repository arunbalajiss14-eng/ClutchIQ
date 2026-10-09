package com.mevsme.view;

import com.mevsme.model.PlayerProfile;
import lombok.AllArgsConstructor;
import lombok.Data;

/** One roster player ranked by overall percentage, as a squad candidate for one match. */
@Data @AllArgsConstructor
public class SquadRankRow {
    private PlayerProfile player;
    private int overall;
    private boolean selected;
    private boolean confirmed;
}
