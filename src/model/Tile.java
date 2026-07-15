package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Tile {
    private final GridPosition position;
    private String tileType;
    private Plant plant;
    private final List<Zombie> zombies;

    public Tile(int row, int col, String tileType) {
        this.position = new GridPosition(row, col);
        this.tileType = tileType;
        this.zombies = new ArrayList<>();
    }

    public GridPosition getPosition() {
        return position;
    }

    public String getTileType() {
        return tileType;
    }

    public void setTileType(String tileType) {
        this.tileType = tileType;
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
        return plant == null && !"WATER".equals(tileType) && !"TOMB".equals(tileType)
            && !"ICE".equals(tileType) && !"SLIPPERY".equals(tileType);
    }

    public void addZombie(Zombie zombie) {
        if (zombie != null && !zombies.contains(zombie)) {
            zombies.add(zombie);
        }
    }

    public void removeZombie(Zombie zombie) {
        zombies.remove(zombie);
    }

    public boolean isEmpty() {
        return plant == null && zombies.isEmpty();
    }
}
