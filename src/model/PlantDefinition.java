package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;

public final class PlantDefinition {
    private final int id;
    private final boolean required;
    private final String key;
    private final String displayName;
    private final PlantFamily family;
    private final List<String> tags;
    private final int cost;
    private final int baseHealth;
    private final int baseDamage;
    private final String damageDisplay;
    private final double actionIntervalSeconds;
    private final double rechargeSeconds;
    private final int projectileCount;
    private final PlantAbility ability;
    private final double abilityPower;
    private final String abilityDescription;
    private final Map<String, Double> abilityParameters;
    private final PlantFoodType plantFoodType;
    private final double plantFoodPower;
    private final String plantFoodDescription;
    private final Map<String, Double> plantFoodParameters;
    private final List<PlantUpgrade> upgrades;

    public PlantDefinition(int id, boolean required, String key, String displayName,
                           PlantFamily family,
                           List<String> tags, int cost, int baseHealth, int baseDamage,
                           String damageDisplay, double actionIntervalSeconds,
                           double rechargeSeconds, int projectileCount,
                           PlantAbility ability, double abilityPower,
                           String abilityDescription, Map<String, Double> abilityParameters,
                           PlantFoodType plantFoodType, double plantFoodPower,
                           String plantFoodDescription,
                           Map<String, Double> plantFoodParameters,
                           List<PlantUpgrade> upgrades) {
        if (id <= 0) {
            throw new IllegalArgumentException("Plant id must be positive.");
        }
        this.id = id;
        this.required = required;
        this.key = requireText(key, "Plant key");
        this.displayName = requireText(displayName, "Plant display name");
        this.family = requireValue(family, "Plant family");
        this.tags = immutableCleanList(tags);
        this.cost = requireNonNegative(cost, "Plant cost");
        this.baseHealth = requireNonNegative(baseHealth, "Plant health");
        this.baseDamage = requireNonNegative(baseDamage, "Plant damage");
        this.damageDisplay = optionalText(damageDisplay, Integer.toString(baseDamage));
        this.actionIntervalSeconds = requireNonNegative(actionIntervalSeconds,
            "Plant action interval");
        this.rechargeSeconds = requireNonNegative(rechargeSeconds, "Plant recharge");
        if (projectileCount <= 0) {
            throw new IllegalArgumentException("Projectile count must be positive.");
        }
        this.projectileCount = projectileCount;
        this.ability = requireValue(ability, "Plant ability");
        this.abilityPower = abilityPower;
        this.abilityDescription = optionalText(abilityDescription, ability.name());
        this.abilityParameters = immutableNumberMap(abilityParameters);
        this.plantFoodType = requireValue(plantFoodType, "Plant food type");
        this.plantFoodPower = plantFoodPower;
        this.plantFoodDescription = optionalText(plantFoodDescription,
            plantFoodType == PlantFoodType.NONE ? "" : plantFoodType.name());
        this.plantFoodParameters = immutableNumberMap(plantFoodParameters);
        this.upgrades = immutableUpgrades(upgrades);
        validateUpgradeLevels();
    }

    public int getId() { return id; }
    public boolean isRequired() { return required; }
    public String getKey() { return key; }
    public String getName() { return displayName; }
    public String getDisplayName() { return displayName; }
    public PlantFamily getFamily() { return family; }
    public String getCategory() { return family.getDisplayName(); }
    public List<String> getTags() { return tags; }
    public int getCost() { return cost; }
    public int getBaseHealth() { return baseHealth; }
    public String getDamage() { return damageDisplay; }
    public int getBaseDamage() { return baseDamage; }
    public boolean isInstantKill() { return baseDamage >= 99_999; }
    public double getAbilityPower() { return abilityPower; }
    public PlantAbility getAbility() { return ability; }
    public PlantFoodType getPlantFoodType() { return plantFoodType; }
    public double getPlantFoodPower() { return plantFoodPower; }
    public int getProjectileCount() { return projectileCount; }
    public List<PlantUpgrade> getUpgrades() { return upgrades; }
    public Map<String, Double> getAbilityParameters() { return abilityParameters; }
    public Map<String, Double> getPlantFoodParameters() { return plantFoodParameters; }

    public double getAbilityParameter(String name, double fallback) {
        return parameter(abilityParameters, name, fallback);
    }

    public int getAbilityParameterInt(String name, int fallback) {
        return (int) Math.round(getAbilityParameter(name, fallback));
    }

    public double getPlantFoodParameter(String name, double fallback) {
        return parameter(plantFoodParameters, name, fallback);
    }

    public int getPlantFoodParameterInt(String name, int fallback) {
        return (int) Math.round(getPlantFoodParameter(name, fallback));
    }

    public String getBaseAbility() { return abilityDescription; }

    public String getPlantFoodEffect() { return plantFoodDescription; }

    public List<String> getLevelUpgrades() {
        ArrayList<String> result = new ArrayList<>();
        for (PlantUpgrade upgrade : upgrades) {
            result.add(upgrade.toString());
        }
        return Collections.unmodifiableList(result);
    }

    public OptionalDouble getActionIntervalSeconds() {
        return OptionalDouble.of(actionIntervalSeconds);
    }

    public OptionalDouble getRechargeSeconds() {
        return OptionalDouble.of(rechargeSeconds);
    }

    public boolean hasTag(String tag) {
        String normalized = normalizeKey(tag);
        for (String currentTag : tags) {
            if (normalizeKey(currentTag).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    public String getNormalizedName() { return normalizeKey(displayName); }
    public String getNormalizedKey() { return normalizeKey(key); }

    public static String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        String lowerCase = value.trim().toLowerCase(Locale.ROOT);
        for (int index = 0; index < lowerCase.length(); index++) {
            char current = lowerCase.charAt(index);
            if (Character.isLetterOrDigit(current)) {
                result.append(current);
            }
        }
        return result.toString();
    }

    @Override
    public String toString() {
        return displayName + " [family=" + family + ", cost=" + cost
            + ", health=" + baseHealth + ", damage=" + damageDisplay
            + ", ability=" + ability + "]";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PlantDefinition definition)) {
            return false;
        }
        return id == definition.id && key.equals(definition.key);
    }

    @Override
    public int hashCode() { return Objects.hash(id, key); }

    private void validateUpgradeLevels() {
        int expected = 2;
        for (PlantUpgrade upgrade : upgrades) {
            if (upgrade.getLevel() != expected) {
                throw new IllegalArgumentException(
                    "Plant upgrades must use consecutive levels starting at 2.");
            }
            expected++;
        }
    }

    private static List<String> immutableCleanList(List<String> input) {
        ArrayList<String> result = new ArrayList<>();
        if (input != null) {
            for (String value : input) {
                if (value != null && !value.isBlank() && !value.trim().equals("-")) {
                    result.add(value.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static List<PlantUpgrade> immutableUpgrades(List<PlantUpgrade> input) {
        ArrayList<PlantUpgrade> result = new ArrayList<>();
        if (input != null) {
            for (PlantUpgrade value : input) {
                if (value != null) {
                    result.add(value);
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static Map<String, Double> immutableNumberMap(Map<String, Double> input) {
        LinkedHashMap<String, Double> result = new LinkedHashMap<>();
        if (input != null) {
            for (Map.Entry<String, Double> entry : input.entrySet()) {
                if (entry.getKey() != null && !entry.getKey().isBlank()
                    && entry.getValue() != null) {
                    result.put(normalizeKey(entry.getKey()), entry.getValue());
                }
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static double parameter(Map<String, Double> parameters, String name,
                                    double fallback) {
        if (name == null) {
            return fallback;
        }
        return parameters.getOrDefault(normalizeKey(name), fallback);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
        return value.trim();
    }

    private static String optionalText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static int requireNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative.");
        }
        return value;
    }

    private static double requireNonNegative(double value, String fieldName) {
        if (value < 0.0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative.");
        }
        return value;
    }

    private static <T> T requireValue(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null.");
        }
        return value;
    }
}
