package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Tile {
    private final GridPosition position;
    private TileType tileType;
    private Plant supportPlant;
    private Plant mainPlant;
    private Plant coverPlant;
    private final List<Zombie> zombies;

    public Tile(int row, int col, String tileType) {
        this(row, col, TileType.fromText(tileType));
    }

    public Tile(int row, int col, TileType tileType) {
        this.position = new GridPosition(row, col);
        this.tileType = tileType == null ? TileType.NORMAL : tileType;
        this.zombies = new ArrayList<>();
    }

    public GridPosition getPosition() { return position; }
    public String getTileType() { return tileType.name(); }
    public TileType getType() { return tileType; }

    public void setTileType(String tileType) {
        this.tileType = TileType.fromText(tileType);
    }

    public void setTileType(TileType tileType) {
        this.tileType = tileType == null ? TileType.NORMAL : tileType;
    }

    /** Returns the main interactive plant, falling back to support or cover. */
    public Plant getPlant() {
        if (mainPlant != null) {
            return mainPlant;
        }
        if (supportPlant != null) {
            return supportPlant;
        }
        return coverPlant;
    }

    public Plant getMainPlant() { return mainPlant; }
    public Plant getSupportPlant() { return supportPlant; }
    public Plant getCoverPlant() { return coverPlant; }

    public Plant getBlockingPlant() {
        if (coverPlant != null && !coverPlant.isDestroyed()) {
            return coverPlant;
        }
        if (mainPlant != null && !mainPlant.isDestroyed()) {
            return mainPlant;
        }
        if (supportPlant != null && !supportPlant.isDestroyed()) {
            return supportPlant;
        }
        return null;
    }

    public List<Plant> getPlants() {
        ArrayList<Plant> result = new ArrayList<>(3);
        if (supportPlant != null) {
            result.add(supportPlant);
        }
        if (mainPlant != null) {
            result.add(mainPlant);
        }
        if (coverPlant != null) {
            result.add(coverPlant);
        }
        return Collections.unmodifiableList(result);
    }

    /** Legacy setter: replaces the main plant. */
    public void setPlant(Plant plant) {
        this.mainPlant = plant;
    }

    public void setSupportPlant(Plant plant) { supportPlant = plant; }
    public void setMainPlant(Plant plant) { mainPlant = plant; }
    public void setCoverPlant(Plant plant) { coverPlant = plant; }

    public boolean removePlant(Plant plant) {
        if (plant == null) {
            return false;
        }
        if (plant == coverPlant) {
            coverPlant = null;
            return true;
        }
        if (plant == mainPlant) {
            mainPlant = null;
            return true;
        }
        if (plant == supportPlant) {
            supportPlant = null;
            return true;
        }
        return false;
    }

    public boolean canPlant() {
        return mainPlant == null && supportPlant == null && coverPlant == null
            && tileType.isPlantable();
    }

    public List<Zombie> getZombies() { return Collections.unmodifiableList(zombies); }

    public void addZombie(Zombie zombie) {
        if (zombie != null && !zombies.contains(zombie)) {
            zombies.add(zombie);
        }
    }

    public void removeZombie(Zombie zombie) { zombies.remove(zombie); }
    public void clearZombies() { zombies.clear(); }

    public boolean isEmpty() {
        return mainPlant == null && supportPlant == null && coverPlant == null
            && zombies.isEmpty();
    }
}
