package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Tile {
    private final GridPosition position;
    private TileType tileType;
    private Plant plant;
    private final List<Zombie> zombies;

    public Tile(int row, int col, String tileType) {
        this(row, col, TileType.fromText(tileType));
    }

    public Tile(int row, int col, TileType tileType) {
        this.position = new GridPosition(row, col);
        this.tileType = tileType == null ? TileType.NORMAL : tileType;
        this.zombies = new ArrayList<>();
    }

    public GridPosition getPosition() {
        return position;
    }

    public String getTileType() {
        return tileType.name();
    }

    public TileType getType() {
        return tileType;
    }

    public void setTileType(String tileType) {
        this.tileType = TileType.fromText(tileType);
    }

    public void setTileType(TileType tileType) {
        this.tileType = tileType == null ? TileType.NORMAL : tileType;
    }

    public Plant getPlant() {
        return plant;
    }

    public void setPlant(Plant plant) {
        this.plant = plant;
    }

    public List<Zombie> getZombies() {
        return Collections.unmodifiableList(zombies);
    }

    public boolean canPlant() {
        return plant == null && tileType.isPlantable();
    }

    public void addZombie(Zombie zombie) {
        if (zombie != null && !zombies.contains(zombie)) {
            zombies.add(zombie);
        }
    }

    public void removeZombie(Zombie zombie) {
        zombies.remove(zombie);
    }

    public void clearZombies() {
        zombies.clear();
    }

    public boolean isEmpty() {
        return plant == null && zombies.isEmpty();
    }
}
