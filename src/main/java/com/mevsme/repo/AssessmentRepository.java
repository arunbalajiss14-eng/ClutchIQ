package com.mevsme.repo;

import com.mevsme.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;

public interface AssessmentRepository extends JpaRepository<PlayerAssessment, Long> {
    Optional<PlayerAssessment> findByPlayer(PlayerProfile player);
}
