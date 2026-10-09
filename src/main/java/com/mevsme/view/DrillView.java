package com.mevsme.view;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DrillView {
    private String scenario;
    private String reason;
    private int nextTarget;
    private int attempts;
    private List<DrillPoint> history = new ArrayList<>();
    private boolean hasHistory;
}
