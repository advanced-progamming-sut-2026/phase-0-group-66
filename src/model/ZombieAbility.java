package model;

public enum ZombieAbility {
    BASIC,
    ARMORED,
    GARGANTUAR,
    IMP,
    RA,
    EXPLORER,
    TOMB_RAISER,
    DODO_RIDER,
    HUNTER,
    TROGLOBITE,
    FISHERMAN,
    OCTOPUS,
    SNORKEL,
    JUGGLER,
    WIZARD,
    KING,
    DRAGON_IMP,
    ALL_STAR,
    PARASOL,
    TURQUOISE_SKULL,
    PROSPECTOR,
    PIANIST,
    NEWSPAPER,
    ARCADE,
    BARREL_ROLLER,
    GENERIC;

    public static ZombieAbility fromDefinition(ZombieDefinition definition) {
        return definition == null ? GENERIC : definition.getAbility();
    }
}
