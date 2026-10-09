package com.mevsme.repo;

import com.mevsme.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;

public interface DailyLogRepository extends JpaRepository<DailyLog, Long> {
    Optional<DailyLog> findByPlayerAndLogDate(PlayerProfile player, LocalDate date);
    List<DailyLog> findByPlayerAndLogDateBetweenOrderByLogDateAsc(PlayerProfile player, LocalDate from, LocalDate to);
}
