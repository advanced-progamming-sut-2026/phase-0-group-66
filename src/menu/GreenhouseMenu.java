package menu;

import controller.ActionResult;
import controller.GreenhouseController;
import view.GreenhouseView;

import java.util.regex.Matcher;

public class GreenhouseMenu extends Menu {
    private final GreenhouseController controller;
    private final GreenhouseView view;

    public GreenhouseMenu(MenuManager menuManager, GreenhouseController controller,
                          GreenhouseView view) {
        super("Greenhouse Menu", menuManager);
        this.controller = controller;
        this.view = view;
    }

    @Override
    public void showCommands() {
        System.out.println("show greenhouse");
        System.out.println("plant pot at (<x>, <y>)");
        System.out.println("collect (<x>, <y>)");
        System.out.println("grow (<x>, <y>)");
        System.out.println("enter shop");
    }

    @Override
    protected void processSpecificCommand(String command) {
        Matcher plant = position(command, "plant pot at ");
        Matcher collect = position(command, "collect ");
        Matcher grow = position(command, "grow ");
        if (command.equals("show greenhouse")) {
            view.showGreenhouse(controller.showGreenhouse());
        } else if (command.equals("enter shop")) {
            menuManager.enterMenu("Shop Menu");
        } else if (plant != null) {
            show(controller.plantPot(number(plant, "x"), number(plant, "y")));
        } else if (collect != null) {
            show(controller.collect(number(collect, "x"), number(collect, "y")));
        } else if (grow != null) {
            show(controller.grow(number(grow, "x"), number(grow, "y")));
        } else if (command.equals("show commands")) {
            showCommands();
        } else {
            view.showMessage("invalid command");
        }
    }

    private Matcher position(String command, String prefix) {
        return getMatcher(command, prefix + "\\((?<x>\\d+),\\s*(?<y>\\d+)\\)");
    }

    private int number(Matcher matcher, String group) {
        return Integer.parseInt(matcher.group(group));
    }

    private void show(ActionResult result) {
        view.showMessage(result.getMessage());
    }
}
