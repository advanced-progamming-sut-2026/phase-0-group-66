package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public abstract class Zombie {
    protected final ZombieDefinition definition;
    protected String name;
    protected int health;
    protected double speed;
    protected int damage;
    protected int waveCost;
    protected BoardPosition position;
    private final ArrayList<Armor> armors;
    private int chilledTicks;
    private boolean iceImmune;
    private boolean rewardDropped;
    private boolean glowing;
    private boolean difficultyApplied;
    private int specialAbilityUses;

    protected Zombie(ZombieDefinition definition, List<Armor> armors) {
        if (definition == null) {
            throw new IllegalArgumentException("Zombie definition cannot be null.");
        }
        this.definition = definition;
        this.name = definition.getDisplayName();
        this.health = definition.getHitpoints();
        this.speed = definition.getSpeed();
        this.damage = definition.getEatDamagePerSecond();
        this.waveCost = definition.getWavePointCost();
        this.armors = new ArrayList<>();
        if (armors != null) {
            this.armors.addAll(armors);
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
        if (damage > 0) {
            damage = Math.max(1, (int) Math.round(damage * factor));
        }
        speed *= factor;
        for (Armor armor : armors) {
            armor.scaleHealth(factor);
        }
        difficultyApplied = true;
    }

    public boolean isGlowing() { return glowing; }
    public void setGlowing(boolean glowing) { this.glowing = glowing; }

    public void move() {
        moveOneTick();
    }

    public void moveOneTick() {
        if (position != null) {
            double actualSpeed = chilledTicks > 0 ? speed * 0.5 : speed;
            position = position.moveHorizontal(-actualSpeed / Game.TICKS_PER_SECOND);
        }
    }

    public void tickEffects() {
        if (chilledTicks > 0) {
            chilledTicks--;
        }
    }

    public void chill(int ticks) {
        if (!iceImmune) {
            chilledTicks = Math.max(chilledTicks, Math.max(0, ticks));
        }
    }

    public void clearChill() {
        chilledTicks = 0;
    }

    public int getChilledTicks() {
        return chilledTicks;
    }

    public boolean isIceImmune() { return iceImmune; }
    public void setIceImmune(boolean iceImmune) { this.iceImmune = iceImmune; }

    public void attackPlant(Plant target) {
        if (target != null) {
            target.takeDamage(damage);
        }
    }

    public void takeDamage(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Damage cannot be negative.");
        }
        int remainingDamage = applyArmorDamage(amount);
        health = Math.max(0, health - remainingDamage);
    }

    public void takeDirectDamage(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Damage cannot be negative.");
        }
        health = Math.max(0, health - amount);
    }

    public void kill() {
        health = 0;
        armors.clear();
    }

    public void dropReward() {
        rewardDropped = true;
    }

    public void specialAbility() {
        specialAbilityUses++;
    }

    public boolean isRewardDropped() { return rewardDropped; }
    public int getSpecialAbilityUses() { return specialAbilityUses; }

    public boolean isDead() {
        return health <= 0;
    }

    public boolean isBoss() {
        String alias = definition.getAlias().toLowerCase();
        return alias.contains("zomboss") || alias.contains("boss");
    }

    public int getEffectiveHealth() {
        int total = health;
        for (Armor armor : armors) {
            total += armor.getHealth();
        }
        return total;
    }

    public ZombieDefinition getDefinition() {
        return definition;
    }

    public String getName() {
        return name;
    }

    public int getHealth() {
        return health;
    }

    public double getSpeed() {
        return speed;
    }

    public int getDamage() {
        return damage;
    }

    public int getWaveCost() {
        return waveCost;
    }

    public List<Armor> getArmors() {
        return Collections.unmodifiableList(armors);
    }

    public BoardPosition getPosition() {
        return position;
    }

    public void setPosition(BoardPosition position) {
        this.position = position;
    }

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
