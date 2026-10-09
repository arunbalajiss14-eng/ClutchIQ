package com.mevsme.repo;

import com.mevsme.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface PressureDrillRepository extends JpaRepository<PressureDrillResult, Long> {
    List<PressureDrillResult> findByPlayerOrderByEntryDateDesc(PlayerProfile player);
}
