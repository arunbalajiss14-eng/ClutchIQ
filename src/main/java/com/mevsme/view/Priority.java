package com.mevsme.view;

import lombok.AllArgsConstructor;
import lombok.Data;

/** One item in today's AI-generated training plan. */
@Data @AllArgsConstructor
public class Priority {
    private String icon;
    private String title;
    private String detail;
    private String why;
    /** Matches a DailyLog status field so completing it here also completes the daily duty. */
    private String dutyKey;
    private boolean done;
}
