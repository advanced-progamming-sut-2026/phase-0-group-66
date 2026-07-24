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
        if (normalized.equals("BEGHOULED")) {
            normalized = "BEGHOULD";
        }
        return valueOf(normalized);
    }

    @Override
    public String toString() {
        return this == BEGHOULD ? "BEGHOULED" : name();
    }
}
