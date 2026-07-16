package controller;

import model.ArmorDefinition;
import model.ArmorFactory;
import model.PlantDefinition;
import model.PlantFactory;
import model.User;
import model.ZombieDefinition;
import model.ZombieFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CollectionController {
    private static final int PLANT_PURCHASE_COST = 2000;

    private final AuthController authController;
    private final PlantFactory plantFactory;
    private final ZombieFactory zombieFactory;
    private final ArmorFactory armorFactory;

    public CollectionController(AuthController authController, PlantFactory plantFactory,
                                ZombieFactory zombieFactory, ArmorFactory armorFactory) {
        this.authController = authController;
        this.plantFactory = plantFactory;
        this.zombieFactory = zombieFactory;
        this.armorFactory = armorFactory;
    }

    public List<String> getOwnedPlants() {
        User user = authController.getCurrentUser();
        if (user == null) {
            return List.of();
        }
        return List.copyOf(user.getCollectionBook().getOwnedPlants());
    }

    public List<String> getAllPlants() {
        return plantFactory.getAllPlants();
    }

    public List<String> getSeenZombies() {
        User user = authController.getCurrentUser();
        if (user == null) {
            return List.of();
        }
        return List.copyOf(user.getCollectionBook().getSeenZombies());
    }

    public List<String> getAllZombies() {
        return zombieFactory.getAllZombies();
    }

    public Optional<PlantDefinition> findPlant(String plantName) {
        return plantFactory.findDefinition(plantName);
    }

    public Optional<ZombieDefinition> findZombie(String zombieName) {
        return zombieFactory.findDefinition(zombieName);
    }

    public List<ArmorDefinition> getArmorDefinitions(ZombieDefinition zombie) {
        ArrayList<ArmorDefinition> result = new ArrayList<>();
        for (String alias : zombie.getArmorAliases()) {
            armorFactory.findDefinition(alias).ifPresent(result::add);
        }
        return List.copyOf(result);
    }

    public ActionResult purchasePlant(String plantName) {
        User user = authController.getCurrentUser();
        if (user == null) {
            return ActionResult.failure("Login is required.");
        }
        Optional<PlantDefinition> definition = plantFactory.findDefinition(plantName);
        if (definition.isEmpty()) {
            return ActionResult.failure("Plant does not exist.");
        }
        String canonicalName = definition.get().getName();
        if (user.getCollectionBook().getOwnedPlants().contains(canonicalName)) {
            return ActionResult.failure("Plant is already owned.");
        }
        if (!user.getWallet().spendCoins(PLANT_PURCHASE_COST)) {
            return ActionResult.failure("Not enough coins. Plant purchase costs 2000 coins.");
        }
        user.getCollectionBook().unlockPlant(canonicalName);
        return saveResult("Plant purchased: " + canonicalName);
    }

    public ActionResult upgradePlant(String plantName) {
        User user = authController.getCurrentUser();
        if (user == null) {
            return ActionResult.failure("Login is required.");
        }
        Optional<PlantDefinition> definition = plantFactory.findDefinition(plantName);
        if (definition.isEmpty()) {
            return ActionResult.failure("Plant does not exist.");
        }
        String canonicalName = definition.get().getName();
        if (user.getCollectionBook().getPlantLevel(canonicalName) == 0) {
            return ActionResult.failure("Plant is not owned.");
        }
        return ActionResult.failure("Upgrade effects are loaded, but upgrade costs are not defined in the data file.");
    }

    public List<PlantDefinition> getOwnedPlantDefinitions() {
        ArrayList<PlantDefinition> result = new ArrayList<>();
        for (String name : getOwnedPlants()) {
            plantFactory.findDefinition(name).ifPresent(result::add);
        }
        return List.copyOf(result);
    }

    private ActionResult saveResult(String successMessage) {
        ActionResult saveResult = authController.saveCurrentState();
        if (!saveResult.isSuccessful()) {
            return saveResult;
        }
        return ActionResult.success(successMessage);
    }
}
