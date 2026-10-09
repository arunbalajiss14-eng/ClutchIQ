package com.mevsme.view;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** Everything the AI Performance Coach page needs, for both the player's own view and the coach's view of a player. */
@Data
public class AiCoachView {
    private String focusArea;
    private String focusWhy;
    private List<Priority> priorities = new ArrayList<>();

    private int readinessOverall;
    private int readinessSignalCount;
    private boolean readinessHasFullData;
    private List<Metric> readinessBreakdown = new ArrayList<>();
    private String readinessNote;

    private Integer daysToMatch;
    private String matchOpponent;

    private boolean hasYesterday;
    private int yesterdayCompletion;
    private List<ReviewLine> yesterdayLines = new ArrayList<>();
    private String reviewFeedback;

    private List<DayCell> week = new ArrayList<>(); // 7 days, oldest first

    private List<String> clutchInsights = new ArrayList<>();
    private DrillView drill;
}
