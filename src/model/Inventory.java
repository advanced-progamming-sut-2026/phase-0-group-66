package model;

import java.io.IOException;
import java.io.ObjectInputStream;
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
    private ArrayList<String> selectedPlants;
    private LinkedHashMap<String, Integer> seedPackets;
    private LinkedHashMap<String, Integer> storedBoosts;

    public Inventory() {
        selectedPlants = new ArrayList<>();
        seedPackets = new LinkedHashMap<>();
        storedBoosts = new LinkedHashMap<>();
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

    public Map<String, Integer> getStoredBoosts() {
        return Collections.unmodifiableMap(storedBoosts);
    }

    public int getPlantFoodCapacityLeft() {
        return MAX_PLANT_FOODS - plantFoods;
    }

    public boolean addStoredBoost(String plantName) {
        if (plantName == null || plantName.isBlank() || storedBoosts.getOrDefault(plantName, 0) > 0) {
            return false;
        }
        storedBoosts.put(plantName, 1);
        return true;
    }

    public boolean consumeStoredBoost(String plantName) {
        if (storedBoosts.getOrDefault(plantName, 0) <= 0) {
            return false;
        }
        storedBoosts.remove(plantName);
        return true;
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

    private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
        input.defaultReadObject();
        if (selectedPlants == null) {
            selectedPlants = new ArrayList<>();
        }
        if (seedPackets == null) {
            seedPackets = new LinkedHashMap<>();
        }
        if (storedBoosts == null) {
            storedBoosts = new LinkedHashMap<>();
        }
    }

    public int getSeedPacketCount(String plantName) {
        return seedPackets.getOrDefault(plantName, 0);
    }

    public boolean consumeSeedPackets(String plantName, int count) {
        if (plantName == null || plantName.isBlank() || count < 0) {
            throw new IllegalArgumentException("Invalid seed packet data.");
        }
        int available = seedPackets.getOrDefault(plantName, 0);
        if (available < count) {
            return false;
        }
        int remaining = available - count;
        if (remaining == 0) {
            seedPackets.remove(plantName);
        } else {
            seedPackets.put(plantName, remaining);
        }
        return true;
    }

    public void addSeedPacket(String plantName, int count) {
        if (plantName == null || plantName.isBlank() || count < 0) {
            throw new IllegalArgumentException("Invalid seed packet data.");
        }
        seedPackets.merge(plantName, count, Integer::sum);
    }
}
