package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ZombieDefinition {
    private final String key;
    private final String alias;
    private final String displayName;
    private final int hitpoints;
    private final int eatDamagePerSecond;
    private final double speed;
    private final int wavePointCost;
    private final int weight;
    private final boolean canSpawnPlantFood;
    private final ZombieAbility ability;
    private final List<SeasonType> seasons;
    private final List<String> armorAliases;
    private final List<String> lookupAliases;
    private final Map<String, Object> specialProperties;

    public ZombieDefinition(String key, String alias, String displayName, int hitpoints,
                            int eatDamagePerSecond, double speed, int wavePointCost, int weight,
                            boolean canSpawnPlantFood, ZombieAbility ability,
                            List<SeasonType> seasons, List<String> armorAliases,
                            List<String> lookupAliases,
                            Map<String, Object> specialProperties) {
        this.key = requireText(key, "Zombie key");
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
        this.ability = ability == null ? ZombieAbility.GENERIC : ability;
        this.seasons = seasons == null ? List.of() : List.copyOf(seasons);
        this.armorAliases = immutableStrings(armorAliases);
        this.lookupAliases = immutableStrings(lookupAliases);
        this.specialProperties = Collections.unmodifiableMap(
            new LinkedHashMap<>(specialProperties == null ? Map.of() : specialProperties));
    }

    public String getKey() { return key; }
    public String getAlias() { return alias; }
    public String getDisplayName() { return displayName; }
    public int getHitpoints() { return hitpoints; }
    public int getEatDamagePerSecond() { return eatDamagePerSecond; }
    public double getSpeed() { return speed; }
    public int getWavePointCost() { return wavePointCost; }
    public int getWeight() { return weight; }
    public boolean canSpawnPlantFood() { return canSpawnPlantFood; }
    public ZombieAbility getAbility() { return ability; }
    public List<SeasonType> getSeasons() { return seasons; }
    public boolean isAvailableIn(SeasonType season) { return seasons.contains(season); }
    public List<String> getArmorAliases() { return armorAliases; }
    public List<String> getLookupAliases() { return lookupAliases; }
    public Map<String, Object> getSpecialProperties() { return specialProperties; }
    public Object getSpecialProperty(String propertyKey) { return specialProperties.get(propertyKey); }

    public int getSpecialPropertyInt(String propertyKey, int fallback) {
        Object value = specialProperties.get(propertyKey);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    public double getSpecialPropertyDouble(String propertyKey, double fallback) {
        Object value = specialProperties.get(propertyKey);
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    public List<String> getSpecialPropertyStrings(String propertyKey) {
        Object value = specialProperties.get(propertyKey);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        ArrayList<String> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof String text && !text.isBlank()) {
                result.add(text.trim());
            }
        }
        return List.copyOf(result);
    }

    public String getNormalizedAlias() { return PlantDefinition.normalizeKey(alias); }
    public String getNormalizedKey() { return PlantDefinition.normalizeKey(key); }

    @Override
    public String toString() {
        return displayName + " [health=" + hitpoints + ", damage=" + eatDamagePerSecond
            + ", speed=" + speed + ", waveCost=" + wavePointCost
            + ", ability=" + ability + "]";
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
