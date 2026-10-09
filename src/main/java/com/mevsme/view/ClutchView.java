package com.mevsme.view;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ClutchView {
    private boolean enough;
    private int entryCount;
    private double baseline;
    private List<SituationStat> stats = new ArrayList<>();
    private SituationStat best;
    private SituationStat worst;
    private List<String> insights = new ArrayList<>();
}
