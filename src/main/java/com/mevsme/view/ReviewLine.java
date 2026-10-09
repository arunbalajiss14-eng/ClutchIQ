package com.mevsme.view;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class ReviewLine {
    private String label;
    /** DONE, PARTIAL, MISSED or PENDING */
    private String state;
}
