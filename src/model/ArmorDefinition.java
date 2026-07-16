package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ArmorDefinition {
    private final String alias;
    private final String armorType;
    private final int baseHealth;
    private final List<String> flags;
    private final List<Double> layerHealthFractions;

    public ArmorDefinition(String alias, String armorType, int baseHealth, List<String> flags,
                           List<Double> layerHealthFractions) {
        this.alias = requireText(alias, "Armor alias");
        this.armorType = requireText(armorType, "Armor type");
        if (baseHealth < 0) {
            throw new IllegalArgumentException("Armor health cannot be negative.");
        }
        this.baseHealth = baseHealth;
        this.flags = immutableStrings(flags);
        this.layerHealthFractions = immutableDoubles(layerHealthFractions);
    }

    public String getAlias() {
        return alias;
    }

    public String getArmorType() {
        return armorType;
    }

    public int getBaseHealth() {
        return baseHealth;
    }

    public List<String> getFlags() {
        return flags;
    }

    public List<Double> getLayerHealthFractions() {
        return layerHealthFractions;
    }

    public boolean hasFlag(String flag) {
        if (flag == null) {
            return false;
        }
        for (String current : flags) {
            if (current.equalsIgnoreCase(flag.trim())) {
                return true;
            }
        }
        return false;
    }

    public String getNormalizedAlias() {
        return PlantDefinition.normalizeKey(alias);
    }

    @Override
    public String toString() {
        return armorType + " [health=" + baseHealth + ", flags=" + flags + "]";
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
        return value.trim();
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

    private static List<Double> immutableDoubles(List<Double> values) {
        ArrayList<Double> result = new ArrayList<>();
        if (values != null) {
            result.addAll(values);
        }
        return Collections.unmodifiableList(result);
    }
}
