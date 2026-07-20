package menu;

import controller.ActionResult;
import controller.GameController;

import java.util.List;
import java.util.regex.Matcher;

public class PlantSelectionMenu extends Menu {
    private final GameController controller;

    public PlantSelectionMenu(MenuManager menuManager, GameController controller) {
        super("Plant Selection Menu", menuManager);
        this.controller = controller;
    }

    @Override
    public void showCommands() {
        System.out.println("show all plants");
        System.out.println("show available plants");
        System.out.println("show selected plants");
        System.out.println("add plant -t <type>");
        System.out.println("remove plant -t <type>");
        System.out.println("boost plant -t <type>");
        System.out.println("start game");
    }

    @Override
    protected void processSpecificCommand(String command) {
        Matcher add = getMatcher(command, "add plant -t (?<type>.+)");
        Matcher remove = getMatcher(command, "remove plant -t (?<type>.+)");
        Matcher boost = getMatcher(command, "boost plant -t (?<type>.+)");
        if (command.equals("show all plants")) {
            show(controller.getAllPlants());
        } else if (command.equals("show available plants")) {
            show(controller.getAvailablePlants());
        } else if (command.equals("show selected plants")) {
            show(controller.getSelectedPlants());
        } else if (add != null) {
            result(controller.selectPlant(add.group("type").trim()), false);
        } else if (remove != null) {
            result(controller.removePlantSelection(remove.group("type").trim()), false);
        } else if (boost != null) {
            result(controller.boostPlant(boost.group("type").trim()), false);
        } else if (command.equals("start game")) {
            result(controller.startGame(), true);
        } else if (command.equals("show commands")) {
            showCommands();
        } else {
            System.out.println("invalid command");
        }
    }

    private void result(ActionResult result, boolean enterBattle) {
        System.out.println(result.getMessage());
        if (enterBattle && result.isSuccessful()) {
            menuManager.enterMenu("Battle Menu");
        }
    }

    private void show(List<String> lines) {
        lines.forEach(System.out::println);
    }
}
