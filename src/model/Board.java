package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Board {
    public static final int DEFAULT_ROWS = 5;
    public static final int DEFAULT_COLUMNS = 9;

    private final int rows;
    private final int cols;
    private final Tile[][] tiles;
    private final List<LawnMower> lawnMowers;
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
        for (int row = 0; row < rows; row++) {
            lawnMowers.add(new LawnMower(row));
            for (int col = 0; col < cols; col++) {
                tiles[row][col] = new Tile(row, col, "NORMAL");
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

    public void placePlant(Plant plant, int row, int col) {
        Tile tile = getTile(row, col);
        if (!tile.canPlant()) {
            throw new IllegalStateException("Cannot plant on this tile.");
        }
        tile.setPlant(plant);
        plant.setPosition(new GridPosition(row, col));
    }

    public void removePlant(int row, int col) {
        getTile(row, col).setPlant(null);
    }

    public Tile getTile(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            throw new IndexOutOfBoundsException("Tile is outside the board.");
        }
        return tiles[row][col];
    }

    public void moveZombies() {
        for (Tile[] rowTiles : tiles) {
            for (Tile tile : rowTiles) {
                for (Zombie zombie : new ArrayList<>(tile.getZombies())) {
                    zombie.move();
                }
            }
        }
    }

    public boolean hasZombiesCrossedColumn(int column) {
        for (Tile[] rowTiles : tiles) {
            for (Tile tile : rowTiles) {
                for (Zombie zombie : tile.getZombies()) {
                    if (zombie.getPosition() != null
                        && zombie.getPosition().getColumn() < column) {
                        return true;
                    }
                }
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
}
