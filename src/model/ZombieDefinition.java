package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ZombieDefinition {
    private final String alias;
    private final String displayName;
    private final int hitpoints;
    private final int eatDamagePerSecond;
    private final double speed;
    private final int wavePointCost;
    private final int weight;
    private final boolean canSpawnPlantFood;
    private final List<String> armorAliases;
    private final Map<String, Object> specialProperties;

    public ZombieDefinition(String alias, String displayName, int hitpoints,
                            int eatDamagePerSecond, double speed, int wavePointCost, int weight,
                            boolean canSpawnPlantFood, List<String> armorAliases,
                            Map<String, Object> specialProperties) {
        this.alias = requireText(alias, "Zombie alias");
        this.displayName = requireText(displayName, "Zombie display name");
        this.hitpoints = requireNonNegative(hitpoints, "Zombie hitpoints");
        this.eatDamagePerSecond = requireNonNegative(eatDamagePerSecond, "Zombie damage");
        if (speed < 0) {
            throw new IllegalArgumentException("Zombie speed cannot be negative.");
        }
        this.speed = speed;
        this.wavePointCost = requireNonNegative(wavePointCost, "Zombie wave cost");
        this.weight = requireNonNegative(weight, "Zombie weight");
        this.canSpawnPlantFood = canSpawnPlantFood;
        this.armorAliases = immutableStrings(armorAliases);
        this.specialProperties = Collections.unmodifiableMap(new LinkedHashMap<>(specialProperties));
    }

    public String getAlias() {
        return alias;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getHitpoints() {
        return hitpoints;
    }

    public int getEatDamagePerSecond() {
        return eatDamagePerSecond;
    }

    public double getSpeed() {
        return speed;
    }

    public int getWavePointCost() {
        return wavePointCost;
    }

    public int getWeight() {
        return weight;
    }

    public boolean canSpawnPlantFood() {
        return canSpawnPlantFood;
    }

    public List<String> getArmorAliases() {
        return armorAliases;
    }

    public Map<String, Object> getSpecialProperties() {
        return specialProperties;
    }

    public Object getSpecialProperty(String key) {
        return specialProperties.get(key);
    }

    public String getNormalizedAlias() {
        return PlantDefinition.normalizeKey(alias);
    }

    @Override
    public String toString() {
        return displayName + " [health=" + hitpoints + ", damage=" + eatDamagePerSecond
            + ", speed=" + speed + ", waveCost=" + wavePointCost + "]";
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
        return value.trim();
    }

    private static int requireNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative.");
        }
        return value;
    }

    private static List<String> immutableStrings(List<String> values) {
        ArrayList<String> result = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    result.add(value.trim());
                }
            }
        }
        return Collections.unmodifiableList(result);
    }
}
