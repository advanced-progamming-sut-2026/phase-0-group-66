package model;

public class Sunflower extends Plant {
    private final int productionAmount;

    public Sunflower(PlantDefinition definition) {
        this(definition, 1);
    }

    public Sunflower(PlantDefinition definition, int level) {
        super(definition, level);
        String normalized = definition.getNormalizedName();
        int baseAmount;
        if (normalized.equals("twinsunflower")) {
            baseAmount = 100;
        } else if (normalized.equals("primalsunflower")) {
            baseAmount = 75;
        } else {
            baseAmount = 50;
        }
        productionAmount = baseAmount + PlantStats.calculate(definition, level).getSunProductionBonus();
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
