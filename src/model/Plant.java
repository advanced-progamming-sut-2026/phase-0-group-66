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
    private int actionIntervalTicks;
    private int rechargeTicks;
    private final int sunProductionBonus;
    private final boolean doubleSunChance;
    private final int chillDurationTicks;
    private final int pierceBonus;
    private final PlantAbility ability;
    private final Map<String, Double> upgradeTraits;
    private int actionTicksRemaining;
    private int actionSequence;
    private int plantFoodShield;
    private int coverShield;
    int ageTicks;
    private int stackCount;
    int disabledTicks;
    int digestionTicks;
    int iceHits;
    int frozenHealth;
    int octopusHealth;
    int armTicksRemaining;
    int lifetimeTicksRemaining;
    int cyanBulbTicks;
    int blueBulbTicks;
    int orangeBulbTicks;
    private int reflectDamageMultiplier = 1;
    private int blueFlameMultiplier = 2;
    private int explosiveShieldDamage;
    private boolean shieldExplosionPending;
    private boolean hypnoGargantuarReady;
    int mintAuraTicksRemaining;
    private final Set<Plant> mintEmpoweredPlants = Collections.newSetFromMap(
        new IdentityHashMap<>());
    String transformedBy;
    private boolean trappedInIceTile;
    private boolean difficultyTimingApplied;

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
            int seconds = Math.min(configured, Math.max(1, (int) Math.round(
                actionIntervalTicks / (double) Game.TICKS_PER_SECOND)));
            armTicksRemaining = seconds * Game.TICKS_PER_SECOND;
        }
        if (ability == PlantAbility.SHORT_RANGE_SHROOM) {
            int seconds = definition.getAbilityParameterInt("lifetimeSeconds", 60)
                + getUpgradeTraitInt("LIFESPAN_10S", 0);
            lifetimeTicksRemaining = Math.max(1, seconds) * Game.TICKS_PER_SECOND;
        }
    }

    public void applyDifficultyTiming(int difficultyLevel) {
        if (difficultyTimingApplied) {
            return;
        }
        actionIntervalTicks = DifficultyScaling.scaleDurationTicks(
            actionIntervalTicks, difficultyLevel);
        rechargeTicks = DifficultyScaling.scaleDurationTicks(rechargeTicks, difficultyLevel);
        actionTicksRemaining = actionIntervalTicks;
        difficultyTimingApplied = true;
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
        PlantRuntimeSystem.tick(this);
    }

    public boolean tickActionTimer() {
        if (actionTicksRemaining > 0) {
            actionTicksRemaining--;
            if (actionTicksRemaining == 0) {
                actionSequence++;
            }
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
        return PlantRuntimeSystem.growthStage(this);
    }

    public int getEffectiveAttackPower() {
        return PlantRuntimeSystem.effectiveAttackPower(this);
    }

    public int getSunShroomProduction() {
        return PlantRuntimeSystem.sunShroomProduction(this);
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
        PlantRuntimeSystem.addIceLayer(this);
    }

    public void freezeImmediately() {
        PlantRuntimeSystem.freezeImmediately(this);
    }

    public void damageIce(int damage, boolean fire) {
        PlantRuntimeSystem.damageIce(this, damage, fire);
    }

    public void coverWithOctopus() {
        octopusHealth = Math.max(octopusHealth, 600);
    }

    public void damageOctopus(int damage) {
        octopusHealth = Math.max(0, octopusHealth - Math.max(0, damage));
    }

    public void transformByWizard(String wizardId) {
        PlantRuntimeSystem.transformByWizard(this, wizardId);
    }

    public void releaseWizardTransformation(String wizardId) {
        PlantRuntimeSystem.releaseWizardTransformation(this, wizardId);
    }

    public void clearControlEffects() {
        PlantRuntimeSystem.clearControlEffects(this);
    }

    public void matureFully() {
        PlantRuntimeSystem.matureFully(this);
    }

    public void restoreLifetime() {
        PlantRuntimeSystem.restoreLifetime(this);
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
        PlantRuntimeSystem.startMintAura(this, ticks);
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
        return PlantRuntimeSystem.nextBowlingBulbDamage(this);
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
    public int getActionSequence() { return actionSequence; }
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
