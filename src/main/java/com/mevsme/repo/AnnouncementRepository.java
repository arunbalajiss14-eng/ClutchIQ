package com.mevsme.repo;

import com.mevsme.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findByTeamOrderByCreatedAtDesc(Team team);
    void deleteByTeam(Team team);
}
