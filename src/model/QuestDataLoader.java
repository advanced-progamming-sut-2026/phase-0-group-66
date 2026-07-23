package model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Loads English quest data with nested condition and reward objects. */
public final class QuestDataLoader {
    public List<QuestDefinition> load(Path path) throws IOException {
        Object root = SimpleJsonParser.parse(path);
        List<Object> entries = requireList(root, "quest root");
        ArrayList<QuestDefinition> definitions = new ArrayList<>();
        Set<Integer> ids = new HashSet<>();
        Set<String> keys = new HashSet<>();
        Set<String> titles = new HashSet<>();
        for (int index = 0; index < entries.size(); index++) {
            try {
                QuestDefinition definition = parseDefinition(
                    requireMap(entries.get(index), "quest entry"));
                if (!ids.add(definition.getId())) {
                    throw new IllegalArgumentException("duplicate quest id: " + definition.getId());
                }
                if (!keys.add(definition.getNormalizedKey())) {
                    throw new IllegalArgumentException("duplicate quest key: " + definition.getKey());
                }
                if (!titles.add(definition.getNormalizedTitle())) {
                    throw new IllegalArgumentException("duplicate quest title: "
                        + definition.getTitle());
                }
                definitions.add(definition);
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid quest data at item " + (index + 1)
                    + ": " + exception.getMessage(), exception);
            }
        }
        return List.copyOf(definitions);
    }

    private QuestDefinition parseDefinition(Map<String, Object> entry) {
        Map<String, Object> condition = requireMap(entry.get("condition"), "condition");
        Map<String, Object> reward = requireMap(entry.get("reward"), "reward");
        QuestCondition questCondition = new QuestCondition(
            enumValue(QuestEventType.class, condition.get("event"), "condition.event"),
            requireInt(condition.get("target"), "condition.target"),
            optionalString(condition.get("qualifier"))
        );
        return new QuestDefinition(
            requireInt(entry.get("id"), "id"),
            requireString(entry.get("key"), "key"),
            requireString(entry.get("title"), "title"),
            enumValue(QuestCategory.class, entry.get("category"), "category"),
            requireString(entry.get("description"), "description"),
            questCondition,
            enumValue(RewardType.class, reward.get("kind"), "reward.kind"),
            requireInt(reward.get("amount"), "reward.amount"),
            enumValue(QuestPriority.class, entry.get("priority"), "priority")
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
