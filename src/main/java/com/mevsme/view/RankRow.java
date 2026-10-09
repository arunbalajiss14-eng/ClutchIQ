package com.mevsme.view;

import com.mevsme.model.PlayerAssessment;
import com.mevsme.model.PlayerProfile;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class RankRow {
    private PlayerProfile profile;
    private PlayerAssessment assessment;
    private OverallScore score;
}
