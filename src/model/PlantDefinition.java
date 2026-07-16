package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlantDefinition {
    private static final Pattern FIRST_NUMBER = Pattern.compile("-?\\d+(?:\\.\\d+)?");

    private final int id;
    private final String name;
    private final String category;
    private final List<String> tags;
    private final int cost;
    private final int baseHealth;
    private final String damage;
    private final String baseAbility;
    private final String plantFoodEffect;
    private final List<String> levelUpgrades;
    private final Double actionIntervalSeconds;
    private final Double rechargeSeconds;

    public PlantDefinition(int id, String name, String category, List<String> tags, int cost,
                           int baseHealth, String damage, String baseAbility,
                           String plantFoodEffect, List<String> levelUpgrades,
                           Double actionIntervalSeconds, Double rechargeSeconds) {
        this.id = id;
        this.name = requireText(name, "Plant name");
        this.category = requireText(category, "Plant category");
        this.tags = immutableCleanList(tags);
        this.cost = requireNonNegative(cost, "Plant cost");
        this.baseHealth = requireNonNegative(baseHealth, "Plant health");
        this.damage = damage == null ? "0" : damage.trim();
        this.baseAbility = baseAbility == null ? "" : baseAbility.trim();
        this.plantFoodEffect = plantFoodEffect == null ? "" : plantFoodEffect.trim();
        this.levelUpgrades = immutableCleanList(levelUpgrades);
        this.actionIntervalSeconds = actionIntervalSeconds;
        this.rechargeSeconds = rechargeSeconds;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public List<String> getTags() {
        return tags;
    }

    public int getCost() {
        return cost;
    }

    public int getBaseHealth() {
        return baseHealth;
    }

    public String getDamage() {
        return damage;
    }

    public int getBaseDamage() {
        Matcher matcher = FIRST_NUMBER.matcher(damage);
        if (!matcher.find()) {
            return 0;
        }
        return (int) Math.round(Double.parseDouble(matcher.group()));
    }

    public boolean isInstantKill() {
        return damage.toLowerCase(Locale.ROOT).contains("insta-kill");
    }

    public String getBaseAbility() {
        return baseAbility;
    }

    public String getPlantFoodEffect() {
        return plantFoodEffect;
    }

    public List<String> getLevelUpgrades() {
        return levelUpgrades;
    }

    public OptionalDouble getActionIntervalSeconds() {
        return optional(actionIntervalSeconds);
    }

    public OptionalDouble getRechargeSeconds() {
        return optional(rechargeSeconds);
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

    public String getNormalizedName() {
        return normalizeKey(name);
    }

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
        return name + " [category=" + category + ", cost=" + cost
            + ", health=" + baseHealth + ", damage=" + damage + "]";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PlantDefinition definition)) {
            return false;
        }
        return id == definition.id && name.equals(definition.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    private static List<String> immutableCleanList(List<String> input) {
        ArrayList<String> result = new ArrayList<>();
        if (input != null) {
            for (String value : input) {
                if (value != null && !value.isBlank() && !value.trim().equals("-")) {
                    result.add(value.trim());
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

    private static OptionalDouble optional(Double value) {
        return value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
    }
}
