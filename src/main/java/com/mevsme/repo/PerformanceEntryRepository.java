package com.mevsme.repo;

import com.mevsme.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;

public interface PerformanceEntryRepository extends JpaRepository<PerformanceEntry, Long> {
    List<PerformanceEntry> findByPlayerOrderByEntryDateAsc(PlayerProfile player);
    Optional<PerformanceEntry> findByPlayerAndEntryDate(PlayerProfile player, LocalDate entryDate);
}
