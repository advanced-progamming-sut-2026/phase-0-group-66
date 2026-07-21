package model;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Effective, level-aware plant statistics calculated from the data-file upgrade descriptions.
 */
public final class PlantStats {
    private static final Pattern NUMBER = Pattern.compile("[-+]?\\d+(?:\\.\\d+)?");

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

    private PlantStats(PlantDefinition definition, int requestedLevel) {
        level = Math.max(1, Math.min(requestedLevel,
            definition.getLevelUpgrades().size() + 1));
        maxHealth = Math.max(1, definition.getBaseHealth());
        damage = Math.max(0, definition.getBaseDamage());
        cost = definition.getCost();
        actionIntervalSeconds = definition.getActionIntervalSeconds().orElse(1.0);
        rechargeSeconds = definition.getRechargeSeconds().orElse(0.0);
        for (int index = 0; index < level - 1; index++) {
            applyUpgrade(definition.getLevelUpgrades().get(index));
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

    private void applyUpgrade(String upgrade) {
        if (upgrade == null || upgrade.isBlank()) {
            return;
        }
        String normalized = upgrade.trim().toLowerCase(Locale.ROOT);
        double value = firstNumber(normalized);
        if (normalized.startsWith("hp +")) {
            maxHealth += (int) Math.round(value);
        } else if (isDamageUpgrade(normalized)) {
            damage += (int) Math.round(value);
        } else if (normalized.startsWith("cost -")) {
            cost -= (int) Math.round(Math.abs(value));
        } else if (normalized.startsWith("cooldown -")) {
            rechargeSeconds -= Math.abs(value);
        } else if (isActionTimeReduction(normalized)) {
            actionIntervalSeconds -= Math.abs(value);
        } else if (normalized.startsWith("atk speed +")) {
            actionIntervalSeconds /= 1.0 + Math.abs(value) / 100.0;
        } else if (normalized.startsWith("sun +")
            || normalized.startsWith("sun drop +")) {
            sunProductionBonus += (int) Math.round(value);
        } else if (normalized.contains("double sun chance")) {
            doubleSunChance = true;
        } else if (normalized.startsWith("chill time +")
            || normalized.startsWith("freeze time +")) {
            chillBonusTicks += (int) Math.round(Math.abs(value) * Game.TICKS_PER_SECOND);
        } else if (normalized.startsWith("pierce +")) {
            pierceBonus += (int) Math.round(value);
        }
    }

    private boolean isDamageUpgrade(String normalized) {
        return normalized.startsWith("dmg +")
            || normalized.startsWith("dmg/tick +")
            || normalized.startsWith("aoe dmg +")
            || normalized.startsWith("explode dmg +")
            || normalized.startsWith("reflect dmg +");
    }

    private boolean isActionTimeReduction(String normalized) {
        return normalized.startsWith("prod. time -")
            || normalized.startsWith("grow time -")
            || normalized.startsWith("charge time -")
            || normalized.startsWith("arm time -")
            || normalized.startsWith("regen -")
            || normalized.startsWith("digest -")
            || normalized.startsWith("eat time -");
    }

    private double firstNumber(String text) {
        Matcher matcher = NUMBER.matcher(text);
        if (!matcher.find()) {
            return 0.0;
        }
        return Double.parseDouble(matcher.group());
    }
}
