package model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

public final class QuestFactory {
    private final LinkedHashMap<String, QuestDefinition> definitions;

    public QuestFactory(Collection<QuestDefinition> definitions) {
        this.definitions = new LinkedHashMap<>();
        if (definitions != null) {
            for (QuestDefinition definition : definitions) {
                registerQuest(definition);
            }
        }
    }

    public Quest createQuest(String title) {
        QuestDefinition definition = findDefinition(title)
            .orElseThrow(() -> new IllegalArgumentException("Unknown quest: " + title));
        return new Quest(definition);
    }

    public Quest createQuest(String title, int targetProgress) {
        QuestDefinition definition = findDefinition(title)
            .orElseThrow(() -> new IllegalArgumentException("Unknown quest: " + title));
        return new Quest(definition, targetProgress);
    }

    public Optional<QuestDefinition> findDefinition(String title) {
        return Optional.ofNullable(definitions.get(normalize(title)));
    }

    public void registerQuest(QuestDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Quest definition cannot be null.");
        }
        definitions.put(definition.getNormalizedTitle(), definition);
    }

    public List<QuestDefinition> getAllDefinitions() {
        return List.copyOf(definitions.values());
    }

    public List<QuestDefinition> getByCategory(String category) {
        ArrayList<QuestDefinition> result = new ArrayList<>();
        for (QuestDefinition definition : definitions.values()) {
            if (definition.isInCategory(category)) {
                result.add(definition);
            }
        }
        return List.copyOf(result);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        String trimmed = value.trim().toLowerCase(java.util.Locale.ROOT);
        for (int index = 0; index < trimmed.length(); index++) {
            char current = trimmed.charAt(index);
            if (!Character.isWhitespace(current) && current != '-' && current != '_') {
                result.append(current);
            }
        }
        return result.toString();
    }
}
