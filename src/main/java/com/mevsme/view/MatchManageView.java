package com.mevsme.view;

import com.mevsme.model.MatchFixture;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/** One upcoming match with its squad ranked by overall percentage, for the coach's Upcoming Match page. */
@Data @AllArgsConstructor
public class MatchManageView {
    private MatchFixture match;
    private List<SquadRankRow> picks;
    private int confirmedCount;
    private int selectedCount;
}
