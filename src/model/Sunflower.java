package model;

public class Sunflower extends Plant {
    private final int productionAmount;

    public Sunflower(PlantDefinition definition) {
        this(definition, 1);
    }

    public Sunflower(PlantDefinition definition, int level) {
        super(definition, level);
        int configured = definition.getAbilityParameterInt("sun",
            (int) Math.round(definition.getAbilityPower()));
        int baseAmount = Math.max(0, configured);
        productionAmount = baseAmount
            + PlantStats.calculate(definition, level).getSunProductionBonus();
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
