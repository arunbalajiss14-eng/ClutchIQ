package com.mevsme.view;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class SituationStat {
    private String situation;
    private double average;
    private int count;
    /** Percentage difference against the player's overall average. */
    private int deltaPct;
}
