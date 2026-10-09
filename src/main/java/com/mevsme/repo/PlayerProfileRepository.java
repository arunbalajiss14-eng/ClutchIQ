package com.mevsme.repo;

import com.mevsme.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;

public interface PlayerProfileRepository extends JpaRepository<PlayerProfile, Long> {
    Optional<PlayerProfile> findByUser(AppUser user);
    List<PlayerProfile> findByTeam(Team team);
    List<PlayerProfile> findByTeamAndUser_ActiveTrue(Team team);
    List<PlayerProfile> findByTeamAndUser_ActiveFalse(Team team);
}
