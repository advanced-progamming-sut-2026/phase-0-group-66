package model;

public class WallNut extends Plant {
    private final int defenseBonus;

    public WallNut(PlantDefinition definition) {
        super(definition);
        defenseBonus = definition.getBaseHealth();
    }

    public void block() {
        healToFull();
    }

    public int getDefenseBonus() {
        return defenseBonus;
    }
}
