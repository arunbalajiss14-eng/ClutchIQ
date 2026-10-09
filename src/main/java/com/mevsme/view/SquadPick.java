package com.mevsme.view;

import com.mevsme.model.PlayerProfile;
import lombok.AllArgsConstructor;
import lombok.Data;

/** One roster player as a candidate for an upcoming match's squad. */
@Data @AllArgsConstructor
public class SquadPick {
    private PlayerProfile player;
    private int performanceScore;
    private boolean selected;
}
