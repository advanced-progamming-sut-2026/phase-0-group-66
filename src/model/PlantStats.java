package model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Effective, level-aware plant statistics calculated from typed upgrades. */
public final class PlantStats {
    private final int level;
    private int maxHealth;
    private int damage;
    private int cost;
    private double actionIntervalSeconds;
    private double rechargeSeconds;
    private int sunProductionBonus;
    private boolean doubleSunChance;
    private int chillBonusTicks;
    private int pierceBonus;
    private final LinkedHashMap<String, Double> traitValues;

    private PlantStats(PlantDefinition definition, int requestedLevel) {
        level = Math.max(1, Math.min(requestedLevel, definition.getUpgrades().size() + 1));
        maxHealth = Math.max(1, definition.getBaseHealth());
        damage = Math.max(0, definition.getBaseDamage());
        cost = definition.getCost();
        actionIntervalSeconds = definition.getActionIntervalSeconds().orElse(1.0);
        rechargeSeconds = definition.getRechargeSeconds().orElse(0.0);
        traitValues = new LinkedHashMap<>();
        for (PlantUpgrade upgrade : definition.getUpgrades()) {
            if (upgrade.getLevel() <= level) {
                applyUpgrade(upgrade);
            }
        }
        actionIntervalSeconds = Math.max(0.1, actionIntervalSeconds);
        rechargeSeconds = Math.max(0.0, rechargeSeconds);
        cost = Math.max(0, cost);
    }

    public static PlantStats calculate(PlantDefinition definition, int level) {
        if (definition == null) {
            throw new IllegalArgumentException("Plant definition cannot be null.");
        }
        return new PlantStats(definition, level);
    }

    public int getLevel() { return level; }
    public int getMaxHealth() { return maxHealth; }
    public int getDamage() { return damage; }
    public int getCost() { return cost; }
    public double getActionIntervalSeconds() { return actionIntervalSeconds; }
    public double getRechargeSeconds() { return rechargeSeconds; }
    public int getSunProductionBonus() { return sunProductionBonus; }
    public boolean hasDoubleSunChance() { return doubleSunChance; }
    public int getChillBonusTicks() { return chillBonusTicks; }
    public int getPierceBonus() { return pierceBonus; }
    public boolean hasTrait(String trait) {
        return traitValues.containsKey(normalizeTrait(trait));
    }
    public double getTraitValue(String trait, double fallback) {
        return traitValues.getOrDefault(normalizeTrait(trait), fallback);
    }
    public Map<String, Double> getTraitValues() {
        return Collections.unmodifiableMap(traitValues);
    }

    private void applyUpgrade(PlantUpgrade upgrade) {
        double amount = upgrade.getAmount();
        switch (upgrade.getEffect()) {
            case MAX_HEALTH_DELTA -> maxHealth += (int) Math.round(amount);
            case DAMAGE_DELTA -> damage += (int) Math.round(amount);
            case SUN_COST_DELTA -> cost += (int) Math.round(amount);
            case ACTION_INTERVAL_DELTA -> actionIntervalSeconds += amount;
            case ACTION_SPEED_PERCENT -> actionIntervalSeconds /= 1.0 + Math.abs(amount) / 100.0;
            case RECHARGE_DELTA -> rechargeSeconds += amount;
            case SUN_OUTPUT_DELTA -> sunProductionBonus += (int) Math.round(amount);
            case CHILL_DURATION_DELTA -> chillBonusTicks += (int) Math.round(
                amount * Game.TICKS_PER_SECOND);
            case PIERCE_DELTA -> pierceBonus += (int) Math.round(amount);
            case ENABLE_TRAIT -> applyTrait(upgrade.getTrait(), amount);
        }
    }

    private void applyTrait(String trait, double amount) {
        String normalized = normalizeTrait(trait);
        if (normalized.isEmpty()) {
            return;
        }
        traitValues.merge(normalized, amount, Double::sum);
        if ("DOUBLE_SUN_CHANCE".equals(normalized)) {
            doubleSunChance = true;
        }
    }

    private static String normalizeTrait(String trait) {
        return trait == null ? "" : trait.trim().toUpperCase(Locale.ROOT);
    }
}
