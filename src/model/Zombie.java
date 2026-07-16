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

    public void move() {
        if (position != null) {
            position = position.moveHorizontal(-speed);
        }
    }

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

    public void dropReward() {
    }

    public void specialAbility() {
    }

    public boolean isDead() {
        return health <= 0;
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
