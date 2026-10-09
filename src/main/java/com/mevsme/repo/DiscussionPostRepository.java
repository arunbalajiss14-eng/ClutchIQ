package com.mevsme.repo;

import com.mevsme.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;

public interface DiscussionPostRepository extends JpaRepository<DiscussionPost, Long> {
    List<DiscussionPost> findByTeamOrderByCreatedAtDesc(Team team);
    void deleteByTeam(Team team);
}
