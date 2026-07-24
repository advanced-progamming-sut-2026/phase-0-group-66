package model;

final class PlantFoodBehaviorFactory {
    private PlantFoodBehaviorFactory() { }

    static void activate(Game game, Plant plant) {
        PlantFoodType type = plant.getDefinition().getPlantFoodType();
        switch (type) {
            case NONE -> game.addEvent(plant.getName() + " has no separate plant-food effect.");
            case SUN_BURST -> PlantFoodSupportSystem.produceSun(game, plant);
            case SHOOTER_VOLLEY -> PlantFoodAttackSystem.shooterVolley(game, plant);
            case REPEATER_GIANT_VOLLEY -> PlantFoodAttackSystem.repeaterVolley(game, plant);
            case THREEPEATER_FAN_VOLLEY -> PlantFoodAttackSystem.threepeaterFan(game, plant);
            case SNOW_PEA_LANE_FREEZE -> PlantFoodAttackSystem.snowPeaLaneFreeze(game, plant);
            case ROTOBAGA_DIAGONAL_VOLLEY -> PlantFoodAttackSystem.rotobagaVolley(game, plant);
            case PEA_POD_GIANT_VOLLEY -> PlantFoodAttackSystem.peaPodGiants(game, plant);
            case SPLIT_PEA_DUAL_VOLLEY -> PlantFoodAttackSystem.splitPeaVolley(game, plant);
            case CLEAR_LANE -> PlantFoodAttackSystem.clearLane(game, plant);
            case HYPNOTIZE_RANDOM -> PlantFoodAttackSystem.hypnotizeRandom(game, plant);
            case ELIMINATE_RANDOM -> PlantFoodAttackSystem.eliminateRandom(game, plant);
            case BOWLING_EXPLOSIVE_BULBS -> PlantFoodAttackSystem.explosiveBulbs(game, plant);
            case CACTUS_ELECTRIC_PIERCE -> PlantFoodAttackSystem.cactusPierce(game, plant);
            case FIRE_LANE_VOLLEY -> PlantFoodAttackSystem.fireLaneVolley(game, plant);
            case STARFRUIT_OMNI_VOLLEY -> PlantFoodAttackSystem.starfruitVolley(game, plant);
            case POISON_VOLLEY -> PlantFoodAttackSystem.poisonVolley(game, plant);
            case GATLING_MEGA_VOLLEY -> PlantFoodAttackSystem.gatlingVolley(game, plant);
            case SHROOM_VOLLEY_RESET -> PlantFoodAttackSystem.shroomVolley(game, plant);
            case KNOCKBACK_BLAST -> PlantFoodAttackSystem.knockbackBlast(game, plant);
            case LOBBER_RANDOM_BARRAGE -> PlantFoodAttackSystem.lobberBarrage(game, plant);
            case BUTTER_ALL -> PlantFoodAttackSystem.butterAll(game, plant);
            case MELON_RANDOM_BARRAGE -> PlantFoodAttackSystem.melonBarrage(game, plant, false, false);
            case WINTER_MELON_RANDOM_BARRAGE -> PlantFoodAttackSystem.melonBarrage(game, plant, true, false);
            case PEPPER_RANDOM_BARRAGE -> PlantFoodAttackSystem.melonBarrage(game, plant, false, true);
            case ARM_AND_CLONE -> PlantFoodSupportSystem.armAndClone(game, plant);
            case MULTI_SMASH -> PlantFoodAttackSystem.multiSmash(game, plant);
            case DROWN_TARGETS -> PlantFoodSupportSystem.drownTargets(game, plant);
            case MAP_FREEZE -> PlantFoodSupportSystem.freezeMap(game, plant);
            case BONK_AREA_BARRAGE -> PlantFoodAttackSystem.bonkArea(game, plant);
            case PHAT_BEET_SHOCKWAVE -> PlantFoodAttackSystem.phatBeetWave(game, plant);
            case CHOMPER_MULTI_SWALLOW -> PlantFoodAttackSystem.chomperSwallow(game, plant);
            case WASABI_SPIN -> PlantFoodAttackSystem.wasabiSpin(game, plant);
            case KIWIBEAST_SLAM -> PlantFoodAttackSystem.kiwibeastSlam(game, plant);
            case REINFORCE -> PlantFoodSupportSystem.reinforce(game, plant);
            case ENDURIAN_REINFORCE -> PlantFoodSupportSystem.reinforceEndurian(game, plant);
            case REDIRECT_LANE -> PlantFoodSupportSystem.redirectLane(game, plant);
            case PULL_TO_DEFENDER -> PlantFoodSupportSystem.pullToDefender(game, plant);
            case EXPLODING_REINFORCE -> PlantFoodSupportSystem.explodingReinforce(game, plant);
            case TORCHWOOD_FLAME -> PlantFoodSupportSystem.blueFlame(game, plant);
            case REMOVE_ARMOR -> PlantFoodSupportSystem.removeArmor(game, plant);
            case HYPNO_GARGANTUAR -> PlantFoodSupportSystem.prepareHypnoGargantuar(game, plant);
            case HOMING_VOLLEY -> PlantFoodAttackSystem.homingVolley(game, plant);
            case CLONE_SUPPORTS -> PlantFoodSupportSystem.cloneSupports(game, plant);
        }
    }
}
