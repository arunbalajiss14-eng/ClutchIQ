package com.mevsme.view;

import lombok.AllArgsConstructor;
import lombok.Data;

/** A player's overall percentage and the three parts it is built from. routine and lastMatch are null when there is no data yet. */
@Data @AllArgsConstructor
public class OverallScore {
    private int performance;
    private Integer routine;
    private Integer lastMatch;
    private int overall;
}
