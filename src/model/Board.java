package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Board {
    public static final int DEFAULT_ROWS = 5;
    public static final int DEFAULT_COLUMNS = 9;

    private final int rows;
    private final int cols;
    private final Tile[][] tiles;
    private final List<LawnMower> lawnMowers;
    private final List<Zombie> zombies;
    private final List<Projectile> projectiles;
    private final List<Sun> suns;
    private boolean endangeredPlantsEaten;

    public Board() {
        this(DEFAULT_ROWS, DEFAULT_COLUMNS);
    }

    public Board(int rows, int cols) {
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("Board dimensions must be positive.");
        }
        this.rows = rows;
        this.cols = cols;
        this.tiles = new Tile[rows][cols];
        this.lawnMowers = new ArrayList<>();
        this.zombies = new ArrayList<>();
        this.projectiles = new ArrayList<>();
        this.suns = new ArrayList<>();
        for (int row = 0; row < rows; row++) {
            lawnMowers.add(new LawnMower(row));
            for (int col = 0; col < cols; col++) {
                tiles[row][col] = new Tile(row, col, TileType.NORMAL);
            }
        }
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public List<LawnMower> getLawnMowers() {
        return Collections.unmodifiableList(lawnMowers);
    }

    public LawnMower getLawnMower(int row) {
        validateRow(row);
        return lawnMowers.get(row);
    }

    public void placePlant(Plant plant, int row, int col) {
        if (plant == null) {
            throw new IllegalArgumentException("Plant cannot be null.");
        }
        Tile tile = getTile(row, col);
        if (!tile.canPlant()) {
            throw new IllegalStateException("Cannot plant on this tile.");
        }
        tile.setPlant(plant);
        plant.setPosition(new GridPosition(row, col));
    }

    public Plant removePlant(int row, int col) {
        Tile tile = getTile(row, col);
        Plant removed = tile.getPlant();
        tile.setPlant(null);
        if (removed != null) {
            removed.setPosition(null);
        }
        return removed;
    }

    public List<Plant> getPlants() {
        ArrayList<Plant> result = new ArrayList<>();
        for (Tile[] rowTiles : tiles) {
            for (Tile tile : rowTiles) {
                if (tile.getPlant() != null) {
                    result.add(tile.getPlant());
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    public Tile getTile(int row, int col) {
        if (!isInside(row, col)) {
            throw new IndexOutOfBoundsException("Tile is outside the board.");
        }
        return tiles[row][col];
    }

    public boolean isInside(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    public void addZombie(Zombie zombie) {
        if (zombie != null && !zombies.contains(zombie)) {
            zombies.add(zombie);
            refreshZombieTiles();
        }
    }

    public void removeZombie(Zombie zombie) {
        zombies.remove(zombie);
        for (Tile[] rowTiles : tiles) {
            for (Tile tile : rowTiles) {
                tile.removeZombie(zombie);
            }
        }
    }

    public List<Zombie> getZombies() {
        return Collections.unmodifiableList(zombies);
    }

    public List<Zombie> getZombiesInRow(int row) {
        validateRow(row);
        ArrayList<Zombie> result = new ArrayList<>();
        for (Zombie zombie : zombies) {
            if (!zombie.isDead() && zombie.getPosition() != null
                && zombie.getPosition().getRow() == row) {
                result.add(zombie);
            }
        }
        result.sort(Comparator.comparingDouble(zombie -> zombie.getPosition().getColumn()));
        return result;
    }

    public Zombie findNearestZombieAhead(int row, double column) {
        Zombie nearest = null;
        for (Zombie zombie : getZombiesInRow(row)) {
            double zombieColumn = zombie.getPosition().getColumn();
            if (zombieColumn + 0.001 < column) {
                continue;
            }
            if (nearest == null
                || zombieColumn < nearest.getPosition().getColumn()) {
                nearest = zombie;
            }
        }
        return nearest;
    }

    public Zombie findNearestZombieAnywhere() {
        Zombie nearest = null;
        for (Zombie zombie : zombies) {
            if (zombie.isDead() || zombie.getPosition() == null) {
                continue;
            }
            if (nearest == null
                || zombie.getPosition().getColumn() < nearest.getPosition().getColumn()) {
                nearest = zombie;
            }
        }
        return nearest;
    }

    public Plant findBlockingPlant(Zombie zombie) {
        if (zombie == null || zombie.getPosition() == null) {
            return null;
        }
        int row = zombie.getPosition().getRow();
        double zombieColumn = zombie.getPosition().getColumn();
        if (row < 0 || row >= rows) {
            return null;
        }
        for (int col = cols - 1; col >= 0; col--) {
            Plant plant = tiles[row][col].getPlant();
            if (plant == null || plant.isDestroyed()) {
                continue;
            }
            if (zombieColumn <= col + 0.82 && zombieColumn >= col - 0.05) {
                return plant;
            }
        }
        return null;
    }

    public void addProjectile(Projectile projectile) {
        if (projectile != null) {
            projectiles.add(projectile);
        }
    }

    public void removeProjectile(Projectile projectile) {
        projectiles.remove(projectile);
    }

    public List<Projectile> getProjectiles() {
        return Collections.unmodifiableList(projectiles);
    }

    public void addSun(Sun sun) {
        if (sun != null) {
            suns.add(sun);
        }
    }

    public void removeSun(Sun sun) {
        suns.remove(sun);
    }

    public List<Sun> getSuns() {
        return Collections.unmodifiableList(suns);
    }

    public List<Sun> getSunsAt(int row, int col) {
        ArrayList<Sun> result = new ArrayList<>();
        for (Sun sun : suns) {
            GridPosition position = sun.getPosition();
            if (!sun.isCollected() && position != null
                && position.getRow() == row && position.getColumn() == col) {
                result.add(sun);
            }
        }
        return result;
    }

    public boolean hasPlantGeneratedSunAt(GridPosition position) {
        if (position == null) {
            return false;
        }
        for (Sun sun : suns) {
            if (!sun.isCollected() && sun.isPlantGenerated()
                && position.equals(sun.getPosition())) {
                return true;
            }
        }
        return false;
    }

    public void refreshZombieTiles() {
        for (Tile[] rowTiles : tiles) {
            for (Tile tile : rowTiles) {
                tile.clearZombies();
            }
        }
        for (Zombie zombie : zombies) {
            if (zombie.isDead() || zombie.getPosition() == null) {
                continue;
            }
            int row = zombie.getPosition().getRow();
            int col = (int) Math.floor(zombie.getPosition().getColumn());
            if (isInside(row, col)) {
                tiles[row][col].addZombie(zombie);
            }
        }
    }

    public boolean hasZombiesCrossedColumn(int column) {
        for (Zombie zombie : zombies) {
            if (!zombie.isDead() && zombie.getPosition() != null
                && zombie.getPosition().getColumn() < column) {
                return true;
            }
        }
        return false;
    }

    public boolean areEndangeredPlantsEaten() {
        return endangeredPlantsEaten;
    }

    public void setEndangeredPlantsEaten(boolean endangeredPlantsEaten) {
        this.endangeredPlantsEaten = endangeredPlantsEaten;
    }

    public String render() {
        StringBuilder output = new StringBuilder();
        output.append("     ");
        for (int col = 0; col < cols; col++) {
            output.append(String.format(" %2d ", col + 1));
        }
        output.append(System.lineSeparator());
        for (int row = 0; row < rows; row++) {
            output.append(String.format("%2d |", row + 1));
            for (int col = 0; col < cols; col++) {
                Tile tile = tiles[row][col];
                boolean hasPlant = tile.getPlant() != null;
                boolean hasZombie = !tile.getZombies().isEmpty();
                String token;
                if (hasPlant && hasZombie) {
                    token = "PZ";
                } else if (hasPlant) {
                    token = "P ";
                } else if (hasZombie) {
                    token = "Z" + Math.min(9, tile.getZombies().size());
                } else if (!getSunsAt(row, col).isEmpty()) {
                    token = "S ";
                } else {
                    token = tileToken(tile.getType());
                }
                output.append(String.format("%-4s", token));
            }
            output.append(" mower=")
                .append(lawnMowers.get(row).isActivated() ? "used" : "ready")
                .append(System.lineSeparator());
        }
        return output.toString();
    }

    private String tileToken(TileType type) {
        return switch (type) {
            case NORMAL -> ". ";
            case WATER -> "~~";
            case TOMB -> "T ";
            case ICE -> "I ";
            case SLIPPERY_UP -> "^ ";
            case SLIPPERY_DOWN -> "v ";
            case LOW_TIDE -> "L ";
            case NECROMANCY -> "N ";
        };
    }

    private void validateRow(int row) {
        if (row < 0 || row >= rows) {
            throw new IndexOutOfBoundsException("Row is outside the board.");
        }
    }
}
