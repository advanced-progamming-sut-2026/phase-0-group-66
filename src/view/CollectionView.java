package view;

import model.ArmorDefinition;
import model.PlantDefinition;
import model.ZombieDefinition;

import java.util.List;

public class CollectionView {
    public void showPlantNames(List<String> plants) {
        showNames("Plants", plants);
    }

    public void showZombieNames(List<String> zombies) {
        showNames("Zombies", zombies);
    }

    public void showPlantDetails(PlantDefinition plant) {
        System.out.println("Plant: " + plant.getName());
        System.out.println("ID: " + plant.getId());
        System.out.println("Category: " + plant.getCategory());
        System.out.println("Tags: " + emptyAsDash(plant.getTags()));
        System.out.println("Sun cost: " + plant.getCost());
        System.out.println("Base health: " + plant.getBaseHealth());
        System.out.println("Damage: " + plant.getDamage());
        System.out.println("Ability: " + plant.getBaseAbility());
        System.out.println("Plant food: " + plant.getPlantFoodEffect());
        System.out.println("Level upgrades: " + emptyAsDash(plant.getLevelUpgrades()));
        System.out.println("Action interval: " + formatSeconds(plant.getActionIntervalSeconds()));
        System.out.println("Recharge: " + formatSeconds(plant.getRechargeSeconds()));
    }

    public void showZombieDetails(ZombieDefinition zombie, List<ArmorDefinition> armors) {
        System.out.println("Zombie: " + zombie.getDisplayName());
        System.out.println("Data alias: " + zombie.getAlias());
        System.out.println("Health: " + zombie.getHitpoints());
        System.out.println("Eat damage per second: " + zombie.getEatDamagePerSecond());
        System.out.println("Speed: " + zombie.getSpeed());
        System.out.println("Wave cost: " + zombie.getWavePointCost());
        System.out.println("Selection weight: " + zombie.getWeight());
        System.out.println("Can drop plant food: " + zombie.canSpawnPlantFood());
        System.out.println("Armors: " + emptyAsDash(armors));
        System.out.println("Special properties: " + emptyAsDash(zombie.getSpecialProperties().entrySet()));
    }

    public void showMessage(String message) {
        System.out.println(message);
    }

    private void showNames(String title, List<String> names) {
        System.out.println(title + " (" + names.size() + "):");
        if (names.isEmpty()) {
            System.out.println("- none");
            return;
        }
        for (String name : names) {
            System.out.println("- " + name);
        }
    }

    private String formatSeconds(java.util.OptionalDouble seconds) {
        return seconds.isPresent() ? seconds.getAsDouble() + "s" : "-";
    }

    private String emptyAsDash(Object value) {
        if (value == null || value.toString().equals("[]") || value.toString().equals("{}")) {
            return "-";
        }
        return value.toString();
    }
}
