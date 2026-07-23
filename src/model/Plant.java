package model;

import java.util.Locale;

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
    private final int rechargeTicks;
    private final int sunProductionBonus;
    private final boolean doubleSunChance;
    private final int chillDurationTicks;
    private final int pierceBonus;
    private final PlantAbility ability;
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
    private String transformedBy;

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
        this.chillDurationTicks = 50 + stats.getChillBonusTicks();
        this.pierceBonus = stats.getPierceBonus();
        this.ability = PlantAbility.fromDefinition(definition);
        this.actionTicksRemaining = actionIntervalTicks;
        this.stackCount = 1;
        initializeRuntimeState();
    }

    private void initializeRuntimeState() {
        if (ability == PlantAbility.POTATO_MINE) {
            armTicksRemaining = 15 * Game.TICKS_PER_SECOND;
        } else if (ability == PlantAbility.PRIMAL_POTATO_MINE) {
            armTicksRemaining = 5 * Game.TICKS_PER_SECOND;
        }
        if (ability == PlantAbility.SHORT_RANGE_SHROOM) {
            lifetimeTicksRemaining = PUFF_LIFETIME_TICKS;
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
        remaining = absorbPlantFoodShield(remaining);
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
        int shield = switch (ability) {
            case WALL_NUT -> 4000;
            case TALL_NUT -> 8000;
            case ENDURIAN, EXPLODE_O_NUT, PUMPKIN, SUN_BEAN -> Math.max(4000, maxHealth);
            default -> maxHealth;
        };
        plantFoodShield = Math.max(plantFoodShield, shield);
        actionTicksRemaining = 0;
        if (ability == PlantAbility.POTATO_MINE
            || ability == PlantAbility.PRIMAL_POTATO_MINE) {
            armTicksRemaining = 0;
        }
        if (ability == PlantAbility.SHORT_RANGE_SHROOM) {
            lifetimeTicksRemaining = PUFF_LIFETIME_TICKS;
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
        return health <= 0;
    }

    public boolean isOperational() {
        return !isDestroyed() && disabledTicks <= 0 && digestionTicks <= 0
            && frozenHealth <= 0 && octopusHealth <= 0 && transformedBy == null;
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
        if (ageTicks < 24 * Game.TICKS_PER_SECOND) {
            return 1;
        }
        if (ageTicks < 72 * Game.TICKS_PER_SECOND) {
            return 2;
        }
        return 3;
    }

    public int getEffectiveAttackPower() {
        if (ability == PlantAbility.KIWIBEAST) {
            return Math.max(1, attackPower) * getGrowthStage();
        }
        return attackPower;
    }

    public int getSunShroomProduction() {
        return switch (getGrowthStage()) {
            case 1 -> 25;
            case 2 -> 50;
            default -> 75;
        };
    }

    public boolean addStack() {
        if (ability != PlantAbility.PEA_POD || stackCount >= 5) {
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
        ageTicks = Math.max(ageTicks, 72 * Game.TICKS_PER_SECOND);
    }

    public void restoreLifetime() {
        if (ability == PlantAbility.SHORT_RANGE_SHROOM) {
            lifetimeTicksRemaining = PUFF_LIFETIME_TICKS;
        }
    }

    public PlantAbility getAbility() { return ability; }
    public int getPlantLevel() { return plantLevel; }
    public int getActionIntervalTicks() { return actionIntervalTicks; }
    public int getActionTicksRemaining() { return actionTicksRemaining; }
    public int getRechargeTicks() { return rechargeTicks; }
    public int getSunProductionBonus() { return sunProductionBonus; }
    public boolean hasDoubleSunChance() { return doubleSunChance; }
    public int getChillDurationTicks() { return chillDurationTicks; }
    public int getPlantFoodShield() { return plantFoodShield; }
    public int getCoverShield() { return coverShield; }
    public int getAgeTicks() { return ageTicks; }
    public int getStackCount() { return stackCount; }
    public int getDisabledTicks() { return disabledTicks; }
    public int getDigestionTicks() { return digestionTicks; }
    public int getIceHits() { return iceHits; }
    public int getFrozenHealth() { return frozenHealth; }
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
