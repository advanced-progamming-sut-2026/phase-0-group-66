package model;

final class BoardTargeting {
    private BoardTargeting() {
    }

    static Zombie findNearestZombieAhead(Board board, int row, double column) {
        Zombie nearest = null;
        for (Zombie zombie : board.getZombiesInRow(row)) {
            double zombieColumn = zombie.getPosition().getColumn();
            if (zombieColumn + 0.001 < column || zombie.isHypnotized()
                || zombie.isTrappedInIceTile()) {
                continue;
            }
            if (nearest == null || zombieColumn < nearest.getPosition().getColumn()) {
                nearest = zombie;
            }
        }
        return nearest;
    }

    static Zombie findNearestZombieBehind(Board board, int row, double column) {
        Zombie nearest = null;
        for (Zombie zombie : board.getZombiesInRow(row)) {
            double zombieColumn = zombie.getPosition().getColumn();
            if (zombieColumn - 0.001 > column || zombie.isHypnotized()
                || zombie.isTrappedInIceTile()) {
                continue;
            }
            if (nearest == null || zombieColumn > nearest.getPosition().getColumn()) {
                nearest = zombie;
            }
        }
        return nearest;
    }

    static Zombie findNearestZombieAnywhere(Board board) {
        Zombie nearest = null;
        for (Zombie zombie : board.getZombies()) {
            if (zombie.isDead() || zombie.getPosition() == null || zombie.isHypnotized()
                || zombie.isTrappedInIceTile()) {
                continue;
            }
            if (nearest == null
                || zombie.getPosition().getColumn() < nearest.getPosition().getColumn()) {
                nearest = zombie;
            }
        }
        return nearest;
    }

    static GridPosition findNearestFrozenZombieTileAhead(Board board, int row, double column) {
        GridPosition nearest = null;
        for (int col = Math.max(0, (int) Math.floor(column)); col < board.getCols(); col++) {
            Tile tile = board.getTile(row, col);
            if (tile.getType() != TileType.ICE) {
                continue;
            }
            for (Zombie zombie : tile.getZombies()) {
                if (!zombie.isDead() && zombie.isTrappedInIceTile()) {
                    nearest = tile.getPosition();
                    break;
                }
            }
            if (nearest != null) {
                break;
            }
        }
        return nearest;
    }

    static Plant findBlockingPlant(Board board, Zombie zombie) {
        if (zombie == null || zombie.getPosition() == null || zombie.isHypnotized()) {
            return null;
        }
        int row = zombie.getPosition().getRow();
        double zombieColumn = zombie.getPosition().getColumn();
        if (row < 0 || row >= board.getRows()) {
            return null;
        }
        for (int col = board.getCols() - 1; col >= 0; col--) {
            Plant plant = board.getTile(row, col).getBlockingPlant();
            if (plant == null || plant.isDestroyed()) {
                continue;
            }
            if (zombieColumn <= col + 0.82 && zombieColumn >= col - 0.05) {
                return plant;
            }
        }
        return null;
    }
}
