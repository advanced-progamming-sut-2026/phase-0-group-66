package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Zombie {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    protected final ZombieDefinition definition;
    protected String name;
    protected int health;
    private int maximumHealth;
    protected double speed;
    protected int damage;
    protected int waveCost;
    protected BoardPosition position;
    private final ArrayList<Armor> armors;
    private final ZombieAbility ability;
    private final String runtimeId;
    private int chilledTicks;
    private int stunnedTicks;
    private int poisonTicks;
    private int poisonDamagePerSecond;
    private int ageTicks;
    private int abilityCooldownTicks;
    private int stolenSun;
    private int bonusArmorHealth;
    private boolean iceImmune;
    private boolean rewardDropped;
    private boolean glowing;
    private boolean difficultyApplied;
    private boolean hypnotized;
    private boolean reversed;
    private boolean submerged;
    private boolean enraged;
    private boolean specialDisabled;
    private boolean impThrown;
    private boolean chargeUsed;
    private boolean machineActive;
    private int specialAbilityUses;

    protected Zombie(ZombieDefinition definition, List<Armor> armors) {
        if (definition == null) {
            throw new IllegalArgumentException("Zombie definition cannot be null.");
        }
        this.definition = definition;
        this.name = definition.getDisplayName();
        this.health = definition.getHitpoints();
        this.maximumHealth = this.health;
        this.speed = definition.getSpeed();
        this.damage = definition.getEatDamagePerSecond();
        this.waveCost = definition.getWavePointCost();
        this.armors = new ArrayList<>();
        if (armors != null) {
            this.armors.addAll(armors);
        }
        this.ability = ZombieAbility.fromDefinition(definition);
        this.runtimeId = definition.getAlias() + "-" + NEXT_ID.getAndIncrement();
        this.machineActive = ability == ZombieAbility.ARCADE;
        if (machineActive) {
            this.bonusArmorHealth = 1100;
        }
    }

    public void applyDifficulty(int difficultyLevel) {
        if (difficultyApplied) {
            return;
        }
        if (difficultyLevel < 1 || difficultyLevel > 5) {
            throw new IllegalArgumentException("Difficulty level must be between 1 and 5.");
        }
        double factor = difficultyLevel / 3.0;
        health = Math.max(1, (int) Math.round(health * factor));
        maximumHealth = health;
        if (damage > 0) {
            damage = Math.max(1, (int) Math.round(damage * factor));
        }
        speed *= factor;
        for (Armor armor : armors) {
            armor.scaleHealth(factor);
        }
        difficultyApplied = true;
    }

    public void move() {
        moveOneTick();
    }

    public void moveOneTick() {
        if (position == null || stunnedTicks > 0) {
            return;
        }
        double actualSpeed = chilledTicks > 0 ? speed * 0.5 : speed;
        if (ability == ZombieAbility.ALL_STAR && !chargeUsed) {
            actualSpeed *= 3.0;
        }
        if (enraged) {
            actualSpeed *= 2.0;
        }
        double direction = hypnotized || reversed ? 1.0 : -1.0;
        position = position.moveHorizontal(direction * actualSpeed / Game.TICKS_PER_SECOND);
    }

    public void tickEffects() {
        ageTicks++;
        if (chilledTicks > 0) {
            chilledTicks--;
        }
        if (stunnedTicks > 0) {
            stunnedTicks--;
        }
        if (abilityCooldownTicks > 0) {
            abilityCooldownTicks--;
        }
        if (poisonTicks > 0) {
            poisonTicks--;
            if (ageTicks % Game.TICKS_PER_SECOND == 0) {
                takeDirectDamage(poisonDamagePerSecond);
            }
        }
        updateNewspaperEnrage();
    }

    public void chill(int ticks) {
        if (!iceImmune) {
            chilledTicks = Math.max(chilledTicks, Math.max(0, ticks));
        }
    }

    public void stun(int ticks) {
        stunnedTicks = Math.max(stunnedTicks, Math.max(0, ticks));
    }

    public void poison(int ticks, int damagePerSecond) {
        poisonTicks = Math.max(poisonTicks, Math.max(0, ticks));
        poisonDamagePerSecond = Math.max(poisonDamagePerSecond, Math.max(0, damagePerSecond));
    }

    public void clearChill() {
        chilledTicks = 0;
    }

    public void attackPlant(Plant target) {
        if (target == null || stunnedTicks > 0) {
            return;
        }
        int actualDamage = enraged ? damage * 3 : damage;
        target.takeDamage(actualDamage);
    }

    public void takeDamage(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Damage cannot be negative.");
        }
        int remainingDamage = absorbBonusArmor(amount);
        remainingDamage = applyArmorDamage(remainingDamage);
        health = Math.max(0, health - remainingDamage);
        updateNewspaperEnrage();
    }

    public void takeDirectDamage(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Damage cannot be negative.");
        }
        health = Math.max(0, health - amount);
    }

    public boolean takeProjectileDamage(int amount, ProjectileType type, int chillTicks,
                                        boolean lobbed) {
        ProjectileType actualType = type == null ? ProjectileType.NORMAL : type;
        if (ability == ZombieAbility.DRAGON_IMP && actualType == ProjectileType.FIRE) {
            return false;
        }
        if (ability == ZombieAbility.PARASOL && lobbed) {
            return false;
        }
        if (ability == ZombieAbility.SNORKEL && submerged && !lobbed) {
            return false;
        }
        if (ability == ZombieAbility.EXPLORER) {
            if (actualType == ProjectileType.ICE) {
                specialDisabled = true;
            } else if (actualType == ProjectileType.FIRE) {
                specialDisabled = false;
            }
        }
        if (ability == ZombieAbility.PROSPECTOR && actualType == ProjectileType.ICE) {
            specialDisabled = true;
        }
        if (actualType == ProjectileType.POISON) {
            takeDirectDamage(amount);
            poison(5 * Game.TICKS_PER_SECOND, Math.max(1, amount / 4));
        } else if (actualType == ProjectileType.FIRE) {
            clearChill();
            takeDamage(amount);
        } else {
            takeDamage(amount);
            if (actualType == ProjectileType.ICE) {
                chill(chillTicks);
            }
        }
        return true;
    }

    public void kill() {
        health = 0;
        armors.clear();
        bonusArmorHealth = 0;
    }

    public void hypnotize() {
        hypnotized = true;
        reversed = false;
        clearChill();
    }

    public void reverseDirection() {
        reversed = true;
    }

    public void dropReward() {
        rewardDropped = true;
    }

    public void specialAbility() {
        specialAbilityUses++;
    }

    public void addBonusArmor(int amount) {
        bonusArmorHealth += Math.max(0, amount);
    }

    public int removeMetalArmor() {
        int removedHealth = bonusArmorHealth;
        bonusArmorHealth = 0;
        Iterator<Armor> iterator = armors.iterator();
        while (iterator.hasNext()) {
            Armor armor = iterator.next();
            if (armor.getDefinition().hasFlag("metallic")) {
                removedHealth += armor.getHealth();
                iterator.remove();
            }
        }
        updateNewspaperEnrage();
        return removedHealth;
    }

    public boolean hasMetalArmor() {
        if (bonusArmorHealth > 0) {
            return true;
        }
        for (Armor armor : armors) {
            if (armor.getDefinition().hasFlag("metallic")) {
                return true;
            }
        }
        return false;
    }

    private int absorbBonusArmor(int amount) {
        int absorbed = Math.min(bonusArmorHealth, amount);
        bonusArmorHealth -= absorbed;
        if (ability == ZombieAbility.ARCADE && bonusArmorHealth <= 0) {
            machineActive = false;
        }
        return amount - absorbed;
    }

    private void updateNewspaperEnrage() {
        if (ability == ZombieAbility.NEWSPAPER && !enraged && armors.isEmpty()
            && bonusArmorHealth <= 0) {
            enraged = true;
        }
    }

    public boolean isDead() { return health <= 0; }

    public boolean isBoss() {
        String alias = definition.getAlias().toLowerCase();
        return alias.contains("zomboss") || alias.contains("boss");
    }

    public int getEffectiveHealth() {
        int total = health + bonusArmorHealth;
        for (Armor armor : armors) {
            total += armor.getHealth();
        }
        return total;
    }

    public ZombieDefinition getDefinition() { return definition; }
    public ZombieAbility getAbility() { return ability; }
    public String getRuntimeId() { return runtimeId; }
    public String getName() { return name; }
    public int getHealth() { return health; }
    public int getMaximumHealth() { return maximumHealth; }
    public double getSpeed() { return speed; }
    public int getDamage() { return damage; }
    public int getWaveCost() { return waveCost; }
    public List<Armor> getArmors() { return Collections.unmodifiableList(armors); }
    public BoardPosition getPosition() { return position; }
    public int getChilledTicks() { return chilledTicks; }
    public int getStunnedTicks() { return stunnedTicks; }
    public int getAgeTicks() { return ageTicks; }
    public int getAbilityCooldownTicks() { return abilityCooldownTicks; }
    public int getStolenSun() { return stolenSun; }
    public int getBonusArmorHealth() { return bonusArmorHealth; }
    public boolean isIceImmune() { return iceImmune; }
    public boolean isGlowing() { return glowing; }
    public boolean isRewardDropped() { return rewardDropped; }
    public boolean isHypnotized() { return hypnotized; }
    public boolean isReversed() { return reversed; }
    public boolean isSubmerged() { return submerged; }
    public boolean isEnraged() { return enraged; }
    public boolean isSpecialDisabled() { return specialDisabled; }
    public boolean isImpThrown() { return impThrown; }
    public boolean isChargeUsed() { return chargeUsed; }
    public boolean isMachineActive() { return machineActive; }
    public int getSpecialAbilityUses() { return specialAbilityUses; }

    public void setPosition(BoardPosition position) { this.position = position; }
    public void setIceImmune(boolean iceImmune) { this.iceImmune = iceImmune; }
    public void setGlowing(boolean glowing) { this.glowing = glowing; }
    public void setAbilityCooldownTicks(int ticks) {
        abilityCooldownTicks = Math.max(0, ticks);
    }
    public void addStolenSun(int amount) { stolenSun += Math.max(0, amount); }
    public int takeStolenSun() {
        int amount = stolenSun;
        stolenSun = 0;
        return amount;
    }
    public void setSubmerged(boolean submerged) { this.submerged = submerged; }
    public void setSpecialDisabled(boolean disabled) { specialDisabled = disabled; }
    public void markImpThrown() { impThrown = true; }
    public void markChargeUsed() { chargeUsed = true; }
    public void breakMachine() { machineActive = false; }

    private int applyArmorDamage(int damageAmount) {
        int remainingDamage = damageAmount;
        Iterator<Armor> iterator = armors.iterator();
        while (iterator.hasNext() && remainingDamage > 0) {
            Armor armor = iterator.next();
            remainingDamage = armor.absorbDamage(remainingDamage);
            if (armor.isDestroyed()) {
                iterator.remove();
            }
        }
        return remainingDamage;
    }
}
