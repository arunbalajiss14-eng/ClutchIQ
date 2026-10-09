package com.mevsme.view;

import lombok.AllArgsConstructor;
import lombok.Data;

/** One labelled 0-100 value shown as a bar, e.g. one line of the Match Readiness breakdown. */
@Data @AllArgsConstructor
public class Metric {
    private String label;
    private int value;
}
