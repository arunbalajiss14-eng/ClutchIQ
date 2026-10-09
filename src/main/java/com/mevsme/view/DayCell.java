package com.mevsme.view;

import lombok.AllArgsConstructor;
import lombok.Data;

/** One day in a 7-day strip. pct is -1 when nothing counts for that day. */
@Data @AllArgsConstructor
public class DayCell {
    private String label;
    private int pct;
}
