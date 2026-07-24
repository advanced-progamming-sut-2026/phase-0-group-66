package model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Loads the structured, English-only plant schema. */
public final class PlantDataLoader {
    public List<PlantDefinition> load(Path path) throws IOException {
        Object root = SimpleJsonParser.parse(path);
        List<Object> entries = requireList(root, "plant root");
        ArrayList<PlantDefinition> definitions = new ArrayList<>();
        Set<Integer> ids = new HashSet<>();
        Set<String> keys = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (int index = 0; index < entries.size(); index++) {
            try {
                PlantDefinition definition = parseDefinition(
                    requireMap(entries.get(index), "plant entry"));
                if (!ids.add(definition.getId())) {
                    throw new IllegalArgumentException("duplicate plant id: " + definition.getId());
                }
                if (!keys.add(definition.getNormalizedKey())) {
                    throw new IllegalArgumentException("duplicate plant key: " + definition.getKey());
                }
                if (!names.add(definition.getNormalizedName())) {
                    throw new IllegalArgumentException("duplicate plant name: " + definition.getName());
                }
                definitions.add(definition);
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid plant data at item " + (index + 1)
                    + ": " + exception.getMessage(), exception);
            }
        }
        return List.copyOf(definitions);
    }

    private PlantDefinition parseDefinition(Map<String, Object> entry) {
        Map<String, Object> stats = requireMap(entry.get("stats"), "stats");
        Map<String, Object> ability = requireMap(entry.get("ability"), "ability");
        Map<String, Object> plantFood = requireMap(entry.get("plantFood"), "plantFood");
        return new PlantDefinition(
            requireInt(entry.get("id"), "id"),
            optionalBoolean(entry.get("required"), true),
            requireString(entry.get("key"), "key"),
            requireString(entry.get("displayName"), "displayName"),
            enumValue(PlantFamily.class, entry.get("family"), "family"),
            stringList(entry.get("tags"), "tags"),
            requireInt(stats.get("sunCost"), "stats.sunCost"),
            requireInt(stats.get("maxHealth"), "stats.maxHealth"),
            requireInt(stats.get("damage"), "stats.damage"),
            optionalString(stats.get("damageDisplay")),
            requireDouble(stats.get("actionInterval"), "stats.actionInterval"),
            requireDouble(stats.get("recharge"), "stats.recharge"),
            requireInt(stats.get("projectileCount"), "stats.projectileCount"),
            enumValue(PlantAbility.class, ability.get("kind"), "ability.kind"),
            optionalDouble(ability.get("power"), "ability.power"),
            optionalString(ability.get("description")),
            numberMap(ability.get("parameters"), "ability.parameters"),
            enumValue(PlantFoodType.class, plantFood.get("kind"), "plantFood.kind"),
            optionalDouble(plantFood.get("power"), "plantFood.power"),
            optionalString(plantFood.get("description")),
            numberMap(plantFood.get("parameters"), "plantFood.parameters"),
            parseUpgrades(entry.get("upgrades"))
        );
    }

    private Map<String, Double> numberMap(Object value, String fieldName) {
        if (value == null) {
            return Map.of();
        }
        Map<String, Object> input = requireMap(value, fieldName);
        java.util.LinkedHashMap<String, Double> result = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            result.put(entry.getKey(), requireDouble(entry.getValue(),
                fieldName + "." + entry.getKey()));
        }
        return Map.copyOf(result);
    }

    private List<PlantUpgrade> parseUpgrades(Object value) {
        ArrayList<PlantUpgrade> result = new ArrayList<>();
        for (Object item : requireList(value, "upgrades")) {
            Map<String, Object> upgrade = requireMap(item, "upgrade");
            result.add(new PlantUpgrade(
                requireInt(upgrade.get("level"), "upgrade.level"),
                enumValue(PlantUpgradeType.class, upgrade.get("effect"), "upgrade.effect"),
                optionalDouble(upgrade.get("amount"), "upgrade.amount"),
                optionalString(upgrade.get("trait"))
            ));
        }
        return List.copyOf(result);
    }

    private <T extends Enum<T>> T enumValue(Class<T> type, Object value, String fieldName) {
        String text = requireString(value, fieldName);
        try {
            return Enum.valueOf(type, text.toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(fieldName + " has an unknown value: " + text);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> requireMap(Object value, String fieldName) {
        if (!(value instanceof Map<?, ?>)) {
            throw new IllegalArgumentException(fieldName + " must be an object.");
        }
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Object> requireList(Object value, String fieldName) {
        if (!(value instanceof List<?>)) {
            throw new IllegalArgumentException(fieldName + " must be an array.");
        }
        return (List<Object>) value;
    }

    private List<String> stringList(Object value, String fieldName) {
        ArrayList<String> result = new ArrayList<>();
        for (Object item : requireList(value, fieldName)) {
            result.add(requireString(item, fieldName + " item"));
        }
        return List.copyOf(result);
    }

    private String requireString(Object value, String fieldName) {
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must be a non-empty string.");
        }
        return text.trim();
    }

    private String optionalString(Object value) {
        return value instanceof String text ? text.trim() : "";
    }

    private boolean optionalBoolean(Object value, boolean fallback) {
        if (value == null) {
            return fallback;
        }
        if (!(value instanceof Boolean booleanValue)) {
            throw new IllegalArgumentException("required must be boolean.");
        }
        return booleanValue;
    }

    private int requireInt(Object value, String fieldName) {
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(fieldName + " must be numeric.");
        }
        return number.intValue();
    }

    private double requireDouble(Object value, String fieldName) {
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(fieldName + " must be numeric.");
        }
        return number.doubleValue();
    }

    private double optionalDouble(Object value, String fieldName) {
        if (value == null) {
            return 0.0;
        }
        return requireDouble(value, fieldName);
    }
}
