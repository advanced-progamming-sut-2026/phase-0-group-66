package model;

import java.util.List;

final class BeghouledBoardRenderer {
    private BeghouledBoardRenderer() {
    }

    static String render(BeghouledCombatPlant[][] grid, boolean[][] crater,
                         List<MiniGameUnit> zombies, int sun, int matches, int target) {
        StringBuilder builder = new StringBuilder(
            "Beghouled board (#=crater, Z=nearest zombie marker)\n");
        for (int row = 0; row < grid.length; row++) {
            appendRow(builder, grid, crater, zombies, row);
        }
        builder.append("Legend: P=Pea line, W=Wall line, F=Fume line, C=Cabbage, M=Melon\n")
            .append("Sun=").append(sun).append(", matches=").append(matches)
            .append('/').append(target);
        return builder.toString();
    }

    private static void appendRow(StringBuilder builder, BeghouledCombatPlant[][] grid,
                                  boolean[][] crater, List<MiniGameUnit> zombies, int row) {
        for (int col = 0; col < grid[row].length; col++) {
            if (hasZombieAt(zombies, row, col)) {
                builder.append("Z ");
            } else if (crater[row][col]) {
                builder.append("# ");
            } else {
                builder.append(symbol(typeAt(grid, row, col))).append(' ');
            }
        }
        builder.append('\n');
    }

    private static boolean hasZombieAt(List<MiniGameUnit> zombies, int row, int col) {
        for (MiniGameUnit zombie : zombies) {
            if (!zombie.isDead() && zombie.getRow() == row
                && Math.round(zombie.getColumn()) == col) {
                return true;
            }
        }
        return false;
    }

    private static String typeAt(BeghouledCombatPlant[][] grid, int row, int col) {
        return grid[row][col] == null ? null : grid[row][col].type;
    }

    private static char symbol(String type) {
        if (type == null) {
            return '.';
        }
        return switch (PlantDefinition.normalizeKey(type)) {
            case "peashooter", "repeater", "megagatlingpea" -> 'P';
            case "wallnut", "tallnut" -> 'W';
            case "puffshroom", "fumeshroom" -> 'F';
            case "cabbagepult" -> 'C';
            case "melonpult", "wintermelon" -> 'M';
            default -> '?';
        };
    }
}
