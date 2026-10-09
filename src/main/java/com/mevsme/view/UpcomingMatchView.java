package com.mevsme.view;

import com.mevsme.model.MatchFixture;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/** An upcoming match together with who is currently selected for its squad. */
@Data @AllArgsConstructor
public class UpcomingMatchView {
    private MatchFixture match;
    private List<String> squadNames;
    private List<SquadPick> picks;
}
