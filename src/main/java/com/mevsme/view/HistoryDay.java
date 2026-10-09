package com.mevsme.view;

import lombok.AllArgsConstructor;
import lombok.Data;

/** One day in the 28-day history. Percent fields are -1 when nothing was logged for that day. */
@Data @AllArgsConstructor
public class HistoryDay {
    private String label;
    private String dateLabel;
    private int completion;
    private Double sleepHours;
    private int sleepPct;
    private Integer energy;
    private int energyPct;
}
