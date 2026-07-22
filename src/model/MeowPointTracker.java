package model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Tracks the five independent scoring patterns required by scored mode. */
public final class MeowPointTracker {
    private final LinkedHashMap<String, Integer> breakdown = new LinkedHashMap<>();
    private int syncedKills;
    private int syncedQuickKills;
    private int syncedMultiKills;
    private int syncedPiercingHits;
    private int syncedFrontLineKills;
    private boolean finalized;

    public MeowPointTracker() {
        breakdown.put("Zombie defeats", 0);
        breakdown.put("Quick kills", 0);
        breakdown.put("Simultaneous multi-kills", 0);
        breakdown.put("Piercing chains", 0);
        breakdown.put("Front-line saves", 0);
        breakdown.put("Completion efficiency", 0);
    }

    public void update(Game game) {
        if (game == null) {
            return;
        }
        addDelta("Zombie defeats", game.getZombieKillCount() - syncedKills, 100);
        addDelta("Quick kills", game.getKillsWithinThirtySeconds() - syncedQuickKills, 175);
        addDelta("Simultaneous multi-kills",
            game.getMultiKillZombieCount() - syncedMultiKills, 225);
        addDelta("Piercing chains", game.getPiercingProjectileHits() - syncedPiercingHits, 35);
        addDelta("Front-line saves",
            game.getFirstColumnNoMowerKills() - syncedFrontLineKills, 300);
        syncedKills = game.getZombieKillCount();
        syncedQuickKills = game.getKillsWithinThirtySeconds();
        syncedMultiKills = game.getMultiKillZombieCount();
        syncedPiercingHits = game.getPiercingProjectileHits();
        syncedFrontLineKills = game.getFirstColumnNoMowerKills();
    }

    public void finalizeScore(Game game) {
        if (finalized || game == null) {
            return;
        }
        update(game);
        int survivingPlants = game.getBoard() == null ? 0 : game.getBoard().getPlants().size();
        int unusedMowers = 0;
        if (game.getBoard() != null) {
            for (LawnMower mower : game.getBoard().getLawnMowers()) {
                if (!mower.isActivated()) {
                    unusedMowers++;
                }
            }
        }
        int efficiency = Math.max(0, game.getSunAmount()) * 2
            + survivingPlants * 50 + unusedMowers * 200;
        if (game.getGameState() == GameState.WON) {
            efficiency += 1500;
        }
        breakdown.put("Completion efficiency", efficiency);
        finalized = true;
    }

    public int getTotalScore() {
        return breakdown.values().stream().mapToInt(Integer::intValue).sum();
    }

    public Map<String, Integer> getBreakdown() {
        return Collections.unmodifiableMap(breakdown);
    }

    public String status() {
        StringBuilder builder = new StringBuilder("Meowpoints: ")
            .append(getTotalScore()).append('\n');
        for (Map.Entry<String, Integer> entry : breakdown.entrySet()) {
            builder.append("- ").append(entry.getKey()).append(": ")
                .append(entry.getValue()).append('\n');
        }
        return builder.toString().stripTrailing();
    }

    private void addDelta(String key, int delta, int pointsPerUnit) {
        if (delta > 0) {
            breakdown.merge(key, delta * pointsPerUnit, Integer::sum);
        }
    }
}
