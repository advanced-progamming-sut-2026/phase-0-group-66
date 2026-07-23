package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalDouble;

/** Immutable, machine-readable plant definition loaded from plants.json. */
public final class PlantDefinition {
    private final int id;
    private final String key;
    private final String displayName;
    private final PlantFamily family;
    private final List<String> tags;
    private final int cost;
    private final int baseHealth;
    private final int baseDamage;
    private final double actionIntervalSeconds;
    private final double rechargeSeconds;
    private final int projectileCount;
    private final PlantAbility ability;
    private final double abilityPower;
    private final PlantFoodType plantFoodType;
    private final double plantFoodPower;
    private final List<PlantUpgrade> upgrades;

    public PlantDefinition(int id, String key, String displayName, PlantFamily family,
                           List<String> tags, int cost, int baseHealth, int baseDamage,
                           double actionIntervalSeconds, double rechargeSeconds,
                           int projectileCount, PlantAbility ability, double abilityPower,
                           PlantFoodType plantFoodType, double plantFoodPower,
                           List<PlantUpgrade> upgrades) {
        if (id <= 0) {
            throw new IllegalArgumentException("Plant id must be positive.");
        }
        this.id = id;
        this.key = requireText(key, "Plant key");
        this.displayName = requireText(displayName, "Plant display name");
        this.family = requireValue(family, "Plant family");
        this.tags = immutableCleanList(tags);
        this.cost = requireNonNegative(cost, "Plant cost");
        this.baseHealth = requireNonNegative(baseHealth, "Plant health");
        this.baseDamage = requireNonNegative(baseDamage, "Plant damage");
        this.actionIntervalSeconds = requireNonNegative(actionIntervalSeconds,
            "Plant action interval");
        this.rechargeSeconds = requireNonNegative(rechargeSeconds, "Plant recharge");
        if (projectileCount <= 0) {
            throw new IllegalArgumentException("Projectile count must be positive.");
        }
        this.projectileCount = projectileCount;
        this.ability = requireValue(ability, "Plant ability");
        this.abilityPower = abilityPower;
        this.plantFoodType = requireValue(plantFoodType, "Plant food type");
        this.plantFoodPower = plantFoodPower;
        this.upgrades = immutableUpgrades(upgrades);
        validateUpgradeLevels();
    }

    public int getId() { return id; }
    public String getKey() { return key; }
    public String getName() { return displayName; }
    public String getDisplayName() { return displayName; }
    public PlantFamily getFamily() { return family; }
    public String getCategory() { return family.getDisplayName(); }
    public List<String> getTags() { return tags; }
    public int getCost() { return cost; }
    public int getBaseHealth() { return baseHealth; }
    public String getDamage() { return Integer.toString(baseDamage); }
    public int getBaseDamage() { return baseDamage; }
    public boolean isInstantKill() { return baseDamage >= 99_999; }
    public double getAbilityPower() { return abilityPower; }
    public PlantAbility getAbility() { return ability; }
    public PlantFoodType getPlantFoodType() { return plantFoodType; }
    public double getPlantFoodPower() { return plantFoodPower; }
    public int getProjectileCount() { return projectileCount; }
    public List<PlantUpgrade> getUpgrades() { return upgrades; }

    /** Compatibility display method used by the collection screen. */
    public String getBaseAbility() {
        return ability + (abilityPower == 0.0 ? "" : " (power=" + format(abilityPower) + ")");
    }

    /** Compatibility display method used by the collection and greenhouse screens. */
    public String getPlantFoodEffect() {
        if (plantFoodType == PlantFoodType.NONE) {
            return "";
        }
        return plantFoodType + (plantFoodPower == 0.0
            ? "" : " (power=" + format(plantFoodPower) + ")");
    }

    /** Compatibility display method; gameplay reads typed upgrades through getUpgrades(). */
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
            + ", health=" + baseHealth + ", damage=" + baseDamage
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
                throw new IllegalArgumentException("Plant upgrades must use consecutive levels starting at 2.");
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

    private static String format(double value) {
        if (Math.rint(value) == value) {
            return Integer.toString((int) value);
        }
        return Double.toString(value);
    }
}
