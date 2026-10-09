package com.mevsme.repo;

import com.mevsme.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;

public interface ClutchEntryRepository extends JpaRepository<ClutchEntry, Long> {
    List<ClutchEntry> findByPlayerOrderByEntryDateDesc(PlayerProfile player);
}
