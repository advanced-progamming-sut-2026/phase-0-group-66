package model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ZombieDataLoader {
    private static final Map<String, String> DISPLAY_NAMES = createDisplayNames();

    public List<ZombieDefinition> load(Path path) throws IOException {
        Object root = SimpleJsonParser.parse(path);
        List<Object> entries = requireList(root, "zombie root");
        ArrayList<ZombieDefinition> definitions = new ArrayList<>();
        for (int index = 0; index < entries.size(); index++) {
            try {
                definitions.add(parseDefinition(requireMap(entries.get(index), "zombie entry")));
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid zombie data at item " + (index + 1)
                    + ": " + exception.getMessage(), exception);
            }
        }
        return List.copyOf(definitions);
    }

    private ZombieDefinition parseDefinition(Map<String, Object> entry) {
        String alias = firstString(requireList(entry.get("aliases"), "aliases"));
        Map<String, Object> data = requireMap(entry.get("objdata"), "objdata");
        int hitpoints = toInt(data.get("Hitpoints"), "Hitpoints");
        int eatDamage = toInt(data.get("EatDPS"), "EatDPS");
        double speed = toDouble(data.get("Speed"), "Speed");
        int waveCost = toInt(data.get("WavePointCost"), "WavePointCost");
        int weight = toInt(data.get("Weight"), "Weight");
        boolean canSpawnPlantFood = toBoolean(data.get("CanSpawnPlantFood"));
        List<String> armorAliases = parseArmorAliases(data.get("ZombieArmorProps"));
        Map<String, Object> specialProperties = extractSpecialProperties(data);
        String displayName = DISPLAY_NAMES.getOrDefault(alias, readableAlias(alias));
        return new ZombieDefinition(alias, displayName, hitpoints, eatDamage, speed, waveCost,
            weight, canSpawnPlantFood, armorAliases, specialProperties);
    }

    private Map<String, Object> extractSpecialProperties(Map<String, Object> data) {
        LinkedHashMap<String, Object> properties = new LinkedHashMap<>(data);
        properties.remove("Hitpoints");
        properties.remove("EatDPS");
        properties.remove("Speed");
        properties.remove("WavePointCost");
        properties.remove("Weight");
        properties.remove("CanSpawnPlantFood");
        properties.remove("ZombieArmorProps");
        properties.remove("ArtCenter");
        properties.remove("AttackRect");
        properties.remove("HitRect");
        properties.remove("ScaledProps");
        properties.remove("ShadowOffset");
        properties.remove("ZombieStats");
        return properties;
    }

    private List<String> parseArmorAliases(Object value) {
        if (value == null) {
            return List.of();
        }
        ArrayList<String> aliases = new ArrayList<>();
        for (Object item : requireList(value, "ZombieArmorProps")) {
            String reference = requireString(item, "armor reference");
            aliases.add(extractRtidAlias(reference));
        }
        return aliases;
    }

    private String extractRtidAlias(String reference) {
        int openParenthesis = reference.indexOf('(');
        int atSign = reference.indexOf('@');
        if (openParenthesis >= 0 && atSign > openParenthesis) {
            return reference.substring(openParenthesis + 1, atSign);
        }
        return reference;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> requireMap(Object value, String fieldName) {
        if (!(value instanceof Map<?, ?>)) {
            throw new IllegalArgumentException(fieldName + " must be an object.");
        }
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Object> requireList(Object value, String fieldName) {
        if (!(value instanceof List<?>)) {
            throw new IllegalArgumentException(fieldName + " must be an array.");
        }
        return (List<Object>) value;
    }

    private String firstString(List<Object> values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("aliases cannot be empty.");
        }
        return requireString(values.get(0), "alias");
    }

    private String requireString(Object value, String fieldName) {
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must be a non-empty string.");
        }
        return text.trim();
    }

    private int toInt(Object value, String fieldName) {
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(fieldName + " must be numeric.");
        }
        return number.intValue();
    }

    private double toDouble(Object value, String fieldName) {
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(fieldName + " must be numeric.");
        }
        return number.doubleValue();
    }

    private boolean toBoolean(Object value) {
        return value instanceof Boolean booleanValue && booleanValue;
    }

    private String readableAlias(String alias) {
        String withoutPrefix = alias.startsWith("Zombie") ? alias.substring("Zombie".length()) : alias;
        return withoutPrefix.replaceAll("(?<=[a-z])(?=[A-Z])", " ").trim() + " Zombie";
    }

    private static Map<String, String> createDisplayNames() {
        LinkedHashMap<String, String> names = new LinkedHashMap<>();
        names.put("ZombieDefault", "Basic Zombie");
        names.put("ZombieArmor1", "Conehead Zombie");
        names.put("ZombieArmor2", "Buckethead Zombie");
        names.put("ZombieArmor4", "Brickhead Zombie");
        names.put("ZombieDarkArmor3", "Knight Zombie");
        names.put("ZombieGargantuar", "Gargantuar");
        names.put("ZombieImp", "Imp");
        names.put("ZombieRa", "Ra Zombie");
        names.put("ZombieExplorer", "Explorer Zombie");
        names.put("ZombieTombRaiser", "Tomb Raiser Zombie");
        names.put("ZombieIceAgeDodo", "Dodo Rider Zombie");
        names.put("ZombieIceAgeHunter", "Hunter Zombie");
        names.put("ZombieIceAgeTroglobite", "Troglobite");
        names.put("ZombieBeachFisherman", "Fisherman Zombie");
        names.put("ZombieBeachOctopus", "Octopus Zombie");
        names.put("ZombieBeachSnorkel", "Snorkel Zombie");
        names.put("ZombieDarkJuggler", "Juggler Zombie");
        names.put("ZombieWizard", "Wizard Zombie");
        names.put("ZombieDarkKing", "King Zombie");
        names.put("ZombieDarkImpDragon", "Dragon Imp");
        names.put("ZombieModernAllStar", "All-Star Zombie");
        names.put("ZombieLostCityJane", "Parasol Zombie");
        names.put("ZombieCrystalSkull", "Turquoise Skull Zombie");
        names.put("ZombieProspector", "Prospector Zombie");
        names.put("ZombiePiano", "Pianist Zombie");
        names.put("ZombieNewspaper", "Newspaper Zombie");
        names.put("ZombieArcade", "Arcade Zombie");
        return Map.copyOf(names);
    }
}
