package com.mevsme.repo;

import com.mevsme.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;

public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> findByCoachOrderByCreatedAtAsc(AppUser coach);
    Optional<Team> findByShareToken(String shareToken);
}
