package model;

final class BoardRenderer {
    private BoardRenderer() {
    }

    static String render(Board board) {
        StringBuilder output = new StringBuilder();
        appendHeader(output, board.getCols());
        for (int row = 0; row < board.getRows(); row++) {
            appendRow(output, board, row);
        }
        return output.toString();
    }

    private static void appendHeader(StringBuilder output, int columns) {
        output.append("     ");
        for (int col = 0; col < columns; col++) {
            output.append(String.format(" %2d ", col + 1));
        }
        output.append(System.lineSeparator());
    }

    private static void appendRow(StringBuilder output, Board board, int row) {
        output.append(String.format("%2d |", row + 1));
        for (int col = 0; col < board.getCols(); col++) {
            output.append(String.format("%-4s", tokenAt(board, row, col)));
        }
        output.append(" mower=")
            .append(board.getLawnMower(row).isActivated() ? "used" : "ready")
            .append(System.lineSeparator());
    }

    private static String tokenAt(Board board, int row, int col) {
        Tile tile = board.getTile(row, col);
        boolean hasPlant = tile.getPlant() != null;
        boolean hasZombie = !tile.getZombies().isEmpty();
        if (tile.getType() == TileType.ICE && tile.hasTrappedEntity()) {
            return hasZombie ? "IZ" : "IP";
        }
        if (hasPlant && hasZombie) {
            return "PZ";
        }
        if (hasPlant) {
            return tile.getCoverPlant() != null ? "PC" : "P ";
        }
        if (hasZombie) {
            return "Z" + Math.min(9, tile.getZombies().size());
        }
        String objectToken = laneObjectToken(board, row, col);
        if (objectToken != null) {
            return objectToken;
        }
        if (!board.getSunsAt(row, col).isEmpty()) {
            return "S ";
        }
        return tileToken(tile.getType());
    }

    private static String laneObjectToken(Board board, int row, int col) {
        for (PushedObstacle obstacle : board.getPushedObstacles()) {
            if (!obstacle.isDestroyed() && obstacle.getPosition().getRow() == row
                && (int) Math.floor(obstacle.getPosition().getColumn()) == col) {
                return switch (obstacle.getType()) {
                    case ICE_BLOCK -> "IB";
                    case ARCADE_MACHINE -> "AM";
                    case BARREL -> "BR";
                };
            }
        }
        for (ProspectorDynamite dynamite : board.getProspectorDynamites()) {
            if (dynamite.isActive() && dynamite.getPosition().getRow() == row
                && (int) Math.floor(dynamite.getPosition().getColumn()) == col) {
                return "DY";
            }
        }
        return null;
    }

    private static String tileToken(TileType type) {
        return switch (type) {
            case NORMAL -> ". ";
            case WATER -> "~~";
            case TOMB -> "T ";
            case ICE -> "I ";
            case SLIPPERY_UP -> "^ ";
            case SLIPPERY_DOWN -> "v ";
            case LOW_TIDE -> "L ";
            case NECROMANCY -> "N ";
            case CRATER -> "C ";
        };
    }
}
