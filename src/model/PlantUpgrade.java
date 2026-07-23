package model;

import java.util.Objects;

/** One typed level upgrade from plants.json. */
public final class PlantUpgrade {
    private final int level;
    private final PlantUpgradeType effect;
    private final double amount;
    private final String trait;

    public PlantUpgrade(int level, PlantUpgradeType effect, double amount, String trait) {
        if (level < 2) {
            throw new IllegalArgumentException("Upgrade level must be at least 2.");
        }
        if (effect == null) {
            throw new IllegalArgumentException("Upgrade effect cannot be null.");
        }
        this.level = level;
        this.effect = effect;
        this.amount = amount;
        this.trait = trait == null ? "" : trait.trim();
        if (effect == PlantUpgradeType.ENABLE_TRAIT && this.trait.isEmpty()) {
            throw new IllegalArgumentException("ENABLE_TRAIT requires a trait name.");
        }
    }

    public int getLevel() { return level; }
    public PlantUpgradeType getEffect() { return effect; }
    public double getAmount() { return amount; }
    public String getTrait() { return trait; }

    @Override
    public String toString() {
        if (effect == PlantUpgradeType.ENABLE_TRAIT) {
            return "level " + level + ": enable " + trait;
        }
        return "level " + level + ": " + effect + " " + amount;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PlantUpgrade upgrade)) {
            return false;
        }
        return level == upgrade.level && Double.compare(amount, upgrade.amount) == 0
            && effect == upgrade.effect && trait.equals(upgrade.trait);
    }

    @Override
    public int hashCode() {
        return Objects.hash(level, effect, amount, trait);
    }
}
