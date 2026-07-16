package model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ArmorDataLoader {
    public List<ArmorDefinition> load(Path path) throws IOException {
        Object root = SimpleJsonParser.parse(path);
        List<Object> entries = requireList(root, "armor root");
        ArrayList<ArmorDefinition> definitions = new ArrayList<>();
        for (int index = 0; index < entries.size(); index++) {
            try {
                definitions.add(parseDefinition(requireMap(entries.get(index), "armor entry")));
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid armor data at item " + (index + 1)
                    + ": " + exception.getMessage(), exception);
            }
        }
        return List.copyOf(definitions);
    }

    private ArmorDefinition parseDefinition(Map<String, Object> entry) {
        String alias = firstString(requireList(entry.get("aliases"), "aliases"));
        Map<String, Object> data = requireMap(entry.get("objdata"), "objdata");
        String armorType = requireString(data.get("ArmorType"), "ArmorType");
        int baseHealth = toInt(data.get("BaseHealth"), "BaseHealth");
        List<String> flags = toStringList(data.get("ArmorFlags"));
        List<Double> layers = toDoubleList(data.get("ArmorLayerHealth"));
        return new ArmorDefinition(alias, armorType, baseHealth, flags, layers);
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

    private String firstString(List<Object> values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("aliases cannot be empty.");
        }
        return requireString(values.get(0), "alias");
    }

    private String requireString(Object value, String fieldName) {
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must be a non-empty string.");
        }
        return text.trim();
    }

    private int toInt(Object value, String fieldName) {
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(fieldName + " must be numeric.");
        }
        return number.intValue();
    }

    private List<String> toStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        ArrayList<String> result = new ArrayList<>();
        for (Object item : requireList(value, "string list")) {
            result.add(requireString(item, "list item"));
        }
        return result;
    }

    private List<Double> toDoubleList(Object value) {
        if (value == null) {
            return List.of();
        }
        ArrayList<Double> result = new ArrayList<>();
        for (Object item : requireList(value, "number list")) {
            if (!(item instanceof Number number)) {
                throw new IllegalArgumentException("Number list contains a non-number.");
            }
            result.add(number.doubleValue());
        }
        return result;
    }
}
