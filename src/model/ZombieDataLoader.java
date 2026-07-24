package model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ZombieDataLoader {
    public List<ZombieDefinition> load(Path path) throws IOException {
        Object root = SimpleJsonParser.parse(path);
        List<Object> entries = requireList(root, "zombie root");
        ArrayList<ZombieDefinition> definitions = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        Set<String> aliases = new HashSet<>();
        for (int index = 0; index < entries.size(); index++) {
            try {
                ZombieDefinition definition = parseDefinition(
                    requireMap(entries.get(index), "zombie entry"));
                if (!keys.add(definition.getNormalizedKey())) {
                    throw new IllegalArgumentException("duplicate zombie key: " + definition.getKey());
                }
                if (!aliases.add(definition.getNormalizedAlias())) {
                    throw new IllegalArgumentException("duplicate zombie alias: "
                        + definition.getAlias());
                }
                definitions.add(definition);
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid zombie data at item " + (index + 1)
                    + ": " + exception.getMessage(), exception);
            }
        }
        return List.copyOf(definitions);
    }

    private ZombieDefinition parseDefinition(Map<String, Object> entry) {
        Map<String, Object> stats = requireMap(entry.get("stats"), "stats");
        return new ZombieDefinition(
            requireString(entry.get("key"), "key"),
            requireString(entry.get("alias"), "alias"),
            requireString(entry.get("displayName"), "displayName"),
            requireInt(stats.get("health"), "stats.health"),
            requireInt(stats.get("eatDamagePerSecond"), "stats.eatDamagePerSecond"),
            requireDouble(stats.get("speed"), "stats.speed"),
            requireInt(stats.get("waveCost"), "stats.waveCost"),
            requireInt(stats.get("selectionWeight"), "stats.selectionWeight"),
            requireBoolean(entry.get("canDropPlantFood"), "canDropPlantFood"),
            enumValue(ZombieAbility.class, entry.get("ability"), "ability"),
            enumList(SeasonType.class, entry.get("seasons"), "seasons"),
            stringList(entry.get("armors"), "armors"),
            optionalStringList(entry.get("lookupAliases"), "lookupAliases"),
            requireMap(entry.get("properties"), "properties")
        );
    }

    private <T extends Enum<T>> T enumValue(Class<T> type, Object value, String fieldName) {
        String text = requireString(value, fieldName);
        try {
            return Enum.valueOf(type, text.toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(fieldName + " has an unknown value: " + text);
        }
    }

    private <T extends Enum<T>> List<T> enumList(Class<T> type, Object value, String fieldName) {
        ArrayList<T> result = new ArrayList<>();
        for (Object item : requireList(value, fieldName)) {
            result.add(enumValue(type, item, fieldName + " item"));
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
        return List.copyOf(result);
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

    private List<String> optionalStringList(Object value, String fieldName) {
        if (value == null) {
            return List.of();
        }
        return stringList(value, fieldName);
    }

    private String requireString(Object value, String fieldName) {
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must be a non-empty string.");
        }
        return text.trim();
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

    private boolean requireBoolean(Object value, String fieldName) {
        if (!(value instanceof Boolean bool)) {
            throw new IllegalArgumentException(fieldName + " must be boolean.");
        }
        return bool;
    }
}
