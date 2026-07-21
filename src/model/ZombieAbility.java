package model;

/** Identifies the special gameplay behavior of a zombie. */
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
    GENERIC;

    public static ZombieAbility fromDefinition(ZombieDefinition definition) {
        if (definition == null) {
            return GENERIC;
        }
        return switch (definition.getAlias()) {
            case "ZombieDefault" -> BASIC;
            case "ZombieArmor1", "ZombieArmor2", "ZombieArmor4", "ZombieDarkArmor3" -> ARMORED;
            case "ZombieGargantuar" -> GARGANTUAR;
            case "ZombieImp" -> IMP;
            case "ZombieRa" -> RA;
            case "ZombieExplorer" -> EXPLORER;
            case "ZombieTombRaiser" -> TOMB_RAISER;
            case "ZombieIceAgeDodo" -> DODO_RIDER;
            case "ZombieIceAgeHunter" -> HUNTER;
            case "ZombieIceAgeTroglobite" -> TROGLOBITE;
            case "ZombieBeachFisherman" -> FISHERMAN;
            case "ZombieBeachOctopus" -> OCTOPUS;
            case "ZombieBeachSnorkel" -> SNORKEL;
            case "ZombieDarkJuggler" -> JUGGLER;
            case "ZombieWizard" -> WIZARD;
            case "ZombieDarkKing" -> KING;
            case "ZombieDarkImpDragon" -> DRAGON_IMP;
            case "ZombieModernAllStar" -> ALL_STAR;
            case "ZombieLostCityJane" -> PARASOL;
            case "ZombieCrystalSkull" -> TURQUOISE_SKULL;
            case "ZombieProspector" -> PROSPECTOR;
            case "ZombiePiano" -> PIANIST;
            case "ZombieNewspaper" -> NEWSPAPER;
            case "ZombieArcade" -> ARCADE;
            default -> GENERIC;
        };
    }
}
