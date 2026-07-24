package model;

final class PlantRuntimeSystem {
    private PlantRuntimeSystem() {
    }

    static void tick(Plant plant) {
        plant.ageTicks++;
        plant.disabledTicks = decrement(plant.disabledTicks);
        plant.digestionTicks = decrement(plant.digestionTicks);
        plant.armTicksRemaining = decrement(plant.armTicksRemaining);
        plant.cyanBulbTicks = decrement(plant.cyanBulbTicks);
        plant.blueBulbTicks = decrement(plant.blueBulbTicks);
        plant.orangeBulbTicks = decrement(plant.orangeBulbTicks);
        tickLifetime(plant);
        tickMintAura(plant);
    }

    private static int decrement(int value) {
        return value > 0 ? value - 1 : value;
    }

    private static void tickLifetime(Plant plant) {
        if (plant.lifetimeTicksRemaining <= 0) {
            return;
        }
        plant.lifetimeTicksRemaining--;
        if (plant.lifetimeTicksRemaining == 0) {
            plant.health = 0;
        }
    }

    private static void tickMintAura(Plant plant) {
        if (plant.mintAuraTicksRemaining <= 0) {
            return;
        }
        plant.mintAuraTicksRemaining--;
        if (plant.mintAuraTicksRemaining == 0 && plant.getAbility().isMint()) {
            plant.health = 0;
        }
    }

    static int growthStage(Plant plant) {
        PlantAbility ability = plant.getAbility();
        if (ability != PlantAbility.SUN_SHROOM && ability != PlantAbility.KIWIBEAST) {
            return 3;
        }
        int growthDelta = plant.getUpgradeTraitInt("GROW_TIME_DELTA", 0);
        int stageTwo = Math.max(1, plant.definition.getAbilityParameterInt("stage2Seconds", 24)
            + growthDelta) * Game.TICKS_PER_SECOND;
        int stageThree = Math.max(1, plant.definition.getAbilityParameterInt("stage3Seconds", 72)
            + growthDelta) * Game.TICKS_PER_SECOND;
        if (plant.ageTicks < stageTwo) {
            return 1;
        }
        return plant.ageTicks < stageThree ? 2 : 3;
    }

    static int effectiveAttackPower(Plant plant) {
        if (plant.getAbility() != PlantAbility.KIWIBEAST) {
            return plant.getAttackPower();
        }
        int stage = growthStage(plant);
        if (stage == 3 && plant.hasUpgradeTrait("MAX_SIZE_1")) {
            stage++;
        }
        return Math.max(1, plant.getAttackPower()) * stage;
    }

    static int sunShroomProduction(Plant plant) {
        return switch (growthStage(plant)) {
            case 1 -> plant.definition.getAbilityParameterInt("stage1Sun", 25);
            case 2 -> plant.definition.getAbilityParameterInt("stage2Sun", 50);
            default -> plant.definition.getAbilityParameterInt("stage3Sun", 75);
        };
    }

    static void addIceLayer(Plant plant) {
        if (plant.definition.hasTag("Fire") || plant.frozenHealth > 0) {
            return;
        }
        plant.iceHits++;
        if (plant.iceHits >= 3) {
            plant.iceHits = 3;
            plant.frozenHealth = 600;
        }
    }

    static void freezeImmediately(Plant plant) {
        if (!plant.definition.hasTag("Fire")) {
            plant.iceHits = 3;
            plant.frozenHealth = 600;
        }
    }

    static void damageIce(Plant plant, int damage, boolean fire) {
        if (plant.frozenHealth <= 0) {
            return;
        }
        plant.frozenHealth = fire ? 0
            : Math.max(0, plant.frozenHealth - Math.max(0, damage));
        if (plant.frozenHealth == 0) {
            plant.iceHits = 0;
        }
    }

    static void transformByWizard(Plant plant, String wizardId) {
        if (wizardId != null && !wizardId.isBlank()) {
            plant.transformedBy = wizardId;
        }
    }

    static void releaseWizardTransformation(Plant plant, String wizardId) {
        if (wizardId != null && wizardId.equals(plant.transformedBy)) {
            plant.transformedBy = null;
        }
    }

    static void clearControlEffects(Plant plant) {
        plant.disabledTicks = 0;
        plant.digestionTicks = 0;
        plant.iceHits = 0;
        plant.frozenHealth = 0;
        plant.octopusHealth = 0;
        plant.transformedBy = null;
    }

    static void matureFully(Plant plant) {
        int stageThree = plant.definition.getAbilityParameterInt("stage3Seconds", 72);
        plant.ageTicks = Math.max(plant.ageTicks, stageThree * Game.TICKS_PER_SECOND);
    }

    static void restoreLifetime(Plant plant) {
        if (plant.getAbility() == PlantAbility.SHORT_RANGE_SHROOM) {
            int seconds = plant.definition.getAbilityParameterInt("lifetimeSeconds", 60)
                + plant.getUpgradeTraitInt("LIFESPAN_10S", 0);
            plant.lifetimeTicksRemaining = Math.max(1, seconds) * Game.TICKS_PER_SECOND;
        }
    }

    static void startMintAura(Plant plant, int ticks) {
        if (!plant.getAbility().isMint()) {
            throw new IllegalStateException("Only mint plants can start a mint aura.");
        }
        plant.mintAuraTicksRemaining = Math.max(1, ticks);
    }

    static int nextBowlingBulbDamage(Plant plant) {
        if (plant.getAbility() != PlantAbility.BOWLING_BULB) {
            return effectiveAttackPower(plant);
        }
        if (plant.orangeBulbTicks <= 0) {
            plant.orangeBulbTicks = adjustedBulbRegenSeconds(plant, "orangeRegenSeconds", 10)
                * Game.TICKS_PER_SECOND;
            return plant.definition.getAbilityParameterInt("orangeDamage", 180);
        }
        if (plant.blueBulbTicks <= 0) {
            plant.blueBulbTicks = adjustedBulbRegenSeconds(plant, "blueRegenSeconds", 5)
                * Game.TICKS_PER_SECOND;
            return plant.definition.getAbilityParameterInt("blueDamage", 120);
        }
        if (plant.cyanBulbTicks <= 0) {
            plant.cyanBulbTicks = adjustedBulbRegenSeconds(plant, "cyanRegenSeconds", 2)
                * Game.TICKS_PER_SECOND;
            return plant.definition.getAbilityParameterInt("cyanDamage", 40);
        }
        return 0;
    }

    private static int adjustedBulbRegenSeconds(Plant plant, String parameter, int fallback) {
        int delta = plant.getUpgradeTraitInt("BULB_REGEN_DELTA", 0);
        return Math.max(1, plant.definition.getAbilityParameterInt(parameter, fallback) + delta);
    }
}
