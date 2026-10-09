package com.mevsme.view;

import com.mevsme.model.MatchFixture;
import com.mevsme.model.MatchRating;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/** A played match together with the coach's per-player ratings for it. */
@Data @AllArgsConstructor
public class MatchResultView {
    private MatchFixture match;
    private List<MatchRating> ratings;
}
