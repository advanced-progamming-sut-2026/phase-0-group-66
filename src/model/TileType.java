package model;

public enum TileType {
    NORMAL(true),
    WATER(false),
    TOMB(false),
    ICE(false),
    SLIPPERY_UP(false),
    SLIPPERY_DOWN(false),
    LOW_TIDE(true),
    NECROMANCY(true);

    private final boolean plantable;

    TileType(boolean plantable) {
        this.plantable = plantable;
    }

    public boolean isPlantable() {
        return plantable;
    }

    public static TileType fromText(String value) {
        if (value == null || value.isBlank()) {
            return NORMAL;
        }
        String normalized = value.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        if (normalized.equals("SLIPPERY")) {
            return SLIPPERY_UP;
        }
        return TileType.valueOf(normalized);
    }
}
