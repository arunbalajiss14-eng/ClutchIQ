package com.mevsme.view;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class WeekSummary {
    private String label;
    private int completion;
    private Double avgSleep;
    private Double avgEnergy;
    private int daysLogged;
}
