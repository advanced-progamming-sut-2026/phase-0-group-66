package model;

import java.time.LocalDate;
import java.util.Random;

public final class DailyScoredLevelFactory {
    public ScoredLevel create(LocalDate date) {
        LocalDate actualDate = date == null ? LocalDate.now() : date;
        long seed = actualDate.toEpochDay() * 1_000_003L + 66L;
        Random random = new Random(seed);
        SeasonType[] seasons = SeasonType.values();
        SeasonType season = seasons[Math.floorMod((int) actualDate.toEpochDay(), seasons.length)];
        Chapter chapter = new Chapter("Daily Scored Arena", 0 + 1, season, true);
        Level level = new Level("scored-" + actualDate, season, 1,
            SpecialLevelType.NORMAL, 8, 100);
        int waveCount = 5;
        int previousCost = 1100 + random.nextInt(7) * 50;
        for (int wave = 1; wave <= waveCount; wave++) {
            int cost;
            if (wave == 1) {
                cost = previousCost;
            } else if (wave == waveCount) {
                cost = roundToFifty(previousCost * 2.0);
            } else {
                cost = roundToFifty(previousCost * (1.20 + random.nextDouble() * 0.10));
            }
            level.addWave(new Wave(wave, cost, 0));
            previousCost = cost;
        }
        chapter.addLevel(level);
        return new ScoredLevel(chapter, level, seed, actualDate);
    }

    private int roundToFifty(double value) {
        return Math.max(1000, (int) Math.round(value / 50.0) * 50);
    }

    public record ScoredLevel(Chapter chapter, Level level, long randomSeed, LocalDate date) { }
}
