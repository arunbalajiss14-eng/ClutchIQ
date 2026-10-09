package com.mevsme.web;

import com.mevsme.model.*;
import com.mevsme.repo.*;
import com.mevsme.service.ContextService;
import com.mevsme.service.ScoreService;
import com.mevsme.view.MatchManageView;
import com.mevsme.view.RankRow;
import com.mevsme.view.SquadRankRow;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Coach-only "Upcoming match" management: create and edit matches (including age requirement and
 * fee), pick the squad ranked by each player's overall percentage, see who has confirmed, and
 * turn the public ranking link on or off.
 */
@Controller
@RequestMapping("/coach/matches")
@RequiredArgsConstructor
public class CoachMatchController {
    private static final String ISO_DATE = "yyyy-MM-dd";

    private final ContextService ctx;
    private final MatchFixtureRepository matches;
    private final MatchSelectionRepository selections;
    private final MatchRatingRepository ratings;
    private final PlayerProfileRepository profiles;
    private final ScoreService scores;

    private Team team(Principal p, HttpSession s) {
        Team t = ctx.activeTeam(ctx.user(p), s);
        if (t == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Create a team first");
        return t;
    }

    @GetMapping
    public String list(Principal p, HttpSession s, Model m) {
        Team t = team(p, s);
        List<RankRow> ranking = scores.ranking(t);
        List<MatchManageView> views = matches.findByTeamOrderByMatchDateAsc(t).stream()
                .filter(mf -> !mf.isPlayed())
                .map(mf -> {
                    List<MatchSelection> sel = selections.findByMatch(mf);
                    Set<Long> selectedIds = sel.stream().map(x -> x.getPlayer().getId()).collect(Collectors.toSet());
                    Set<Long> confirmedIds = sel.stream().filter(MatchSelection::isConfirmed)
                            .map(x -> x.getPlayer().getId()).collect(Collectors.toSet());
                    List<SquadRankRow> picks = ranking.stream()
                            .map(r -> new SquadRankRow(r.getProfile(), r.getScore().getOverall(),
                                    selectedIds.contains(r.getProfile().getId()), confirmedIds.contains(r.getProfile().getId())))
                            .toList();
                    return new MatchManageView(mf, picks, confirmedIds.size(), selectedIds.size());
                }).toList();
        m.addAttribute("team", t);
        m.addAttribute("views", views);
        m.addAttribute("shareUrl", t.getShareToken() == null ? null
                : org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/share/" + t.getShareToken()).build().toUriString());
        return "coach/matches";
    }

    @PostMapping
    public String create(Principal p, HttpSession s, RedirectAttributes ra,
                         @RequestParam String opponent,
                         @RequestParam @DateTimeFormat(pattern = ISO_DATE) LocalDate matchDate,
                         @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime matchTime,
                         @RequestParam(required = false) String location,
                         @RequestParam(required = false) Integer ageRequirement,
                         @RequestParam(required = false) String fee,
                         @RequestParam(required = false) String info) {
        MatchFixture mf = new MatchFixture();
        mf.setTeam(team(p, s)); mf.setOpponent(opponent); mf.setMatchDate(matchDate); mf.setMatchTime(matchTime);
        mf.setLocation(location); mf.setAgeRequirement(ageRequirement); mf.setFee(fee); mf.setInfo(info);
        matches.save(mf);
        ra.addFlashAttribute("flash", "Match created. Select the squad below.");
        return "redirect:/coach/matches";
    }

    @PostMapping("/{id}")
    public String edit(Principal p, HttpSession s, RedirectAttributes ra, @PathVariable Long id,
                       @RequestParam String opponent,
                       @RequestParam @DateTimeFormat(pattern = ISO_DATE) LocalDate matchDate,
                       @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime matchTime,
                       @RequestParam(required = false) String location,
                       @RequestParam(required = false) Integer ageRequirement,
                       @RequestParam(required = false) String fee,
                       @RequestParam(required = false) String info) {
        Team t = team(p, s);
        MatchFixture mf = matches.findById(id).filter(x -> x.getTeam().getId().equals(t.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        mf.setOpponent(opponent); mf.setMatchDate(matchDate); mf.setMatchTime(matchTime);
        mf.setLocation(location); mf.setAgeRequirement(ageRequirement); mf.setFee(fee); mf.setInfo(info);
        matches.save(mf);
        ra.addFlashAttribute("flash", "Match updated.");
        return "redirect:/coach/matches";
    }

    /** Replaces the whole squad for one match with the checked players. Dropped players lose their confirmation. */
    @PostMapping("/{id}/squad")
    public String squad(Principal p, HttpSession s, RedirectAttributes ra, @PathVariable Long id,
                        @RequestParam(required = false) List<Long> playerIds) {
        Team t = team(p, s);
        MatchFixture mf = matches.findById(id).filter(x -> x.getTeam().getId().equals(t.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<MatchSelection> existing = selections.findByMatch(mf);
        Set<Long> keep = playerIds == null ? Set.of() : Set.copyOf(playerIds);
        for (MatchSelection sel : existing) if (!keep.contains(sel.getPlayer().getId())) selections.delete(sel);
        if (playerIds != null) {
            Set<Long> already = existing.stream().map(x -> x.getPlayer().getId()).collect(Collectors.toSet());
            for (Long pid : playerIds) {
                if (already.contains(pid)) continue;
                profiles.findById(pid).filter(pp -> pp.getTeam() != null && pp.getTeam().getId().equals(t.getId()))
                        .ifPresent(pp -> selections.save(new MatchSelection(mf, pp)));
            }
        }
        ra.addFlashAttribute("flash", "Squad saved for vs " + mf.getOpponent() + ".");
        return "redirect:/coach/matches";
    }

    /** Deletes a match entirely, upcoming or already played, along with its squad picks and any player ratings. */
    @PostMapping("/{id}/delete")
    public String delete(Principal p, HttpSession s, RedirectAttributes ra, @PathVariable Long id,
                         @RequestParam(required = false, defaultValue = "matches") String from) {
        Team t = team(p, s);
        MatchFixture mf = matches.findById(id).filter(x -> x.getTeam().getId().equals(t.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        selections.deleteByMatch(mf);
        ratings.deleteByMatch(mf);
        matches.delete(mf);
        ra.addFlashAttribute("flash", "Match vs " + mf.getOpponent() + " deleted.");
        return "results".equals(from) ? "redirect:/team/results" : "redirect:/coach/matches";
    }
}
