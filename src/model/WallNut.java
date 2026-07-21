package model;

public class WallNut extends Plant {
    private final int defenseBonus;

    public WallNut(PlantDefinition definition) {
        this(definition, 1);
    }

    public WallNut(PlantDefinition definition, int level) {
        super(definition, level);
        defenseBonus = PlantStats.calculate(definition, level).getMaxHealth();
    }

    public void block() { healToFull(); }
    public int getDefenseBonus() { return defenseBonus; }
}
