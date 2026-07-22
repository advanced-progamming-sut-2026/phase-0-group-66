package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

final class BattleRuleSystem {
    private BattleRuleSystem() { }

    static void removeUnsupportedWaterPlants(Game engine) {
        for (int row = 0; row < engine.board.getRows(); row++) {
            for (int col = 0; col < engine.board.getCols(); col++) {
                Tile tile = engine.board.getTile(row, col);
                boolean water = tile.getType() == TileType.WATER
                    || tile.getType() == TileType.LOW_TIDE;
                Plant main = tile.getMainPlant();
                if (water && main != null && !main.getDefinition().hasTag("Water")
                    && tile.getSupportPlant() == null) {
                    main.takeDamage(Math.max(main.getHealth(), 1));
                }
            }
        }
    }
    static void releaseWizardTransformations(Game engine, String wizardId) {
        for (Plant plant : engine.board.getPlants()) {
            plant.releaseWizardTransformation(wizardId);
        }
    }
    static void dropStolenSunFromZombie(Game engine, Zombie zombie) {
        int stolen = zombie.takeStolenSun();
        if (stolen <= 0) {
            return;
        }
        int returned = zombie.getAbility() == ZombieAbility.TURQUOISE_SKULL
            ? stolen / 2 : stolen;
        engine.sunAmount += returned;
        engine.addEvent(zombie.getName() + " dropped " + returned + " stolen sun.");
    }
    static void removeInstantPlant(Game engine, Plant plant) {
        plant.takeDamage(Math.max(plant.getHealth(), 1));
    }
    static void activateMint(Game engine, Plant mint) {
        int affected = 0;
        for (Plant plant : new ArrayList<>(engine.board.getPlants())) {
            if (plant != mint && engine.plantMatchesMint(plant, mint.getAbility())) {
                engine.activatePlantFood(plant, mint.getName());
                affected++;
            }
        }
        engine.removeInstantPlant(mint);
        engine.addEvent(mint.getName() + " empowered " + affected + " related plant(s).");
    }
    static boolean plantMatchesMint(Game engine, Plant plant, PlantAbility mint) {
        return switch (mint) {
            case ENLIGHTEN_MINT -> plant.isSunProducer();
            case APPEASE_MINT -> plant.getDefinition().getCategory().equalsIgnoreCase("Shooter");
            case ARMA_MINT -> plant.isLobber();
            case BOMBARD_MINT -> plant.isExplosive();
            case ENFORCE_MINT -> plant.isMelee();
            case REINFORCE_MINT -> plant.getDefinition().getCategory().equalsIgnoreCase("Wall-nut");
            case ENCHANT_MINT -> plant.getDefinition().getCategory().equalsIgnoreCase("Modifier");
            case PIERCE_MINT -> plant.getDefinition().getCategory().equalsIgnoreCase("Strike-through");
            case CATTAIL_MINT -> plant.isHoming();
            default -> false;
        };
    }
    static void freezeAllZombies(Game engine, Plant source, boolean killWeak) {
        int affected = 0;
        for (Zombie zombie : engine.hostileZombies()) {
            if (killWeak && zombie.getEffectiveHealth() <= source.getEffectiveAttackPower() * 5) {
                zombie.kill(source.getName());
            } else {
                zombie.stun(5 * Game.TICKS_PER_SECOND);
                zombie.chill(10 * Game.TICKS_PER_SECOND);
            }
            affected++;
        }
        engine.addEvent(source.getName() + " froze " + affected + " zombie(s).");
    }
    static boolean explosionHits(Game engine, PlantAbility ability, int rowDistance,
                                      double columnDistance) {
        return switch (ability) {
            case JALAPENO -> rowDistance == 0;
            case DOOM_SHROOM -> true;
            case POTATO_MINE, SQUASH, TANGLE_KELP -> rowDistance == 0
                && columnDistance <= 0.9;
            default -> rowDistance <= 1 && columnDistance <= 1.5;
        };
    }
    static void launchGrapeshotFragments(Game engine, Plant plant, int multiplier) {
        ArrayList<Zombie> targets = engine.hostileZombies();
        int fragments = Math.min(8, targets.size());
        for (int index = 0; index < fragments; index++) {
            Zombie target = targets.get(engine.random.nextInt(targets.size()));
            target.takeDamage(100 * Math.max(1, multiplier), plant.getName());
        }
        engine.addEvent("Grapeshot launched " + fragments + " bouncing fragment(s).");
    }
    static int calculateNextSkySunTick(Game engine) {
        return engine.calculateSkySunIntervalTicks();
    }
    static int calculateSkySunIntervalTicks(Game engine) {
        double seconds = engine.elapsedTicks / (double) Game.TICKS_PER_SECOND;
        double baseInterval = Math.min(12.0, 6.0 + 0.05 * seconds);
        double intervalSeconds = baseInterval * engine.difficultyLevel / 3.0;
        return Math.max(1, (int) Math.round(intervalSeconds * Game.TICKS_PER_SECOND));
    }
    static void addForcedPlantSelections(Game engine) {
        if (engine.currentLevel == null) {
            return;
        }
        for (String plantName : engine.currentLevel.getForcedPlants()) {
            PlantDefinition definition = engine.plantFactory.findDefinition(plantName).orElse(null);
            if (definition != null && engine.selectedPlants.size() < engine.currentLevel.getAllowedPlantCount()) {
                engine.selectedPlants.add(definition.getName());
                engine.cooldownTicks.put(definition.getNormalizedName(), 0);
            }
        }
    }
    static void validatePlantSelectionRule(Game engine, PlantDefinition definition) {
        if (engine.currentLevel == null) {
            return;
        }
        if (engine.containsNormalized(engine.currentLevel.getLockedPlants(), definition.getName())) {
            throw new IllegalStateException("This plant is locked in the current level.");
        }
        for (String family : engine.currentLevel.getBannedPlantFamilies()) {
            if (engine.matchesPlantFamily(definition, family)) {
                throw new IllegalStateException("The " + family
                    + " plant family is locked in this level.");
            }
        }
        for (Map.Entry<String, String> entry
            : engine.currentLevel.getFamilyRepresentativePlants().entrySet()) {
            if (engine.matchesPlantFamily(definition, entry.getKey())
                && !definition.getName().equalsIgnoreCase(entry.getValue())) {
                throw new IllegalStateException("Only " + entry.getValue()
                    + " is available from the " + entry.getKey() + " family.");
            }
        }
        if (engine.currentLevel.getSpecialType() == SpecialLevelType.PLANT_WHAT_YOU_GET
            && engine.isSunProducerDefinition(definition)) {
            throw new IllegalStateException("Sun-producing plants are unavailable in this level.");
        }
    }
    static boolean matchesPlantFamily(Game engine, PlantDefinition definition, String family) {
        if (definition.getCategory().equalsIgnoreCase(family) || definition.hasTag(family)) {
            return true;
        }
        String normalizedFamily = PlantDefinition.normalizeKey(family);
        return normalizedFamily.equals("mint")
            && (PlantAbility.fromDefinition(definition).isMint()
            || definition.getNormalizedName().endsWith("mint"));
    }
    static boolean containsNormalized(Game engine, List<String> values, String expected) {
        String normalized = PlantDefinition.normalizeKey(expected);
        for (String value : values) {
            if (PlantDefinition.normalizeKey(value).equals(normalized)) {
                return true;
            }
        }
        return false;
    }
    static boolean isSunProducerDefinition(Game engine, PlantDefinition definition) {
        return definition.getCategory().equalsIgnoreCase("Sun Producer")
            || definition.getCategory().equalsIgnoreCase("SunProducer")
            || definition.hasTag("Sun");
    }
    static boolean isPreWaveSetup(Game engine) {
        return engine.currentLevel != null
            && engine.currentLevel.getSpecialType() == SpecialLevelType.PLANT_WHAT_YOU_GET
            && !engine.zombieWavesStarted;
    }
    static void recordPlantUsage(Game engine, Plant plant) {
        if (plant == null) {
            return;
        }
        engine.plantedPlantNames.add(plant.getName());
        engine.plantedPlantFamilies.add(plant.getDefinition().getCategory());
        if (plant.isSunProducer()) {
            engine.sunProducerPlantsPlanted++;
        }
    }
    static void autoSelectStarterPlants(Game engine) {
        for (String starter : List.of("Sunflower", "Peashooter", "Wall-nut")) {
            if (engine.plantFactory.findDefinition(starter).isPresent()) {
                engine.selectPlant(starter);
            }
        }
    }
    static void requirePlantSelection(Game engine) {
        if (engine.currentLevel == null || engine.gameState != GameState.PLANT_SELECTION) {
            throw new IllegalStateException("A level must be prepared for plant selection.");
        }
    }
    static void requireRunning(Game engine) {
        if (engine.gameState != GameState.RUNNING || engine.board == null) {
            throw new IllegalStateException("No game is currently running.");
        }
    }
    static void addEvent(Game engine, String event) {
        engine.events.add(event);
    }
    static String display(Game engine, int row, int col) {
        return "(" + (col + 1) + ", " + (row + 1) + ")";
    }
    static String formatSeconds(Game engine, int ticks) {
        return String.format("%.1f", ticks / (double) Game.TICKS_PER_SECOND);
    }
    static String formatColumn(Game engine, double column) {
        return String.format("%.2f", column + 1);
    }
}
