package model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PlantCatalogValidator {
    public static final int EXPECTED_PLANT_COUNT = 69;
    public static final int EXPECTED_REQUIRED_COUNT = 57;
    private static final Set<Integer> BONUS_IDS = Set.of(
        11, 14, 15, 19, 20, 34, 41, 42, 43, 48, 54, 55
    );
    private static final List<String> EXPECTED_NAMES = List.of(
        "Sunflower", "Twin Sunflower", "Sun-shroom", "Primal Sunflower", "Gold Bloom",
        "Peashooter", "Repeater", "Threepeater", "Snow Pea", "Rotobaga", "Pea Pod",
        "Split Pea", "Citron", "Caulipower", "Electric Blueberry", "Bowling Bulb",
        "Cactus", "Fire Peashooter", "Starfruit", "Goo Peashooter", "Mega Gatling Pea",
        "Sea-shroom", "Puff-shroom", "Fume-shroom", "Cabbage-pult", "Kernel-pult",
        "Melon-pult", "Winter Melon", "Pepper-pult", "Potato Mine", "Primal Potato Mine",
        "Cherry Bomb", "Squash", "Grapeshot", "Jalapeno", "Doom-shroom", "Tangle Kelp",
        "Iceberg Lettuce", "Bonk Choy", "Phat Beet", "Chomper", "Wasabi Whip",
        "Kiwibeast", "Wall-nut", "Tall-nut", "Endurian", "Garlic", "Sweet Potato",
        "Explode-o-nut", "Pumpkin", "Sun Bean", "Torchwood", "Magnet-shroom",
        "Hypno-shroom", "Cat-tail", "Imitater", "Ice-shroom", "Lily Pad", "Hot Potato",
        "Grave Buster", "Enlighten-mint", "Appease-mint", "Arma-mint", "Bombard-mint",
        "Enforce-mint", "Reinforce-mint", "Enchant-mint", "Pierce-mint", "catTail-mint"
    );

    private PlantCatalogValidator() { }

    public static void validate(List<PlantDefinition> definitions) {
        if (definitions == null || definitions.size() != EXPECTED_PLANT_COUNT) {
            throw new IllegalStateException("The official plant catalog must contain exactly "
                + EXPECTED_PLANT_COUNT + " plants.");
        }
        Map<Integer, PlantDefinition> byId = new HashMap<>();
        int requiredCount = 0;
        for (PlantDefinition definition : definitions) {
            byId.put(definition.getId(), definition);
            if (definition.isRequired()) {
                requiredCount++;
            }
            validateDefinition(definition);
        }
        if (requiredCount != EXPECTED_REQUIRED_COUNT) {
            throw new IllegalStateException("The official plant catalog must contain exactly "
                + EXPECTED_REQUIRED_COUNT + " required plants.");
        }
        for (int id = 1; id <= EXPECTED_PLANT_COUNT; id++) {
            validateId(byId, id);
        }
    }

    private static void validateDefinition(PlantDefinition definition) {
        if (definition.getAbility() == PlantAbility.GENERIC) {
            throw new IllegalStateException("Plant " + definition.getName()
                + " must have an explicit ability kind.");
        }
        if (definition.getBaseAbility().isBlank()) {
            throw new IllegalStateException("Plant " + definition.getName()
                + " must have an ability description.");
        }
        if (definition.getDamage().isBlank()) {
            throw new IllegalStateException("Plant " + definition.getName()
                + " must have a damage display value.");
        }
        validateAbilityParameterContract(definition);
        validatePlantFoodParameterContract(definition);
    }

    private static void validateAbilityParameterContract(PlantDefinition definition) {
        switch (definition.getAbility()) {
            case SUNFLOWER, TWIN_SUNFLOWER, PRIMAL_SUNFLOWER, GOLD_BLOOM ->
                requireAbilityParameters(definition, "sun");
            case SUN_SHROOM -> requireAbilityParameters(definition, "stage1Sun", "stage2Sun",
                "stage3Sun", "stage2Seconds", "stage3Seconds");
            case THREEPEATER -> requireAbilityParameters(definition, "laneRadius");
            case SNOW_PEA -> requireAbilityParameters(definition, "chillSeconds");
            case ROTOBAGA -> requireAbilityParameters(definition, "shotsPerDirection");
            case PEA_POD -> requireAbilityParameters(definition, "maxStacks");
            case SPLIT_PEA -> requireAbilityParameters(definition, "forwardShots",
                "backwardShots");
            case CAULIPOWER, ELECTRIC_BLUEBERRY ->
                requireAbilityParameters(definition, "targets");
            case BOWLING_BULB -> requireAbilityParameters(definition, "cyanDamage",
                "blueDamage", "orangeDamage", "cyanRegenSeconds", "blueRegenSeconds",
                "orangeRegenSeconds", "maxBounces");
            case CACTUS -> requireAbilityParameters(definition, "maxHits");
            case GOO_PEASHOOTER -> requireAbilityParameters(definition, "poisonSeconds",
                "poisonDamageFactor");
            case SHORT_RANGE_SHROOM -> requireAbilityParameters(definition, "rangeTiles",
                "lifetimeSeconds");
            case FUME_SHROOM -> requireAbilityParameters(definition, "rangeTiles");
            case KERNEL_PULT -> requireAbilityParameters(definition, "butterChancePercent",
                "butterDamage", "butterStunSeconds");
            case MELON_PULT, WINTER_MELON, PEPPER_PULT ->
                requireAbilityParameters(definition, "splashDamageFactor");
            case POTATO_MINE, PRIMAL_POTATO_MINE ->
                requireAbilityParameters(definition, "armSeconds");
            case GRAPESHOT -> requireAbilityParameters(definition, "fragmentCount",
                "fragmentDamage", "fragmentLifetimeSeconds");
            case ICEBERG_LETTUCE, ICE_SHROOM ->
                requireAbilityParameters(definition, "freezeSeconds", "chillSeconds");
            case BONK_CHOY, PHAT_BEET, WASABI_WHIP ->
                requireAbilityParameters(definition, "rowRadius", "rangeTiles");
            case CHOMPER -> requireAbilityParameters(definition, "digestSeconds", "rangeTiles");
            case KIWIBEAST -> requireAbilityParameters(definition, "stage2Seconds",
                "stage3Seconds", "rowRadius", "rangeTiles");
            case SWEET_POTATO -> requireAbilityParameters(definition, "pullRadiusTiles");
            case SUN_BEAN -> requireAbilityParameters(definition, "sunPerHit");
            case TORCHWOOD -> requireAbilityParameters(definition, "peaDamageMultiplier",
                "deathExplosionDamage");
            case MAGNET_SHROOM -> requireAbilityParameters(definition, "rangeTiles");
            case HYPNO_SHROOM -> requireAbilityParameters(definition,
                "hypnotizedHealthBuffPercent", "hypnotizedDamageBuffPercent");
            case HOT_POTATO, GRAVE_BUSTER ->
                requireAbilityParameters(definition, "finishExplosionDamage");
            case ENLIGHTEN_MINT, APPEASE_MINT, ARMA_MINT, BOMBARD_MINT,
                 ENFORCE_MINT, REINFORCE_MINT, ENCHANT_MINT, PIERCE_MINT,
                 CATTAIL_MINT -> requireAbilityParameters(definition, "durationSeconds");
            default -> { }
        }
    }

    private static void validatePlantFoodParameterContract(PlantDefinition definition) {
        switch (definition.getPlantFoodType()) {
            case NONE, CLEAR_LANE -> { }
            case SUN_BURST -> requireFoodParameters(definition, "sun");
            case SHOOTER_VOLLEY, HOMING_VOLLEY -> requireFoodParameters(definition, "shots");
            case REPEATER_GIANT_VOLLEY -> requireFoodParameters(definition, "volleyShots",
                "giantMultiplier");
            case THREEPEATER_FAN_VOLLEY, ROTOBAGA_DIAGONAL_VOLLEY,
                 SPLIT_PEA_DUAL_VOLLEY, FIRE_LANE_VOLLEY, STARFRUIT_OMNI_VOLLEY,
                 SHROOM_VOLLEY_RESET -> requireFoodParameters(definition, "volleys");
            case SNOW_PEA_LANE_FREEZE -> requireFoodParameters(definition, "volleys",
                "freezeSeconds", "chillSeconds");
            case PEA_POD_GIANT_VOLLEY -> requireFoodParameters(definition, "giantMultiplier");
            case HYPNOTIZE_RANDOM, ELIMINATE_RANDOM, LOBBER_RANDOM_BARRAGE,
                 MULTI_SMASH, DROWN_TARGETS, CHOMPER_MULTI_SWALLOW ->
                requireFoodParameters(definition, "targets");
            case BOWLING_EXPLOSIVE_BULBS -> requireFoodParameters(definition, "bulbs",
                "damageMultiplier", "splashDamageFactor", "rowRadius", "columnRadius");
            case CACTUS_ELECTRIC_PIERCE -> requireFoodParameters(definition,
                "damageMultiplier", "maxHits");
            case POISON_VOLLEY -> requireFoodParameters(definition, "volleys",
                "poisonSeconds", "poisonDamageFactor");
            case GATLING_MEGA_VOLLEY -> requireFoodParameters(definition, "volleyShots",
                "giantShots", "giantMultiplier");
            case KNOCKBACK_BLAST -> requireFoodParameters(definition, "damageMultiplier",
                "knockbackTiles");
            case BUTTER_ALL -> requireFoodParameters(definition, "stunSeconds");
            case MELON_RANDOM_BARRAGE, WINTER_MELON_RANDOM_BARRAGE,
                 PEPPER_RANDOM_BARRAGE -> requireFoodParameters(definition, "targets",
                "damageMultiplier", "splashDamageFactor");
            case ARM_AND_CLONE, CLONE_SUPPORTS -> requireFoodParameters(definition, "clones");
            case MAP_FREEZE -> requireFoodParameters(definition, "freezeSeconds",
                "chillSeconds");
            case BONK_AREA_BARRAGE -> requireFoodParameters(definition, "repetitions",
                "rowRadius", "columnRadius");
            case PHAT_BEET_SHOCKWAVE, WASABI_SPIN, KIWIBEAST_SLAM ->
                requireFoodParameters(definition, "damageMultiplier", "rowRadius",
                    "columnRadius");
            case REINFORCE -> requireFoodParameters(definition, "shield");
            case ENDURIAN_REINFORCE -> requireFoodParameters(definition, "shield",
                "reflectMultiplier");
            case PULL_TO_DEFENDER -> requireFoodParameters(definition, "pullRadiusTiles");
            case EXPLODING_REINFORCE -> requireFoodParameters(definition, "shield",
                "explosionDamage");
            case TORCHWOOD_FLAME -> requireFoodParameters(definition, "peaDamageMultiplier");
            case REDIRECT_LANE, REMOVE_ARMOR, HYPNO_GARGANTUAR -> { }
        }
    }

    private static void requireAbilityParameters(PlantDefinition definition, String... names) {
        requireParameters(definition, definition.getAbilityParameters(), "ability", names);
    }

    private static void requireFoodParameters(PlantDefinition definition, String... names) {
        requireParameters(definition, definition.getPlantFoodParameters(), "plant food", names);
    }

    private static void requireParameters(PlantDefinition definition, Map<String, Double> values,
                                          String section, String... names) {
        for (String name : names) {
            if (!values.containsKey(PlantDefinition.normalizeKey(name))) {
                throw new IllegalStateException("Plant " + definition.getName() + " " + section
                    + " is missing JSON parameter: " + name + ".");
            }
        }
    }

    private static void validateId(Map<Integer, PlantDefinition> byId, int id) {
        PlantDefinition definition = byId.get(id);
        if (definition == null) {
            throw new IllegalStateException("Missing official plant id " + id + ".");
        }
        String expectedName = EXPECTED_NAMES.get(id - 1);
        if (!definition.getName().equals(expectedName)) {
            throw new IllegalStateException("Plant id " + id + " must be " + expectedName
                + ", not " + definition.getName() + ".");
        }
        boolean expectedRequired = !BONUS_IDS.contains(id);
        if (definition.isRequired() != expectedRequired) {
            throw new IllegalStateException("Plant " + definition.getName()
                + " has an incorrect required/bonus flag.");
        }
    }

    public static Set<Integer> getBonusIds() { return BONUS_IDS; }
}
