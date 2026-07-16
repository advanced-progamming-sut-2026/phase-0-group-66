package model;

public class Sunflower extends Plant {
    private final int productionAmount;

    public Sunflower(PlantDefinition definition) {
        super(definition);
        String normalized = definition.getNormalizedName();
        if (normalized.equals("twinsunflower")) {
            productionAmount = 100;
        } else if (normalized.equals("primalsunflower")) {
            productionAmount = 75;
        } else {
            productionAmount = 50;
        }
    }

    public Sun produceSun() {
        GridPosition plantPosition = getPosition();
        if (plantPosition == null) {
            throw new IllegalStateException("Sunflower is not planted.");
        }
        return new Sun(productionAmount, plantPosition);
    }

    public int getProductionAmount() {
        return productionAmount;
    }
}
