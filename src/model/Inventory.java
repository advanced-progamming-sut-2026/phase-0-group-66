package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Inventory implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int MAX_PLANT_FOODS = 3;

    private int plantFoods;
    private int pots;
    private final ArrayList<String> selectedPlants;
    private final LinkedHashMap<String, Integer> seedPackets;

    public Inventory() {
        selectedPlants = new ArrayList<>();
        seedPackets = new LinkedHashMap<>();
    }

    public int getPlantFoods() {
        return plantFoods;
    }

    public int getPots() {
        return pots;
    }

    public List<String> getSelectedPlants() {
        return Collections.unmodifiableList(selectedPlants);
    }

    public Map<String, Integer> getSeedPackets() {
        return Collections.unmodifiableMap(seedPackets);
    }

    public void addPlantFood(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Count cannot be negative.");
        }
        plantFoods = Math.min(MAX_PLANT_FOODS, plantFoods + count);
    }

    public boolean consumePlantFood() {
        if (plantFoods == 0) {
            return false;
        }
        plantFoods--;
        return true;
    }

    public void addPot() {
        pots++;
    }

    public boolean consumePot() {
        if (pots == 0) {
            return false;
        }
        pots--;
        return true;
    }

    public void selectPlant(String plantName) {
        if (plantName != null && !selectedPlants.contains(plantName)) {
            selectedPlants.add(plantName);
        }
    }

    public void clearSelectedPlants() {
        selectedPlants.clear();
    }

    public void addSeedPacket(String plantName, int count) {
        if (plantName == null || plantName.isBlank() || count < 0) {
            throw new IllegalArgumentException("Invalid seed packet data.");
        }
        seedPackets.merge(plantName, count, Integer::sum);
    }
}
