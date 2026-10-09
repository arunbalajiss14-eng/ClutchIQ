package com.mevsme.service;

import com.mevsme.model.AppUser;
import com.mevsme.model.PlayerProfile;
import com.mevsme.model.Team;
import com.mevsme.repo.AppUserRepository;
import com.mevsme.repo.PlayerProfileRepository;
import com.mevsme.repo.TeamRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

/** Resolves who is logged in and which team is active, so the Coach/Player and Team contexts share one login. */
@Service
@RequiredArgsConstructor
public class ContextService {
    public static final String ACTIVE_TEAM = "activeTeamId";

    private final AppUserRepository users;
    private final TeamRepository teams;
    private final PlayerProfileRepository profiles;

    public AppUser user(Principal principal) {
        return users.findByEmailIgnoreCase(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Signed-in user no longer exists"));
    }

    public Optional<PlayerProfile> playerProfile(AppUser user) {
        return profiles.findByUser(user);
    }

    public List<Team> teamsOf(AppUser coach) {
        return teams.findByCoachOrderByCreatedAtAsc(coach);
    }

    /** Coach: the team chosen in the session (or the first one). Player: their own team. May return null. */
    public Team activeTeam(AppUser user, HttpSession session) {
        if (user.isCoach()) {
            List<Team> mine = teamsOf(user);
            if (mine.isEmpty()) return null;
            Object id = session.getAttribute(ACTIVE_TEAM);
            if (id instanceof Long teamId) {
                for (Team t : mine) if (t.getId().equals(teamId)) return t;
            }
            return mine.get(0);
        }
        return profiles.findByUser(user).map(PlayerProfile::getTeam).orElse(null);
    }
}
