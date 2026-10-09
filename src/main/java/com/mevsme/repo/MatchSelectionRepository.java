package com.mevsme.repo;

import com.mevsme.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface MatchSelectionRepository extends JpaRepository<MatchSelection, Long> {
    List<MatchSelection> findByMatch(MatchFixture match);
    java.util.Optional<MatchSelection> findByMatchAndPlayer(MatchFixture match, PlayerProfile player);
    void deleteByMatch(MatchFixture match);
}
