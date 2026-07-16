package model;

public class Sunflower extends Plant {
    private final int productionAmount;

    public Sunflower(PlantDefinition definition) {
        super(definition);
        productionAmount = definition.getName().equalsIgnoreCase("Twin Sunflower") ? 100 : 50;
    }

    public Sun produceSun() {
        return new Sun();
    }

    public int getProductionAmount() {
        return productionAmount;
    }
}
