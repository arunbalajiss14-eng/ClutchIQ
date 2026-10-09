package com.mevsme.repo;

import com.mevsme.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    List<Tournament> findByTeamOrderByStartDateAsc(Team team);
    void deleteByTeam(Team team);
}
