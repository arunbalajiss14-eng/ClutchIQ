package com.mevsme.repo;

import com.mevsme.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface MatchRatingRepository extends JpaRepository<MatchRating, Long> {
    List<MatchRating> findByMatch(MatchFixture match);
    List<MatchRating> findByPlayer(PlayerProfile player);
    Optional<MatchRating> findByMatchAndPlayer(MatchFixture match, PlayerProfile player);
    void deleteByMatch(MatchFixture match);
}
