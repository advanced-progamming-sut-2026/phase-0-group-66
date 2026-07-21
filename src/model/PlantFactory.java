package model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PlantFactory {
    private final LinkedHashMap<String, PlantDefinition> definitions;

    public PlantFactory(Collection<PlantDefinition> definitions) {
        this.definitions = new LinkedHashMap<>();
        if (definitions != null) {
            for (PlantDefinition definition : definitions) {
                registerPlant(definition);
            }
        }
    }

    public Plant createPlant(String type) {
        return createPlant(type, 1);
    }

    public Plant createPlant(String type, int level) {
        PlantDefinition definition = findDefinition(type)
            .orElseThrow(() -> new IllegalArgumentException("Unknown plant type: " + type));
        String normalizedName = definition.getNormalizedName();
        if (isSunflower(normalizedName)) {
            return new Sunflower(definition, level);
        }
        if (isWallNut(normalizedName)) {
            return new WallNut(definition, level);
        }
        if (isPeaShooter(normalizedName)) {
            return new Peashooter(definition, level);
        }
        return new GenericPlant(definition, level);
    }

    public Optional<PlantDefinition> findDefinition(String type) {
        return Optional.ofNullable(definitions.get(PlantDefinition.normalizeKey(type)));
    }

    public void registerPlant(PlantDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Plant definition cannot be null.");
        }
        definitions.put(definition.getNormalizedName(), definition);
    }

    public List<String> getAllPlants() {
        ArrayList<String> names = new ArrayList<>();
        for (PlantDefinition definition : definitions.values()) {
            names.add(definition.getName());
        }
        return Collections.unmodifiableList(names);
    }

    public List<PlantDefinition> getAllDefinitions() {
        return List.copyOf(definitions.values());
    }

    public Map<String, PlantDefinition> getDefinitionMap() {
        return Collections.unmodifiableMap(definitions);
    }

    private boolean isSunflower(String normalizedName) {
        return normalizedName.equals("sunflower") || normalizedName.equals("twinsunflower")
            || normalizedName.equals("primalsunflower");
    }

    private boolean isWallNut(String normalizedName) {
        return normalizedName.equals("wallnut") || normalizedName.equals("tallnut")
            || normalizedName.equals("explodeonut");
    }

    private boolean isPeaShooter(String normalizedName) {
        return normalizedName.contains("peashooter") || normalizedName.equals("repeater")
            || normalizedName.equals("threepeater") || normalizedName.equals("splitpea")
            || normalizedName.equals("peapod") || normalizedName.equals("megagatlingpea")
            || normalizedName.equals("snowpea");
    }
}
