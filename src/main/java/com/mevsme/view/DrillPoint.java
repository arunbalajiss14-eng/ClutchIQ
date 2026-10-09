package com.mevsme.view;

import lombok.Data;

/** One logged pressure-drill attempt, pre-computed to a percentage for the bar chart. */
@Data
public class DrillPoint {
    private int successful;
    private int attempts;
    private int pct;

    public DrillPoint(int successful, int attempts) {
        this.successful = successful;
        this.attempts = attempts;
        this.pct = attempts <= 0 ? 0 : (int) Math.round(successful * 100.0 / attempts);
    }
}
