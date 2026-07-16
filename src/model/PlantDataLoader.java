package model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlantDataLoader {
    public List<PlantDefinition> load(Path path) throws IOException {
        Object root = SimpleJsonParser.parse(path);
        List<Object> entries = requireList(root, "plant root");
        ArrayList<PlantDefinition> definitions = new ArrayList<>();
        for (int index = 0; index < entries.size(); index++) {
            try {
                definitions.add(parseDefinition(requireMap(entries.get(index), "plant entry")));
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid plant data at item " + (index + 1)
                        + ": " + exception.getMessage(), exception);
            }
        }
        validateUniqueNames(definitions, path);
        return List.copyOf(definitions);
    }

    private PlantDefinition parseDefinition(Map<String, Object> entry) {
        int id = toInt(entry.get("id"), "id");
        String name = requireString(entry.get("name"), "name");
        String category = requireString(entry.get("category"), "category");
        List<String> tags = toStringList(entry.get("tags"), "tags");
        int cost = toInt(entry.get("cost"), "cost");
        int baseHealth = toInt(entry.get("baseHealth"), "baseHealth");
        String damage = optionalString(entry.get("damage"));
        String baseAbility = optionalString(entry.get("baseAbility"));
        String plantFoodEffect = optionalString(entry.get("plantFoodEffect"));
        List<String> upgrades = toStringList(entry.get("levelUpgrades"), "levelUpgrades");
        Double actionInterval = toOptionalDouble(entry.get("actionIntervalSeconds"),
                "actionIntervalSeconds");
        Double recharge = toOptionalDouble(entry.get("rechargeSeconds"), "rechargeSeconds");
        return new PlantDefinition(id, name, category, tags, cost, baseHealth, damage,
                baseAbility, plantFoodEffect, upgrades, actionInterval, recharge);
    }

    private void validateUniqueNames(List<PlantDefinition> definitions, Path path) throws IOException {
        LinkedHashMap<String, String> names = new LinkedHashMap<>();
        for (PlantDefinition definition : definitions) {
            String previous = names.put(definition.getNormalizedName(), definition.getName());
            if (previous != null) {
                throw new IOException("Duplicate plant name in " + path + ": " + definition.getName());
            }
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

    private String requireString(Object value, String fieldName) {
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must be a non-empty string.");
        }
        return text.trim();
    }

    private String optionalString(Object value) {
        return value instanceof String text ? text.trim() : "";
    }

    private int toInt(Object value, String fieldName) {
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(fieldName + " must be numeric.");
        }
        return number.intValue();
    }

    private Double toOptionalDouble(Object value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(fieldName + " must be numeric or null.");
        }
        return number.doubleValue();
    }

    private List<String> toStringList(Object value, String fieldName) {
        if (value == null) {
            return List.of();
        }
        ArrayList<String> result = new ArrayList<>();
        for (Object item : requireList(value, fieldName)) {
            result.add(requireString(item, fieldName + " item"));
        }
        return result;
    }
}
