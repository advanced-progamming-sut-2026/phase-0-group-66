package model;

import java.util.EnumMap;
import java.util.Map;

/** Resolves zombie behavior from the explicit ability field in zombies.json. */
public final class ZombieBehaviorFactory {
    private static final ZombieBehavior NO_ACTION = (game, zombie) -> { };
    private static final Map<ZombieAbility, ZombieBehavior> BEHAVIORS = createBehaviors();

    private ZombieBehaviorFactory() { }

    public static ZombieBehavior create(ZombieAbility ability) {
        return BEHAVIORS.getOrDefault(ability == null ? ZombieAbility.GENERIC : ability,
            NO_ACTION);
    }

    private static Map<ZombieAbility, ZombieBehavior> createBehaviors() {
        EnumMap<ZombieAbility, ZombieBehavior> result = new EnumMap<>(ZombieAbility.class);
        result.put(ZombieAbility.GARGANTUAR, Game::throwGargantuarImp);
        result.put(ZombieAbility.RA, Game::stealSunWithRa);
        result.put(ZombieAbility.TOMB_RAISER, Game::raiseTombs);
        result.put(ZombieAbility.HUNTER, Game::throwHunterSnowball);
        result.put(ZombieAbility.TROGLOBITE, Game::pushTroglobiteIce);
        result.put(ZombieAbility.FISHERMAN, Game::hookPlantWithFisherman);
        result.put(ZombieAbility.OCTOPUS, Game::throwOctopus);
        result.put(ZombieAbility.WIZARD, Game::transformPlantWithWizard);
        result.put(ZombieAbility.KING, Game::knightNearbyZombie);
        result.put(ZombieAbility.TURQUOISE_SKULL, Game::useTurquoiseSkull);
        result.put(ZombieAbility.PROSPECTOR, Game::launchProspectorDynamite);
        result.put(ZombieAbility.PIANIST, Game::playPiano);
        return Map.copyOf(result);
    }
}
