package model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Creates runtime plants from structured definitions rather than display-name checks. */
public final class PlantFactory {
    private final LinkedHashMap<String, PlantDefinition> definitionsByLookup;
    private final ArrayList<PlantDefinition> orderedDefinitions;

    public PlantFactory(Collection<PlantDefinition> definitions) {
        definitionsByLookup = new LinkedHashMap<>();
        orderedDefinitions = new ArrayList<>();
        if (definitions != null) {
            for (PlantDefinition definition : definitions) {
                registerPlant(definition);
            }
        }
    }

    public Plant createPlant(String type) { return createPlant(type, 1); }

    public Plant createPlant(String type, int level) {
        PlantDefinition definition = findDefinition(type)
            .orElseThrow(() -> new IllegalArgumentException("Unknown plant type: " + type));
        if (definition.getFamily() == PlantFamily.SUN_PRODUCER
            && definition.getAbility() != PlantAbility.GOLD_BLOOM
            && !definition.getAbility().isMint()) {
            return new Sunflower(definition, level);
        }
        if (definition.getFamily() == PlantFamily.WALL_NUT) {
            return new WallNut(definition, level);
        }
        if (definition.getFamily() == PlantFamily.SHOOTER) {
            return new Peashooter(definition, level);
        }
        return new GenericPlant(definition, level);
    }

    public Optional<PlantDefinition> findDefinition(String type) {
        return Optional.ofNullable(definitionsByLookup.get(PlantDefinition.normalizeKey(type)));
    }

    public void registerPlant(PlantDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Plant definition cannot be null.");
        }
        String nameKey = definition.getNormalizedName();
        String dataKey = definition.getNormalizedKey();
        PlantDefinition existingName = definitionsByLookup.get(nameKey);
        PlantDefinition existingKey = definitionsByLookup.get(dataKey);
        if ((existingName != null && existingName != definition)
            || (existingKey != null && existingKey != definition)) {
            throw new IllegalArgumentException("Duplicate plant lookup key: " + definition.getKey());
        }
        orderedDefinitions.add(definition);
        definitionsByLookup.put(nameKey, definition);
        definitionsByLookup.put(dataKey, definition);
    }

    public List<String> getAllPlants() {
        ArrayList<String> names = new ArrayList<>();
        for (PlantDefinition definition : orderedDefinitions) {
            names.add(definition.getName());
        }
        return Collections.unmodifiableList(names);
    }

    public List<PlantDefinition> getAllDefinitions() { return List.copyOf(orderedDefinitions); }

    public Map<String, PlantDefinition> getDefinitionMap() {
        LinkedHashMap<String, PlantDefinition> result = new LinkedHashMap<>();
        for (PlantDefinition definition : orderedDefinitions) {
            result.put(definition.getKey(), definition);
        }
        return Collections.unmodifiableMap(result);
    }
}
