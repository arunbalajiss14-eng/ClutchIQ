package com.mevsme.service;

import com.mevsme.model.ClutchEntry;
import com.mevsme.model.PlayerProfile;
import com.mevsme.repo.ClutchEntryRepository;
import com.mevsme.view.ClutchView;
import com.mevsme.view.PlayerRow;
import com.mevsme.view.SituationStat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/** ClutchAI: finds the situations in which a player performs above or below their own average. */
@Service
@RequiredArgsConstructor
public class ClutchService {
    public static final List<String> SITUATIONS = List.of(
            "Normal play", "Close game", "Final period", "Trailing", "Leading",
            "Home", "Away", "Against strong opponent");

    private static final int MIN_ENTRIES = 4;

    private final ClutchEntryRepository entries;

    public ClutchView build(PlayerProfile player) {
        List<ClutchEntry> all = entries.findByPlayerOrderByEntryDateDesc(player);
        ClutchView view = new ClutchView();
        view.setEntryCount(all.size());
        if (all.size() < MIN_ENTRIES) {
            view.setEnough(false);
            view.getInsights().add("Add at least " + MIN_ENTRIES + " match entries with their situation to unlock ClutchAI patterns.");
            return view;
        }
        view.setEnough(true);
        double baseline = all.stream().mapToDouble(ClutchEntry::getValue).average().orElse(0);
        view.setBaseline(round1(baseline));

        Map<String, List<ClutchEntry>> bySituation = new LinkedHashMap<>();
        for (ClutchEntry e : all) bySituation.computeIfAbsent(e.getSituation(), k -> new ArrayList<>()).add(e);

        List<SituationStat> stats = new ArrayList<>();
        for (Map.Entry<String, List<ClutchEntry>> en : bySituation.entrySet()) {
            double avg = en.getValue().stream().mapToDouble(ClutchEntry::getValue).average().orElse(0);
            int delta = baseline == 0 ? 0 : (int) Math.round((avg - baseline) / baseline * 100);
            stats.add(new SituationStat(en.getKey(), round1(avg), en.getValue().size(), delta));
        }
        stats.sort(Comparator.comparingDouble(SituationStat::getAverage).reversed());
        view.setStats(stats);
        view.setBest(stats.get(0));
        view.setWorst(stats.get(stats.size() - 1));

        SituationStat best = view.getBest();
        SituationStat worst = view.getWorst();
        if (best.getDeltaPct() > 0) {
            view.getInsights().add("Best situation: " + best.getSituation() + ". Averages " + best.getAverage()
                    + " against a normal " + view.getBaseline() + " (" + signed(best.getDeltaPct()) + ").");
        }
        if (worst.getDeltaPct() < 0 && !worst.getSituation().equals(best.getSituation())) {
            view.getInsights().add("Performance drops in: " + worst.getSituation() + " (" + signed(worst.getDeltaPct())
                    + "). Plan pressure drills that copy this situation.");
        }
        if (view.getInsights().isEmpty()) {
            view.getInsights().add("Performance is steady across situations so far. Keep adding entries to sharpen the picture.");
        }
        return view;
    }

    /** One line per player who has enough data, for the coach dashboard. */
    public List<String> teamInsights(List<PlayerRow> rows) {
        List<String> out = new ArrayList<>();
        for (PlayerRow r : rows) {
            ClutchView v = build(r.getProfile());
            if (v.isEnough() && v.getBest() != null && v.getBest().getDeltaPct() > 0) {
                out.add(r.getProfile().getUser().getFullName() + " peaks in " + v.getBest().getSituation().toLowerCase()
                        + " (" + signed(v.getBest().getDeltaPct()) + " above their average).");
            }
        }
        return out;
    }

    private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }
    private static String signed(int v) { return (v > 0 ? "+" : "") + v + "%"; }
}
