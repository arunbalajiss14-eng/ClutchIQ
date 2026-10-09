package com.mevsme.service;

import com.mevsme.model.DailyLog;
import com.mevsme.model.PlayerProfile;
import com.mevsme.repo.DailyLogRepository;
import com.mevsme.view.HistoryDay;
import com.mevsme.view.HistoryView;
import com.mevsme.view.WeekSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

/** The last 28 days of a player's daily routine, sleep and energy, plus a summary of each of the four weeks. */
@Service
@RequiredArgsConstructor
public class HistoryService {
    private static final int DAYS = 28;
    private final DailyLogRepository logs;

    public HistoryView build(PlayerProfile p) {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(DAYS - 1);
        double target = p.getTargetSleepHours() == null ? 8.0 : p.getTargetSleepHours();

        Map<LocalDate, DailyLog> byDate = new HashMap<>();
        for (DailyLog l : logs.findByPlayerAndLogDateBetweenOrderByLogDateAsc(p, start, today)) byDate.put(l.getLogDate(), l);

        HistoryView v = new HistoryView();
        v.setSleepTarget(target);
        DateTimeFormatter dm = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);

        for (int i = 0; i < DAYS; i++) {
            LocalDate d = start.plusDays(i);
            DailyLog l = byDate.get(d);
            int completion = l == null ? -1 : ScoreService.dayCompletion(l);
            Double sleep = l == null ? null : l.getSleepHours();
            Integer energy = l == null ? null : l.getEnergy();
            int sleepPct = sleep == null ? -1 : (int) Math.min(100, Math.round(sleep / target * 100));
            int energyPct = energy == null ? -1 : energy * 10;
            v.getDays().add(new HistoryDay(d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    d.format(dm), completion, sleep, sleepPct, energy, energyPct));
        }

        // weeks, most recent first
        for (int w = 0; w < 4; w++) {
            int endIdx = DAYS - 1 - 7 * w;
            List<HistoryDay> slice = v.getDays().subList(endIdx - 6, endIdx + 1);
            String label = w == 0 ? "Last 7 days" : slice.get(0).getDateLabel() + " to " + slice.get(6).getDateLabel();
            v.getWeeks().add(summarise(label, slice));
        }
        WeekSummary all = summarise("", v.getDays());
        v.setMonthCompletion(all.getCompletion());
        v.setAvgSleep(all.getAvgSleep());
        v.setAvgEnergy(all.getAvgEnergy());
        v.setDaysLogged(all.getDaysLogged());
        return v;
    }

    private static WeekSummary summarise(String label, List<HistoryDay> days) {
        int logged = 0, compSum = 0, sleepN = 0, energyN = 0;
        double sleepSum = 0, energySum = 0;
        for (HistoryDay d : days) {
            if (d.getCompletion() >= 0) { logged++; compSum += d.getCompletion(); }
            if (d.getSleepHours() != null) { sleepN++; sleepSum += d.getSleepHours(); }
            if (d.getEnergy() != null) { energyN++; energySum += d.getEnergy(); }
        }
        return new WeekSummary(label, logged == 0 ? 0 : (int) Math.round(compSum / (double) logged),
                sleepN == 0 ? null : Math.round(sleepSum / sleepN * 10) / 10.0,
                energyN == 0 ? null : Math.round(energySum / energyN * 10) / 10.0, logged);
    }
}
