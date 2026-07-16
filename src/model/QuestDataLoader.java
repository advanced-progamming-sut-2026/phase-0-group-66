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
        return new QuestDefinition(
            requireString(entry.get("title"), "title"),
            requireString(entry.get("category"), "category"),
            requireString(entry.get("completionCondition"), "completionCondition"),
            requireString(entry.get("rewardDescription"), "rewardDescription"),
            requireString(entry.get("priority"), "priority"),
            optionalString(entry.get("variables"))
        );
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
}
