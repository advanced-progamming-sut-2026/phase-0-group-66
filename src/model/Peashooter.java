package model;

public class Peashooter extends Plant {
    private final String projectileType;

    public Peashooter(PlantDefinition definition) {
        super(definition);
        projectileType = definition.hasTag("Fire") ? "fire-pea" : "pea";
    }

    public Projectile shoot() {
        return new Projectile();
    }

    public String getProjectileType() {
        return projectileType;
    }
}
