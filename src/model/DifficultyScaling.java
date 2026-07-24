package model;

public final class DifficultyScaling {
    private DifficultyScaling() { }

    public static double intensityFactor(int difficultyLevel) {
        validate(difficultyLevel);
        return difficultyLevel / 3.0;
    }

    public static double durationFactor(int difficultyLevel) {
        return 1.0 / intensityFactor(difficultyLevel);
    }

    public static int scaleDurationTicks(int baseTicks, int difficultyLevel) {
        if (baseTicks <= 0) {
            return 0;
        }
        return Math.max(1, (int) Math.round(baseTicks * durationFactor(difficultyLevel)));
    }

    private static void validate(int difficultyLevel) {
        if (difficultyLevel < 1 || difficultyLevel > 5) {
            throw new IllegalArgumentException("Difficulty level must be between 1 and 5.");
        }
    }
}
