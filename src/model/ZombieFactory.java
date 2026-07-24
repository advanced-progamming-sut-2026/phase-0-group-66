package model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ZombieFactory {
    private final LinkedHashMap<String, ZombieDefinition> definitionsByLookup;
    private final ArrayList<ZombieDefinition> orderedDefinitions;
    private final ArmorFactory armorFactory;

    public ZombieFactory(Collection<ZombieDefinition> definitions, ArmorFactory armorFactory) {
        if (armorFactory == null) {
            throw new IllegalArgumentException("Armor factory cannot be null.");
        }
        this.armorFactory = armorFactory;
        definitionsByLookup = new LinkedHashMap<>();
        orderedDefinitions = new ArrayList<>();
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
        return Optional.ofNullable(definitionsByLookup.get(PlantDefinition.normalizeKey(type)));
    }

    public void registerZombie(ZombieDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Zombie definition cannot be null.");
        }
        ArrayList<String> lookups = new ArrayList<>(List.of(definition.getKey(),
            definition.getAlias(), definition.getDisplayName()));
        lookups.addAll(definition.getLookupAliases());
        for (String lookup : lookups) {
            String normalized = PlantDefinition.normalizeKey(lookup);
            ZombieDefinition existing = definitionsByLookup.get(normalized);
            if (existing != null && existing != definition) {
                throw new IllegalArgumentException("Duplicate zombie lookup key: " + lookup);
            }
            definitionsByLookup.put(normalized, definition);
        }
        orderedDefinitions.add(definition);
    }

    public List<String> getAllZombies() {
        ArrayList<String> names = new ArrayList<>();
        for (ZombieDefinition definition : orderedDefinitions) {
            names.add(definition.getDisplayName());
        }
        return Collections.unmodifiableList(names);
    }

    public List<ZombieDefinition> getAllDefinitions() { return List.copyOf(orderedDefinitions); }

    public List<ZombieDefinition> getDefinitionsForSeason(SeasonType season) {
        SeasonType actualSeason = season == null ? SeasonType.ANCIENT_EGYPT : season;
        ArrayList<ZombieDefinition> result = new ArrayList<>();
        for (ZombieDefinition definition : orderedDefinitions) {
            if (definition.isAvailableIn(actualSeason)) {
                result.add(definition);
            }
        }
        return List.copyOf(result);
    }

    public Map<String, ZombieDefinition> getDefinitionMap() {
        LinkedHashMap<String, ZombieDefinition> result = new LinkedHashMap<>();
        for (ZombieDefinition definition : orderedDefinitions) {
            result.put(definition.getKey(), definition);
        }
        return Collections.unmodifiableMap(result);
    }

    private Zombie createSpecializedZombie(ZombieDefinition definition, List<Armor> armors) {
        return switch (definition.getAbility()) {
            case BASIC -> new BasicZombie(definition, armors);
            case ARMORED -> new ConeheadZombie(definition, armors);
            case GARGANTUAR -> new Gargantuar(definition, armors);
            default -> new GenericZombie(definition, armors);
        };
    }
}
