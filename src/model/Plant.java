package model;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public abstract class Plant {
    private static final int PUFF_LIFETIME_TICKS = 60 * Game.TICKS_PER_SECOND;

    protected final PlantDefinition definition;
    protected String name;
    protected int health;
    protected final int maxHealth;
    protected int sunCost;
    protected GridPosition position;
    protected int attackPower;
    protected int cooldown;
    private final int plantLevel;
    private final int actionIntervalTicks;
    private int rechargeTicks;
    private final int sunProductionBonus;
    private final boolean doubleSunChance;
    private final int chillDurationTicks;
    private final int pierceBonus;
    private final PlantAbility ability;
    private final Map<String, Double> upgradeTraits;
    private int actionTicksRemaining;
    private int plantFoodShield;
    private int coverShield;
    private int ageTicks;
    private int stackCount;
    private int disabledTicks;
    private int digestionTicks;
    private int iceHits;
    private int frozenHealth;
    private int octopusHealth;
    private int armTicksRemaining;
    private int lifetimeTicksRemaining;
    private int cyanBulbTicks;
    private int blueBulbTicks;
    private int orangeBulbTicks;
    private int reflectDamageMultiplier = 1;
    private int blueFlameMultiplier = 2;
    private int explosiveShieldDamage;
    private boolean shieldExplosionPending;
    private boolean hypnoGargantuarReady;
    private int mintAuraTicksRemaining;
    private final Set<Plant> mintEmpoweredPlants = Collections.newSetFromMap(
        new IdentityHashMap<>());
    private String transformedBy;
    private boolean trappedInIceTile;

    protected Plant(PlantDefinition definition) {
        this(definition, 1);
    }

    protected Plant(PlantDefinition definition, int level) {
        if (definition == null) {
            throw new IllegalArgumentException("Plant definition cannot be null.");
        }
        PlantStats stats = PlantStats.calculate(definition, level);
        this.definition = definition;
        this.name = definition.getName();
        this.plantLevel = stats.getLevel();
        this.maxHealth = stats.getMaxHealth();
        this.health = maxHealth;
        this.sunCost = stats.getCost();
        this.attackPower = stats.getDamage();
        this.actionIntervalTicks = Math.max(1, (int) Math.round(
            stats.getActionIntervalSeconds() * Game.TICKS_PER_SECOND));
        this.rechargeTicks = Math.max(0, (int) Math.round(
            stats.getRechargeSeconds() * Game.TICKS_PER_SECOND));
        this.sunProductionBonus = stats.getSunProductionBonus();
        this.doubleSunChance = stats.hasDoubleSunChance();
        int baseChillSeconds = definition.getAbilityParameterInt("chillSeconds", 5);
        this.chillDurationTicks = Math.max(0, baseChillSeconds) * Game.TICKS_PER_SECOND
            + stats.getChillBonusTicks();
        this.pierceBonus = stats.getPierceBonus();
        this.ability = PlantAbility.fromDefinition(definition);
        this.upgradeTraits = stats.getTraitValues();
        this.actionTicksRemaining = actionIntervalTicks;
        this.stackCount = 1;
        initializeRuntimeState();
    }

    private void initializeRuntimeState() {
        if (ability == PlantAbility.POTATO_MINE
            || ability == PlantAbility.PRIMAL_POTATO_MINE) {
            int defaultSeconds = ability == PlantAbility.POTATO_MINE ? 15 : 5;
            int configured = definition.getAbilityParameterInt("armSeconds", defaultSeconds);
            int seconds = Math.min(configured,
                Math.max(1, (int) Math.round(actionIntervalTicks
                    / (double) Game.TICKS_PER_SECOND)));
            armTicksRemaining = seconds * Game.TICKS_PER_SECOND;
        }
        if (ability == PlantAbility.SHORT_RANGE_SHROOM) {
            int seconds = definition.getAbilityParameterInt("lifetimeSeconds", 60)
                + getUpgradeTraitInt("LIFESPAN_10S", 0);
            lifetimeTicksRemaining = Math.max(1, seconds) * Game.TICKS_PER_SECOND;
        }
    }

    public void attack() {
        actionTicksRemaining = 0;
    }

    public void takeDamage(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Damage cannot be negative.");
        }
        int remaining = absorbCoverDamage(amount);
        int shieldBefore = plantFoodShield;
        remaining = absorbPlantFoodShield(remaining);
        if (explosiveShieldDamage > 0 && shieldBefore > 0 && plantFoodShield == 0) {
            shieldExplosionPending = true;
        }
        health = Math.max(0, health - remaining);
    }

    private int absorbCoverDamage(int amount) {
        int absorbed = Math.min(coverShield, amount);
        coverShield -= absorbed;
        return amount - absorbed;
    }

    private int absorbPlantFoodShield(int amount) {
        int absorbed = Math.min(plantFoodShield, amount);
        plantFoodShield -= absorbed;
        return amount - absorbed;
    }

    public void healToFull() {
        health = maxHealth;
    }

    public void usePlantFood() {
        healToFull();
        clearControlEffects();
        actionTicksRemaining = 0;
        if (ability == PlantAbility.POTATO_MINE
            || ability == PlantAbility.PRIMAL_POTATO_MINE) {
            armTicksRemaining = 0;
        }
        if (ability == PlantAbility.SHORT_RANGE_SHROOM) {
            restoreLifetime();
        }
    }

    public void tickRuntimeState() {
        ageTicks++;
        if (disabledTicks > 0) {
            disabledTicks--;
        }
        if (digestionTicks > 0) {
            digestionTicks--;
        }
        if (armTicksRemaining > 0) {
            armTicksRemaining--;
        }
        if (lifetimeTicksRemaining > 0) {
            lifetimeTicksRemaining--;
            if (lifetimeTicksRemaining == 0) {
                health = 0;
            }
        }
        if (cyanBulbTicks > 0) {
            cyanBulbTicks--;
        }
        if (blueBulbTicks > 0) {
            blueBulbTicks--;
        }
        if (orangeBulbTicks > 0) {
            orangeBulbTicks--;
        }
        if (mintAuraTicksRemaining > 0) {
            mintAuraTicksRemaining--;
            if (mintAuraTicksRemaining == 0 && ability.isMint()) {
                health = 0;
            }
        }
    }

    public boolean tickActionTimer() {
        if (actionTicksRemaining > 0) {
            actionTicksRemaining--;
        }
        return actionTicksRemaining <= 0;
    }

    public void resetActionTimer() {
        actionTicksRemaining = actionIntervalTicks;
    }

    public boolean isAvailable() {
        return cooldown <= 0;
    }

    public boolean isDestroyed() {
        return health <= 0 && !isMintAuraActive();
    }

    public boolean isOperational() {
        return !isDestroyed() && disabledTicks <= 0 && digestionTicks <= 0
            && frozenHealth <= 0 && octopusHealth <= 0 && transformedBy == null
            && !trappedInIceTile;
    }

    public boolean isSunProducer() {
        return categoryEquals("Sun Producer");
    }

    public boolean isShooter() {
        return categoryEquals("Shooter") || categoryEquals("Lobber")
            || categoryEquals("Strike-through");
    }

    public boolean isLobber() {
        return categoryEquals("Lobber");
    }

    public boolean isHoming() {
        return categoryEquals("Homing");
    }

    public boolean isMelee() {
        return categoryEquals("Melee");
    }

    public boolean isExplosive() {
        return categoryEquals("Explosive") || definition.hasTag("Explosive");
    }

    public boolean isTrap() {
        return definition.hasTag("Trap");
    }

    public boolean isPiercing() {
        return categoryEquals("Strike-through") || pierceBonus > 0;
    }

    public boolean isArmed() {
        return armTicksRemaining <= 0;
    }

    public int getProjectileCount() {
        if (ability == PlantAbility.PEA_POD) {
            return stackCount;
        }
        return definition.getProjectileCount();
    }

    public ProjectileType getProjectileElementType() {
        if (definition.hasTag("Poison")) {
            return ProjectileType.POISON;
        }
        if (definition.hasTag("Fire")) {
            return ProjectileType.FIRE;
        }
        if (definition.hasTag("Ice")) {
            return ProjectileType.ICE;
        }
        return ProjectileType.NORMAL;
    }

    public int getGrowthStage() {
        if (ability != PlantAbility.SUN_SHROOM && ability != PlantAbility.KIWIBEAST) {
            return 3;
        }
        int growthDelta = getUpgradeTraitInt("GROW_TIME_DELTA", 0);
        int stageTwo = Math.max(1, definition.getAbilityParameterInt("stage2Seconds", 24)
            + growthDelta) * Game.TICKS_PER_SECOND;
        int stageThree = Math.max(1, definition.getAbilityParameterInt("stage3Seconds", 72)
            + growthDelta) * Game.TICKS_PER_SECOND;
        if (ageTicks < stageTwo) {
            return 1;
        }
        if (ageTicks < stageThree) {
            return 2;
        }
        return 3;
    }

    public int getEffectiveAttackPower() {
        if (ability == PlantAbility.KIWIBEAST) {
            int stage = getGrowthStage();
            if (stage == 3 && hasUpgradeTrait("MAX_SIZE_1")) {
                stage++;
            }
            return Math.max(1, attackPower) * stage;
        }
        return attackPower;
    }

    public int getSunShroomProduction() {
        return switch (getGrowthStage()) {
            case 1 -> definition.getAbilityParameterInt("stage1Sun", 25);
            case 2 -> definition.getAbilityParameterInt("stage2Sun", 50);
            default -> definition.getAbilityParameterInt("stage3Sun", 75);
        };
    }

    public boolean addStack() {
        int maximumStacks = definition.getAbilityParameterInt("maxStacks", 5);
        if (ability != PlantAbility.PEA_POD || stackCount >= maximumStacks) {
            return false;
        }
        stackCount++;
        return true;
    }

    public void addCoverShield(int amount) {
        if (amount > 0) {
            coverShield = Math.max(coverShield, amount);
        }
    }

    public void disableForTicks(int ticks) {
        disabledTicks = Math.max(disabledTicks, Math.max(0, ticks));
    }

    public void startDigestion(int ticks) {
        digestionTicks = Math.max(digestionTicks, Math.max(0, ticks));
    }

    public void addIceLayer() {
        if (definition.hasTag("Fire") || frozenHealth > 0) {
            return;
        }
        iceHits++;
        if (iceHits >= 3) {
            iceHits = 3;
            frozenHealth = 600;
        }
    }

    public void freezeImmediately() {
        if (!definition.hasTag("Fire")) {
            iceHits = 3;
            frozenHealth = 600;
        }
    }

    public void damageIce(int damage, boolean fire) {
        if (frozenHealth <= 0) {
            return;
        }
        frozenHealth = fire ? 0 : Math.max(0, frozenHealth - Math.max(0, damage));
        if (frozenHealth == 0) {
            iceHits = 0;
        }
    }

    public void coverWithOctopus() {
        octopusHealth = Math.max(octopusHealth, 600);
    }

    public void damageOctopus(int damage) {
        octopusHealth = Math.max(0, octopusHealth - Math.max(0, damage));
    }

    public void transformByWizard(String wizardId) {
        if (wizardId != null && !wizardId.isBlank()) {
            transformedBy = wizardId;
        }
    }

    public void releaseWizardTransformation(String wizardId) {
        if (wizardId != null && wizardId.equals(transformedBy)) {
            transformedBy = null;
        }
    }

    public void clearControlEffects() {
        disabledTicks = 0;
        digestionTicks = 0;
        iceHits = 0;
        frozenHealth = 0;
        octopusHealth = 0;
        transformedBy = null;
    }


    public void matureFully() {
        int stageThree = definition.getAbilityParameterInt("stage3Seconds", 72);
        ageTicks = Math.max(ageTicks, stageThree * Game.TICKS_PER_SECOND);
    }

    public void restoreLifetime() {
        if (ability == PlantAbility.SHORT_RANGE_SHROOM) {
            int seconds = definition.getAbilityParameterInt("lifetimeSeconds", 60)
                + getUpgradeTraitInt("LIFESPAN_10S", 0);
            lifetimeTicksRemaining = Math.max(1, seconds) * Game.TICKS_PER_SECOND;
        }
    }

    public void addPlantFoodShield(int amount) {
        plantFoodShield += Math.max(0, amount);
    }

    public void setReflectDamageMultiplier(int multiplier) {
        reflectDamageMultiplier = Math.max(reflectDamageMultiplier, Math.max(1, multiplier));
    }

    public int getReflectedDamage() {
        return Math.max(1, getEffectiveAttackPower()) * reflectDamageMultiplier;
    }

    public void igniteBlueFlame(int multiplier) {
        blueFlameMultiplier = Math.max(blueFlameMultiplier, Math.max(2, multiplier));
    }

    public int getTorchwoodMultiplier() {
        return blueFlameMultiplier;
    }

    public void armExplosiveShield(int damage) {
        explosiveShieldDamage = Math.max(explosiveShieldDamage, Math.max(0, damage));
    }

    public int consumeShieldExplosionDamage() {
        if (!shieldExplosionPending) {
            return 0;
        }
        shieldExplosionPending = false;
        return explosiveShieldDamage;
    }

    public void enableHypnoGargantuar() {
        hypnoGargantuarReady = true;
    }

    public boolean consumeHypnoGargantuar() {
        boolean result = hypnoGargantuarReady;
        hypnoGargantuarReady = false;
        return result;
    }

    public void startMintAura(int ticks) {
        if (!ability.isMint()) {
            throw new IllegalStateException("Only mint plants can start a mint aura.");
        }
        mintAuraTicksRemaining = Math.max(1, ticks);
    }

    public boolean isMintAuraActive() {
        return ability.isMint() && mintAuraTicksRemaining > 0;
    }

    public int getMintAuraTicksRemaining() {
        return mintAuraTicksRemaining;
    }

    public boolean markMintEmpowered(Plant plant) {
        return plant != null && mintEmpoweredPlants.add(plant);
    }

    public int nextBowlingBulbDamage() {
        if (ability != PlantAbility.BOWLING_BULB) {
            return getEffectiveAttackPower();
        }
        if (orangeBulbTicks <= 0) {
            orangeBulbTicks = adjustedBulbRegenSeconds("orangeRegenSeconds", 10)
                * Game.TICKS_PER_SECOND;
            return definition.getAbilityParameterInt("orangeDamage", 180);
        }
        if (blueBulbTicks <= 0) {
            blueBulbTicks = adjustedBulbRegenSeconds("blueRegenSeconds", 5)
                * Game.TICKS_PER_SECOND;
            return definition.getAbilityParameterInt("blueDamage", 120);
        }
        if (cyanBulbTicks <= 0) {
            cyanBulbTicks = adjustedBulbRegenSeconds("cyanRegenSeconds", 2)
                * Game.TICKS_PER_SECOND;
            return definition.getAbilityParameterInt("cyanDamage", 40);
        }
        return 0;
    }

    private int adjustedBulbRegenSeconds(String parameter, int fallback) {
        int delta = getUpgradeTraitInt("BULB_REGEN_DELTA", 0);
        return Math.max(1, definition.getAbilityParameterInt(parameter, fallback) + delta);
    }

    public final boolean hasUpgradeTrait(String trait) {
        return upgradeTraits.containsKey(normalizeTrait(trait));
    }

    public final double getUpgradeTrait(String trait, double fallback) {
        return upgradeTraits.getOrDefault(normalizeTrait(trait), fallback);
    }

    public final int getUpgradeTraitInt(String trait, int fallback) {
        return (int) Math.round(getUpgradeTrait(trait, fallback));
    }

    public double getEffectiveRange(double baseRange) {
        return Math.max(0.0, baseRange + getUpgradeTrait("RANGE_1_TILE", 0.0));
    }

    public int getDigestionSeconds() {
        int base = definition.getAbilityParameterInt("digestSeconds", 40);
        return Math.max(1, base + getUpgradeTraitInt("DIGEST_SECONDS_DELTA", 0));
    }

    public int getWarmthRadius() {
        return Math.max(1, 1 + getUpgradeTraitInt("WARMTH_RADIUS_1", 0));
    }

    public void applyImitaterCardModifiers(PlantDefinition imitaterDefinition,
                                            int imitaterLevel) {
        if (imitaterDefinition == null
            || imitaterDefinition.getAbility() != PlantAbility.IMITATER) {
            throw new IllegalArgumentException("Imitater modifiers require its definition.");
        }
        int costDelta = 0;
        double rechargeDelta = 0.0;
        for (PlantUpgrade upgrade : imitaterDefinition.getUpgrades()) {
            if (upgrade.getLevel() > imitaterLevel) {
                continue;
            }
            if (upgrade.getEffect() == PlantUpgradeType.SUN_COST_DELTA) {
                costDelta += (int) Math.round(upgrade.getAmount());
            } else if (upgrade.getEffect() == PlantUpgradeType.RECHARGE_DELTA) {
                rechargeDelta += upgrade.getAmount();
            }
        }
        sunCost = Math.max(0, sunCost + costDelta);
        rechargeTicks = Math.max(0, rechargeTicks
            + (int) Math.round(rechargeDelta * Game.TICKS_PER_SECOND));
    }

    public int getSplashDamageBonus() {
        return Math.max(0, getUpgradeTraitInt("AOE_DAMAGE_BONUS", 0));
    }

    private static String normalizeTrait(String trait) {
        return trait == null ? "" : trait.trim().toUpperCase(Locale.ROOT);
    }

    public PlantAbility getAbility() { return ability; }
    public int getPlantLevel() { return plantLevel; }
    public int getActionIntervalTicks() { return actionIntervalTicks; }
    public int getActionTicksRemaining() { return actionTicksRemaining; }
    public int getRechargeTicks() { return rechargeTicks; }
    public int getSunProductionBonus() { return sunProductionBonus; }
    public boolean hasDoubleSunChance() { return doubleSunChance; }
    public int getChillDurationTicks() { return chillDurationTicks; }
    public int getPierceBonus() { return pierceBonus; }
    public int getChillBonusTicks() {
        int base = Math.max(0, definition.getAbilityParameterInt("chillSeconds", 5))
            * Game.TICKS_PER_SECOND;
        return Math.max(0, chillDurationTicks - base);
    }
    public int getPlantFoodShield() { return plantFoodShield; }
    public int getCoverShield() { return coverShield; }
    public int getAgeTicks() { return ageTicks; }
    public int getStackCount() { return stackCount; }
    public int getDisabledTicks() { return disabledTicks; }
    public int getDigestionTicks() { return digestionTicks; }
    public int getIceHits() { return iceHits; }
    public int getFrozenHealth() { return frozenHealth; }
    public boolean isTrappedInIceTile() { return trappedInIceTile; }
    public void setTrappedInIceTile(boolean trapped) { trappedInIceTile = trapped; }
    public int getOctopusHealth() { return octopusHealth; }
    public int getArmTicksRemaining() { return armTicksRemaining; }
    public int getLifetimeTicksRemaining() { return lifetimeTicksRemaining; }
    public String getTransformedBy() { return transformedBy; }

    public PlantDefinition getDefinition() { return definition; }
    public String getName() { return name; }
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public int getSunCost() { return sunCost; }
    public int getAttackPower() { return attackPower; }
    public int getCooldown() { return cooldown; }

    public void setCooldown(int cooldown) {
        this.cooldown = Math.max(0, cooldown);
    }

    public GridPosition getPosition() { return position; }

    public void setPosition(GridPosition position) {
        this.position = position;
    }

    private boolean categoryEquals(String expected) {
        return definition.getCategory().trim().toLowerCase(Locale.ROOT)
            .equals(expected.toLowerCase(Locale.ROOT));
    }
}
