package menu;

import controller.ActionResult;
import controller.CollectionController;
import model.PlantDefinition;
import model.ZombieDefinition;
import view.CollectionView;

import java.util.regex.Matcher;

public class CollectionMenu extends Menu {
    private final CollectionController controller;
    private final CollectionView view;

    public CollectionMenu(MenuManager menuManager, CollectionController controller,
                          CollectionView view) {
        super("Collection Menu", menuManager);
        this.controller = controller;
        this.view = view;
    }

    @Override
    public void showCommands() {
        System.out.println("menu collection show-plants");
        System.out.println("menu collection show-all-plants");
        System.out.println("menu collection show-zombies");
        System.out.println("menu collection show-all-zombies");
        System.out.println("menu collection show-plant -p <plant_name>");
        System.out.println("menu collection show-zombie -z <zombie_name>");
        System.out.println("menu collection purchase-plant -p <plant_name>");
        System.out.println("menu collection upgrade-plant -p <plant_name>");
    }

    @Override
    protected void processSpecificCommand(String command) {
        Matcher showPlant = getMatcher(command, "menu collection show-plant -p (?<name>.+)");
        Matcher showZombie = getMatcher(command, "menu collection show-zombie -z (?<name>.+)");
        Matcher upgrade = getMatcher(command, "menu collection upgrade-plant -p (?<name>.+)");
        Matcher purchase = getMatcher(command, "menu collection purchase-plant -p (?<name>.+)");

        if (command.equals("menu collection show-plants")) {
            view.showPlantNames(controller.getOwnedPlants());
        } else if (command.equals("menu collection show-all-plants")) {
            view.showPlantNames(controller.getAllPlants());
        } else if (command.equals("menu collection show-zombies")) {
            view.showZombieNames(controller.getSeenZombies());
        } else if (command.equals("menu collection show-all-zombies")) {
            view.showZombieNames(controller.getAllZombies());
        } else if (showPlant != null) {
            showPlant(showPlant.group("name").trim());
        } else if (showZombie != null) {
            showZombie(showZombie.group("name").trim());
        } else if (upgrade != null) {
            showResult(controller.upgradePlant(upgrade.group("name").trim()));
        } else if (purchase != null) {
            showResult(controller.purchasePlant(purchase.group("name").trim()));
        } else {
            view.showMessage("invalid command");
        }
    }

    private void showPlant(String name) {
        PlantDefinition definition = controller.findPlant(name).orElse(null);
        if (definition == null) {
            view.showMessage("Plant does not exist.");
        } else {
            view.showPlantDetails(definition);
        }
    }

    private void showZombie(String name) {
        ZombieDefinition definition = controller.findZombie(name).orElse(null);
        if (definition == null) {
            view.showMessage("Zombie does not exist.");
            return;
        }
        view.showZombieDetails(definition, controller.getArmorDefinitions(definition));
    }

    private void showResult(ActionResult result) {
        view.showMessage(result.getMessage());
    }
}
