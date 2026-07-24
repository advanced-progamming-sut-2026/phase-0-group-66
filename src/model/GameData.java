package model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class GameData {
    private final PlantFactory plantFactory;
    private final ArmorFactory armorFactory;
    private final ZombieFactory zombieFactory;
    private final QuestFactory questFactory;

    private GameData(PlantFactory plantFactory, ArmorFactory armorFactory,
                     ZombieFactory zombieFactory, QuestFactory questFactory) {
        this.plantFactory = plantFactory;
        this.armorFactory = armorFactory;
        this.zombieFactory = zombieFactory;
        this.questFactory = questFactory;
    }

    public static GameData loadDefault() throws IOException {
        Path plantsPath = DataFileLocator.locate("plants.json");
        Path armorPath = DataFileLocator.locate("armor-types.json");
        Path zombiesPath = DataFileLocator.locate("zombies.json");
        Path questsPath = DataFileLocator.locate("quests.json");

        List<PlantDefinition> plants = new PlantDataLoader().load(plantsPath);
        PlantCatalogValidator.validate(plants);
        List<ArmorDefinition> armors = new ArmorDataLoader().load(armorPath);
        List<ZombieDefinition> zombies = new ZombieDataLoader().load(zombiesPath);
        List<QuestDefinition> quests = new QuestDataLoader().load(questsPath);

        ArmorFactory armorFactory = new ArmorFactory(armors);
        return new GameData(
            new PlantFactory(plants),
            armorFactory,
            new ZombieFactory(zombies, armorFactory),
            new QuestFactory(quests)
        );
    }

    public PlantFactory getPlantFactory() {
        return plantFactory;
    }

    public ArmorFactory getArmorFactory() {
        return armorFactory;
    }

    public ZombieFactory getZombieFactory() {
        return zombieFactory;
    }

    public QuestFactory getQuestFactory() {
        return questFactory;
    }

    public String summary() {
        return "Loaded " + plantFactory.getAllDefinitions().size() + " plants, "
            + zombieFactory.getAllDefinitions().size() + " zombies, "
            + armorFactory.getDefinitions().size() + " armor types and "
            + questFactory.getAllDefinitions().size() + " quests.";
    }
}
