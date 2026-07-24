package model;

import java.util.EnumMap;
import java.util.Map;

public final class PlantBehaviorFactory {
    private static final PlantBehavior NO_ACTION = (game, plant) -> { };
    private static final PlantBehavior GENERIC_ATTACK = (game, plant) -> {
        if (plant.isHoming()) {
            game.attackHoming(plant);
        } else if (plant.isMelee()) {
            game.attackMelee(plant);
        } else if (plant.isShooter()) {
            game.shootProjectiles(plant);
        }
    };
    private static final Map<PlantAbility, PlantBehavior> BEHAVIORS = createBehaviors();

    private PlantBehaviorFactory() { }

    public static PlantBehavior create(PlantAbility ability) {
        return BEHAVIORS.getOrDefault(ability == null ? PlantAbility.GENERIC : ability,
            GENERIC_ATTACK);
    }

    private static Map<PlantAbility, PlantBehavior> createBehaviors() {
        EnumMap<PlantAbility, PlantBehavior> result = new EnumMap<>(PlantAbility.class);
        result.put(PlantAbility.THREEPEATER, Game::fireThreepeater);
        result.put(PlantAbility.ROTOBAGA, Game::fireRotobaga);
        result.put(PlantAbility.SPLIT_PEA, Game::fireSplitPea);
        result.put(PlantAbility.STARFRUIT, Game::fireStarfruit);
        result.put(PlantAbility.BOWLING_BULB, Game::bowlBulbs);
        result.put(PlantAbility.FUME_SHROOM, Game::attackFumeShroom);
        PlantBehavior lobber = Game::attackLobber;
        for (PlantAbility ability : new PlantAbility[] {
            PlantAbility.CABBAGE_PULT, PlantAbility.KERNEL_PULT,
            PlantAbility.MELON_PULT, PlantAbility.WINTER_MELON,
            PlantAbility.PEPPER_PULT
        }) {
            result.put(ability, lobber);
        }
        result.put(PlantAbility.CAULIPOWER, Game::hypnotizeWithCaulipower);
        result.put(PlantAbility.ELECTRIC_BLUEBERRY, Game::strikeWithBlueberry);
        result.put(PlantAbility.MAGNET_SHROOM, Game::useMagnetShroom);
        result.put(PlantAbility.CHOMPER, Game::chompZombie);
        result.put(PlantAbility.CAT_TAIL, Game::attackHoming);
        PlantBehavior melee = Game::attackMelee;
        for (PlantAbility ability : new PlantAbility[] {
            PlantAbility.BONK_CHOY, PlantAbility.PHAT_BEET,
            PlantAbility.WASABI_WHIP, PlantAbility.KIWIBEAST
        }) {
            result.put(ability, melee);
        }
        for (PlantAbility ability : new PlantAbility[] {
            PlantAbility.TORCHWOOD, PlantAbility.WALL_NUT, PlantAbility.TALL_NUT,
            PlantAbility.ENDURIAN, PlantAbility.GARLIC, PlantAbility.SWEET_POTATO,
            PlantAbility.EXPLODE_O_NUT, PlantAbility.PUMPKIN, PlantAbility.SUN_BEAN,
            PlantAbility.HYPNO_SHROOM, PlantAbility.LILY_PAD, PlantAbility.IMITATER,
            PlantAbility.GENERIC
        }) {
            result.put(ability, NO_ACTION);
        }
        return Map.copyOf(result);
    }
}
