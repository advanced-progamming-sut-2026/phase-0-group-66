package model;

import java.util.Locale;

/** Identifies the gameplay behavior attached to a plant definition. */
public enum PlantAbility {
    SUNFLOWER,
    TWIN_SUNFLOWER,
    SUN_SHROOM,
    PRIMAL_SUNFLOWER,
    GOLD_BLOOM,
    BASIC_SHOOTER,
    REPEATER,
    THREEPEATER,
    SNOW_PEA,
    ROTOBAGA,
    PEA_POD,
    SPLIT_PEA,
    CITRON,
    CAULIPOWER,
    ELECTRIC_BLUEBERRY,
    BOWLING_BULB,
    CACTUS,
    FIRE_PEASHOOTER,
    STARFRUIT,
    GOO_PEASHOOTER,
    MEGA_GATLING_PEA,
    SHORT_RANGE_SHROOM,
    FUME_SHROOM,
    CABBAGE_PULT,
    KERNEL_PULT,
    MELON_PULT,
    WINTER_MELON,
    PEPPER_PULT,
    POTATO_MINE,
    PRIMAL_POTATO_MINE,
    CHERRY_BOMB,
    SQUASH,
    GRAPESHOT,
    JALAPENO,
    DOOM_SHROOM,
    TANGLE_KELP,
    ICEBERG_LETTUCE,
    BONK_CHOY,
    PHAT_BEET,
    CHOMPER,
    WASABI_WHIP,
    KIWIBEAST,
    WALL_NUT,
    TALL_NUT,
    ENDURIAN,
    GARLIC,
    SWEET_POTATO,
    EXPLODE_O_NUT,
    PUMPKIN,
    SUN_BEAN,
    TORCHWOOD,
    MAGNET_SHROOM,
    HYPNO_SHROOM,
    CAT_TAIL,
    IMITATER,
    ICE_SHROOM,
    LILY_PAD,
    HOT_POTATO,
    GRAVE_BUSTER,
    ENLIGHTEN_MINT,
    APPEASE_MINT,
    ARMA_MINT,
    BOMBARD_MINT,
    ENFORCE_MINT,
    REINFORCE_MINT,
    ENCHANT_MINT,
    PIERCE_MINT,
    CATTAIL_MINT,
    GENERIC;

    public static PlantAbility fromDefinition(PlantDefinition definition) {
        if (definition == null) {
            return GENERIC;
        }
        return switch (definition.getNormalizedName()) {
            case "sunflower" -> SUNFLOWER;
            case "twinsunflower" -> TWIN_SUNFLOWER;
            case "sunshroom" -> SUN_SHROOM;
            case "primalsunflower" -> PRIMAL_SUNFLOWER;
            case "goldbloom" -> GOLD_BLOOM;
            case "peashooter" -> BASIC_SHOOTER;
            case "repeater" -> REPEATER;
            case "threepeater" -> THREEPEATER;
            case "snowpea" -> SNOW_PEA;
            case "rotobaga" -> ROTOBAGA;
            case "peapod" -> PEA_POD;
            case "splitpea" -> SPLIT_PEA;
            case "citron" -> CITRON;
            case "caulipower" -> CAULIPOWER;
            case "electricblueberry" -> ELECTRIC_BLUEBERRY;
            case "bowlingbulb" -> BOWLING_BULB;
            case "cactus" -> CACTUS;
            case "firepeashooter" -> FIRE_PEASHOOTER;
            case "starfruit" -> STARFRUIT;
            case "goopeashooter" -> GOO_PEASHOOTER;
            case "megagatlingpea" -> MEGA_GATLING_PEA;
            case "seashroom", "puffshroom" -> SHORT_RANGE_SHROOM;
            case "fumeshroom" -> FUME_SHROOM;
            case "cabbagepult" -> CABBAGE_PULT;
            case "kernelpult" -> KERNEL_PULT;
            case "melonpult" -> MELON_PULT;
            case "wintermelon" -> WINTER_MELON;
            case "pepperpult" -> PEPPER_PULT;
            case "potatomine" -> POTATO_MINE;
            case "primalpotatomine" -> PRIMAL_POTATO_MINE;
            case "cherrybomb" -> CHERRY_BOMB;
            case "squash" -> SQUASH;
            case "grapeshot" -> GRAPESHOT;
            case "jalapeno" -> JALAPENO;
            case "doomshroom" -> DOOM_SHROOM;
            case "tanglekelp" -> TANGLE_KELP;
            case "iceberglettuce" -> ICEBERG_LETTUCE;
            case "bonkchoy" -> BONK_CHOY;
            case "phatbeet" -> PHAT_BEET;
            case "chomper" -> CHOMPER;
            case "wasabiwhip" -> WASABI_WHIP;
            case "kiwibeast" -> KIWIBEAST;
            case "wallnut" -> WALL_NUT;
            case "tallnut" -> TALL_NUT;
            case "endurian" -> ENDURIAN;
            case "garlic" -> GARLIC;
            case "sweetpotato" -> SWEET_POTATO;
            case "explodeonut" -> EXPLODE_O_NUT;
            case "pumpkin" -> PUMPKIN;
            case "sunbean" -> SUN_BEAN;
            case "torchwood" -> TORCHWOOD;
            case "magnetshroom" -> MAGNET_SHROOM;
            case "hypnoshroom" -> HYPNO_SHROOM;
            case "cattail" -> CAT_TAIL;
            case "imitater" -> IMITATER;
            case "iceshroom" -> ICE_SHROOM;
            case "lilypad" -> LILY_PAD;
            case "hotpotato" -> HOT_POTATO;
            case "gravebuster" -> GRAVE_BUSTER;
            case "enlightenmint" -> ENLIGHTEN_MINT;
            case "appeasemint" -> APPEASE_MINT;
            case "armamint" -> ARMA_MINT;
            case "bombardmint" -> BOMBARD_MINT;
            case "enforcemint" -> ENFORCE_MINT;
            case "reinforcemint" -> REINFORCE_MINT;
            case "enchantmint" -> ENCHANT_MINT;
            case "piercemint" -> PIERCE_MINT;
            case "cattailmint" -> CATTAIL_MINT;
            default -> inferGeneric(definition);
        };
    }

    private static PlantAbility inferGeneric(PlantDefinition definition) {
        String category = definition.getCategory().toLowerCase(Locale.ROOT);
        if (category.contains("sun producer")) {
            return SUNFLOWER;
        }
        if (category.contains("lobber")) {
            return CABBAGE_PULT;
        }
        if (category.contains("homing")) {
            return CAT_TAIL;
        }
        if (category.contains("shooter") || category.contains("strike")) {
            return BASIC_SHOOTER;
        }
        if (category.contains("melee")) {
            return BONK_CHOY;
        }
        return GENERIC;
    }

    public boolean isMint() {
        return name().endsWith("MINT");
    }
}
