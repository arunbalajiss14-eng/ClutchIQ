package com.mevsme.view;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class HistoryView {
    private List<WeekSummary> weeks = new ArrayList<>();
    private List<HistoryDay> days = new ArrayList<>();
    private int monthCompletion;
    private Double avgSleep;
    private Double avgEnergy;
    private int daysLogged;
    private double sleepTarget;
}
