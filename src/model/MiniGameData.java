package model;

import java.io.IOException;

final class MiniGameData {
    private static PlantFactory plantFactory;
    private static ZombieFactory zombieFactory;

    private MiniGameData() { }

    static synchronized PlantFactory plantFactory() {
        ensureLoaded();
        return plantFactory;
    }

    static synchronized ZombieFactory zombieFactory() {
        ensureLoaded();
        return zombieFactory;
    }

    private static void ensureLoaded() {
        if (plantFactory != null && zombieFactory != null) {
            return;
        }
        try {
            plantFactory = new PlantFactory(
                new PlantDataLoader().load(DataFileLocator.locate("plants.json")));
            ArmorFactory armorFactory = new ArmorFactory(
                new ArmorDataLoader().load(DataFileLocator.locate("armor-types.json")));
            zombieFactory = new ZombieFactory(
                new ZombieDataLoader().load(DataFileLocator.locate("zombies.json")),
                armorFactory);
        } catch (IOException exception) {
            throw new IllegalStateException("Mini-game data catalogs could not be loaded: "
                + exception.getMessage(), exception);
        }
    }
}
