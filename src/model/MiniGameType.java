package model;

import java.util.Locale;

public enum MiniGameType {
    VASEBREAKER,
    WALLNUT_BOWLING,
    I_ZOMBIE,
    BEGHOULD,
    ZOMBOTANY;

    public static MiniGameType fromText(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Mini-game name is required.");
        }
        String normalized = text.trim().toUpperCase(Locale.ROOT)
            .replace('-', '_').replace(' ', '_').replace(",", "");
        if (normalized.equals("IZOMBIE")) {
            normalized = "I_ZOMBIE";
        }
        if (normalized.equals("WALLNUTBOWLING")) {
            normalized = "WALLNUT_BOWLING";
        }
        return valueOf(normalized);
    }
}
