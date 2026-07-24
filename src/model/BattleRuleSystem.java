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
        int baseDuration = mint.getDefinition().getAbilityParameterInt("durationSeconds", 3);
        int duration = Math.max(1, baseDuration
            + mint.getUpgradeTraitInt("DURATION_1S", 0));
        mint.startMintAura(duration * Game.TICKS_PER_SECOND);
        int affected = empowerMintFamily(engine, mint);
        resetMintFamilyCooldowns(engine, mint);
        engine.addEvent(mint.getName() + " started a " + duration
            + " second aura and empowered " + affected + " related plant(s).");
    }

    static int empowerMintFamily(Game engine, Plant mint) {
        if (!mint.isMintAuraActive()) {
            return 0;
        }
        int affected = 0;
        for (Plant plant : new ArrayList<>(engine.board.getPlants())) {
            if (plant == mint || plant.isDestroyed()
                || !engine.plantMatchesMint(plant, mint.getAbility())
                || !mint.markMintEmpowered(plant)) {
                continue;
            }
            if (plant.getDefinition().getPlantFoodType() != PlantFoodType.NONE) {
                engine.activatePlantFood(plant, mint.getName());
            }
            affected++;
        }
        return affected;
    }

    private static void resetMintFamilyCooldowns(Game engine, Plant mint) {
        if (!mint.hasUpgradeTrait("RESET_FAMILY_COOLDOWNS")) {
            return;
        }
        int reset = 0;
        for (PlantDefinition definition : engine.plantFactory.getAllDefinitions()) {
            Plant sample = engine.plantFactory.createPlant(definition.getName());
            if (engine.plantMatchesMint(sample, mint.getAbility())) {
                String key = definition.getNormalizedName();
                if (engine.cooldownTicks.containsKey(key)) {
                    engine.cooldownTicks.put(key, 0);
                    reset++;
                }
            }
        }
        engine.addEvent(mint.getName() + " reset " + reset
            + " related plant cooldown(s).");
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
        int freezeTicks = source.getDefinition().getAbilityParameterInt("freezeSeconds", 5)
            * Game.TICKS_PER_SECOND + source.getChillBonusTicks();
        int chillTicks = source.getDefinition().getAbilityParameterInt("chillSeconds", 10)
            * Game.TICKS_PER_SECOND;
        int affected = 0;
        for (Zombie zombie : engine.hostileZombies()) {
            if (killWeak && zombie.getEffectiveHealth() <= source.getEffectiveAttackPower() * 5) {
                zombie.kill(source.getName());
            } else {
                zombie.stun(freezeTicks);
                zombie.chill(chillTicks);
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
        GridPosition center = plant.getPosition();
        int count = plant.getDefinition().getAbilityParameterInt("fragmentCount", 8);
        int baseDamage = plant.getDefinition().getAbilityParameterInt("fragmentDamage", 100);
        int lifetimeSeconds = plant.getDefinition().getAbilityParameterInt(
            "fragmentLifetimeSeconds", 5);
        int maximumHits = 1 + plant.getUpgradeTraitInt("BOUNCES_1", 0);
        double[][] directions = {
            {-1, -1}, {-1, 0}, {-1, 1}, {0, -1},
            {0, 1}, {1, -1}, {1, 0}, {1, 1}
        };
        for (int index = 0; index < count; index++) {
            double[] direction = directions[index % directions.length];
            engine.board.addGrapeshotFragment(new GrapeshotFragment(
                center.getRow(), center.getColumn(), direction[0], direction[1],
                baseDamage * Math.max(1, multiplier),
                lifetimeSeconds * Game.TICKS_PER_SECOND, maximumHits, plant.getName()));
        }
        engine.addEvent("Grapeshot launched " + count + " bouncing fragment(s) for "
            + lifetimeSeconds + " seconds.");
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
        engine.currentLevel.getRuleStrategy().validatePlantSelection(engine, definition);
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
        return definition.getFamily() == PlantFamily.SUN_PRODUCER || definition.hasTag("SUN");
    }
    static boolean isPreWaveSetup(Game engine) {
        return engine.currentLevel != null
            && engine.currentLevel.getRuleStrategy().isPreWaveSetup(engine);
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
