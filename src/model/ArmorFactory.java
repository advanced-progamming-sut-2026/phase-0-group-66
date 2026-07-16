package model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ArmorFactory {
    private final LinkedHashMap<String, ArmorDefinition> definitions;

    public ArmorFactory(Collection<ArmorDefinition> definitions) {
        this.definitions = new LinkedHashMap<>();
        if (definitions != null) {
            for (ArmorDefinition definition : definitions) {
                registerArmor(definition);
            }
        }
    }

    public Armor createArmor(String aliasOrType) {
        ArmorDefinition definition = findDefinition(aliasOrType)
            .orElseThrow(() -> new IllegalArgumentException("Unknown armor type: " + aliasOrType));
        return new Armor(definition);
    }

    public List<Armor> createArmors(List<String> aliases) {
        ArrayList<Armor> armors = new ArrayList<>();
        if (aliases != null) {
            for (String alias : aliases) {
                armors.add(createArmor(alias));
            }
        }
        return armors;
    }

    public Optional<ArmorDefinition> findDefinition(String aliasOrType) {
        String key = PlantDefinition.normalizeKey(aliasOrType);
        ArmorDefinition byAlias = definitions.get(key);
        if (byAlias != null) {
            return Optional.of(byAlias);
        }
        for (ArmorDefinition definition : definitions.values()) {
            if (PlantDefinition.normalizeKey(definition.getArmorType()).equals(key)) {
                return Optional.of(definition);
            }
        }
        return Optional.empty();
    }

    public void registerArmor(ArmorDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Armor definition cannot be null.");
        }
        definitions.put(definition.getNormalizedAlias(), definition);
    }

    public Map<String, ArmorDefinition> getDefinitions() {
        return Collections.unmodifiableMap(definitions);
    }
}
