package model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class QuestFactory {
    private final LinkedHashMap<Integer, QuestDefinition> byId = new LinkedHashMap<>();
    private final LinkedHashMap<String, QuestDefinition> byTitle = new LinkedHashMap<>();

    public QuestFactory(Collection<QuestDefinition> definitions) {
        if (definitions != null) {
            definitions.forEach(this::registerQuest);
        }
    }

    public void registerQuest(QuestDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Quest definition cannot be null.");
        }
        if (byId.containsKey(definition.getId())) {
            throw new IllegalArgumentException("Duplicate quest id: " + definition.getId());
        }
        if (byTitle.containsKey(definition.getNormalizedTitle())) {
            throw new IllegalArgumentException("Duplicate quest title: " + definition.getTitle());
        }
        byId.put(definition.getId(), definition);
        byTitle.put(definition.getNormalizedTitle(), definition);
    }

    public Optional<QuestDefinition> findDefinition(int id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Optional<QuestDefinition> findDefinition(String title) {
        return Optional.ofNullable(byTitle.get(normalize(title)));
    }

    public List<QuestDefinition> getAllDefinitions() {
        return List.copyOf(byId.values());
    }

    public List<QuestDefinition> getByCategory(QuestCategory category) {
        ArrayList<QuestDefinition> result = new ArrayList<>();
        for (QuestDefinition definition : byId.values()) {
            if (definition.getCategory() == category) {
                result.add(definition);
            }
        }
        return List.copyOf(result);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
            .replace(" ", "").replace("-", "").replace("_", "");
    }
}
