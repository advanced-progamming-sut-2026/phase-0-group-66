package model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class QuestDataLoader {
    public List<QuestDefinition> load(Path path) throws IOException {
        Object root = SimpleJsonParser.parse(path);
        List<Object> entries = requireList(root, "quest root");
        ArrayList<QuestDefinition> definitions = new ArrayList<>();
        for (int index = 0; index < entries.size(); index++) {
            try {
                definitions.add(parseDefinition(requireMap(entries.get(index), "quest entry")));
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid quest data at item " + (index + 1)
                    + ": " + exception.getMessage(), exception);
            }
        }
        return List.copyOf(definitions);
    }

    private QuestDefinition parseDefinition(Map<String, Object> entry) {
        Map<String, Object> reward = requireMap(entry.get("reward"), "reward");
        return new QuestDefinition(
            requireInt(entry.get("id"), "id"),
            requireString(entry.get("title"), "title"),
            enumValue(QuestCategory.class, entry.get("category"), "category"),
            requireString(entry.get("description"), "description"),
            enumValue(QuestEventType.class, entry.get("eventType"), "eventType"),
            requireInt(entry.get("target"), "target"),
            enumValue(RewardType.class, reward.get("type"), "reward.type"),
            requireInt(reward.get("amount"), "reward.amount"),
            enumValue(QuestPriority.class, entry.get("priority"), "priority"),
            optionalString(entry.get("parameter"))
        );
    }

    private <T extends Enum<T>> T enumValue(Class<T> type, Object value, String fieldName) {
        String text = requireString(value, fieldName);
        try {
            return Enum.valueOf(type, text.toUpperCase(java.util.Locale.ROOT));
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

    private String requireString(Object value, String fieldName) {
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must be a non-empty string.");
        }
        return text.trim();
    }

    private int requireInt(Object value, String fieldName) {
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(fieldName + " must be a number.");
        }
        return number.intValue();
    }

    private String optionalString(Object value) {
        return value instanceof String text ? text.trim() : "";
    }
}
