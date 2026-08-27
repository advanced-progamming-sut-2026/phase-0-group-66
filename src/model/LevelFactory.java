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
            int cost = waveCost(waveNumber, waveCount, firstWaveCost, previousCost);
            level.addWave(new Wave(waveNumber, cost, 0));
            previousCost = cost;
        }
    }

    private int waveCost(int waveNumber, int waveCount, int firstWaveCost, int previousCost) {
        if (waveNumber == 1) {
            return roundToNearestFifty(Math.max(1000, firstWaveCost));
        }
        double multiplier = waveNumber == waveCount ? 2.0 : 1.25;
        int scaledCost = roundToNearestFifty(previousCost * multiplier);
        return Math.max(previousCost + 500, scaledCost);
    }

    private int roundToNearestFifty(double value) {
        return Math.max(1000, (int) Math.round(value / 50.0) * 50);
    }
}
