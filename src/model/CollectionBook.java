package model;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class CollectionBook implements Serializable {
    private static final long serialVersionUID = 1L;

    private final LinkedHashSet<String> ownedPlants;
    private final LinkedHashSet<String> seenZombies;
    private final LinkedHashMap<String, Integer> plantLevels;

    public CollectionBook() {
        ownedPlants = new LinkedHashSet<>();
        seenZombies = new LinkedHashSet<>();
        plantLevels = new LinkedHashMap<>();
    }

    public void unlockPlant(String plantName) {
        if (plantName != null && !plantName.isBlank()) {
            ownedPlants.add(plantName);
            plantLevels.putIfAbsent(plantName, 1);
        }
    }

    public boolean unlockZombie(String zombieName) {
        return zombieName != null && !zombieName.isBlank() && seenZombies.add(zombieName);
    }

    public void upgradePlant(String plantName) {
        if (ownedPlants.contains(plantName)) {
            plantLevels.merge(plantName, 1, Integer::sum);
        }
    }

    public Set<String> getOwnedPlants() {
        return Collections.unmodifiableSet(ownedPlants);
    }

    public Set<String> getSeenZombies() {
        return Collections.unmodifiableSet(seenZombies);
    }

    public int getPlantLevel(String plantName) {
        return plantLevels.getOrDefault(plantName, 0);
    }

    public Map<String, Integer> getPlantLevels() {
        return Collections.unmodifiableMap(plantLevels);
    }
}
