package model;

public class Peashooter extends Plant {
    private final String projectileType;

    public Peashooter(PlantDefinition definition) {
        super(definition);
        if (definition.hasTag("Poison")) {
            projectileType = "poison";
        } else if (definition.hasTag("Fire")) {
            projectileType = "fire";
        } else if (definition.hasTag("Ice")) {
            projectileType = "ice";
        } else {
            projectileType = "normal";
        }
    }

    public Projectile shoot() {
        GridPosition plantPosition = getPosition();
        BoardPosition start = plantPosition == null
            ? new BoardPosition(0, 0) : new BoardPosition(plantPosition.getRow(),
            plantPosition.getColumn() + 0.25);
        return new Projectile(getAttackPower(), 5.0, start, getProjectileElementType(), isPiercing());
    }

    public String getProjectileTypeName() {
        return projectileType;
    }

    public String getProjectileType() {
        return projectileType;
    }
}
