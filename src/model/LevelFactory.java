package model;

public final class LevelFactory {
    public Level createAdventureLevel(SeasonType season, int levelNumber,
                                      SpecialLevelType type, int waveCount,
                                      int firstWaveCost) {
        int startingSun = type == SpecialLevelType.PLANT_WHAT_YOU_GET ? 800 : 50;
        String levelId = season.name().toLowerCase().replace('_', '-') + "-" + levelNumber;
        Level level = new Level(levelId, season, levelNumber, type, 8, startingSun);
        level.getRuleStrategy().configure(level);
        addWaves(level, waveCount, firstWaveCost);
        return level;
    }

    private void addWaves(Level level, int waveCount, int firstWaveCost) {
        int previousCost = firstWaveCost;
        for (int waveNumber = 1; waveNumber <= waveCount; waveNumber++) {
            int cost = waveCost(level, waveNumber, waveCount, firstWaveCost, previousCost);
            level.addWave(new Wave(waveNumber, cost, 0));
            previousCost = cost;
        }
    }

    private int waveCost(Level level, int waveNumber, int waveCount,
                         int firstWaveCost, int previousCost) {
        if (level.getLevelNumber() == 1) {
            return waveNumber == waveCount
                ? roundToNearestFifty(firstWaveCost * 2.0)
                : firstWaveCost;
        }
        if (waveNumber == 1) {
            return previousCost;
        }
        if (waveNumber == waveCount) {
            return roundToNearestFifty(previousCost * 2.0);
        }
        return roundToNearestFifty(previousCost * 1.25);
    }

    private int roundToNearestFifty(double value) {
        return Math.max(1000, (int) Math.round(value / 50.0) * 50);
    }
}
