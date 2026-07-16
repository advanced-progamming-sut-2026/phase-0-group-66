package model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ZombieFactory {
    private final LinkedHashMap<String, ZombieDefinition> definitions;
    private final ArmorFactory armorFactory;

    public ZombieFactory(Collection<ZombieDefinition> definitions, ArmorFactory armorFactory) {
        if (armorFactory == null) {
            throw new IllegalArgumentException("Armor factory cannot be null.");
        }
        this.armorFactory = armorFactory;
        this.definitions = new LinkedHashMap<>();
        if (definitions != null) {
            for (ZombieDefinition definition : definitions) {
                registerZombie(definition);
            }
        }
    }

    public Zombie createZombie(String type) {
        ZombieDefinition definition = findDefinition(type)
            .orElseThrow(() -> new IllegalArgumentException("Unknown zombie type: " + type));
        List<Armor> armors = armorFactory.createArmors(definition.getArmorAliases());
        return createSpecializedZombie(definition, armors);
    }

    public Optional<ZombieDefinition> findDefinition(String type) {
        String key = PlantDefinition.normalizeKey(type);
        ZombieDefinition byAlias = definitions.get(key);
        if (byAlias != null) {
            return Optional.of(byAlias);
        }
        for (ZombieDefinition definition : definitions.values()) {
            if (PlantDefinition.normalizeKey(definition.getDisplayName()).equals(key)) {
                return Optional.of(definition);
            }
        }
        return Optional.empty();
    }

    public void registerZombie(ZombieDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Zombie definition cannot be null.");
        }
        definitions.put(definition.getNormalizedAlias(), definition);
    }

    public List<String> getAllZombies() {
        ArrayList<String> names = new ArrayList<>();
        for (ZombieDefinition definition : definitions.values()) {
            names.add(definition.getDisplayName());
        }
        return Collections.unmodifiableList(names);
    }

    public List<ZombieDefinition> getAllDefinitions() {
        return List.copyOf(definitions.values());
    }

    public Map<String, ZombieDefinition> getDefinitionMap() {
        return Collections.unmodifiableMap(definitions);
    }

    private Zombie createSpecializedZombie(ZombieDefinition definition, List<Armor> armors) {
        return switch (definition.getAlias()) {
            case "ZombieDefault" -> new BasicZombie(definition, armors);
            case "ZombieArmor1" -> new ConeheadZombie(definition, armors);
            case "ZombieGargantuar" -> new Gargantuar(definition, armors);
            default -> new GenericZombie(definition, armors);
        };
    }
}
