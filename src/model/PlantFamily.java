package model;

/** Stable plant family identifiers used by data files and gameplay rules. */
public enum PlantFamily {
    SUN_PRODUCER("Sun Producer"),
    SHOOTER("Shooter"),
    HOMING("Homing"),
    STRIKE_THROUGH("Strike-through"),
    LOBBER("Lobber"),
    EXPLOSIVE("Explosive"),
    MELEE("Melee"),
    WALL_NUT("Wall-nut"),
    MODIFIER("Modifier");

    private final String displayName;

    PlantFamily(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
