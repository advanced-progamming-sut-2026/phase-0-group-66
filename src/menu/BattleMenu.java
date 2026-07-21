package menu;

import controller.ActionResult;
import controller.GameController;

import java.util.regex.Matcher;

public class BattleMenu extends Menu {
    private final GameController controller;

    public BattleMenu(MenuManager menuManager, GameController controller) {
        super("Battle Menu", menuManager);
        this.controller = controller;
    }

    @Override
    public void showCommands() {
        System.out.println("plant plant -t <type> -l (<x>, <y>)");
        System.out.println("pluck plant -l (<x>, <y>) | collect sun -l (<x>, <y>)");
        System.out.println("feed plant -l (<x>, <y>) | show plant-food amount");
        System.out.println("advance time -t <count> ticks");
        System.out.println("show map | show sun amount | show plants status");
        System.out.println("show tile status -l (<x>, <y>) | zombies info");
        System.out.println("cheat add -n <count> suns | cheat remove-cooldown");
        System.out.println("cheat add-plant-food");
        System.out.println("release the nuke | cheat spawn-zombie -t <type> -l (<x>, <y>)");
    }

    @Override
    protected void processSpecificCommand(String command) {
        Matcher plant = getMatcher(command,
            "plant plant -t (?<type>.+?) -l \\((?<x>\\d+),\\s*(?<y>\\d+)\\)");
        Matcher pluck = position(command, "pluck plant -l ");
        Matcher collect = position(command, "collect sun -l ");
        Matcher feed = position(command, "feed plant -l ");
        Matcher tile = position(command, "show tile status -l ");
        Matcher advance = getMatcher(command, "advance time -t (?<ticks>\\d+) ticks");
        Matcher addSun = getMatcher(command, "cheat add -n (?<count>\\d+) suns");
        Matcher spawn = getMatcher(command,
            "cheat spawn-zombie -t (?<type>.+?) -l \\((?<x>\\d+),\\s*(?<y>\\d+)\\)");
        if (plant != null) {
            show(controller.plantPlant(plant.group("type").trim(), number(plant, "x"),
                number(plant, "y")));
        } else if (pluck != null) {
            show(controller.pluckPlant(number(pluck, "x"), number(pluck, "y")));
        } else if (collect != null) {
            show(controller.collectSun(number(collect, "x"), number(collect, "y")));
        } else if (feed != null) {
            show(controller.feedPlant(number(feed, "x"), number(feed, "y")));
        } else if (command.equals("show plant-food amount")) {
            System.out.println(controller.plantFoodAmount());
        } else if (advance != null) {
            show(controller.advanceTime(number(advance, "ticks")));
        } else if (command.equals("show map")) {
            controller.printMap();
        } else if (command.equals("show sun amount")) {
            System.out.println(controller.sunAmount());
        } else if (command.equals("show plants status")) {
            controller.showPlantStatus();
        } else if (tile != null) {
            controller.showTileStatus(number(tile, "x"), number(tile, "y"));
        } else if (command.equals("zombies info")) {
            controller.showZombieInfo();
        } else if (addSun != null) {
            show(controller.addSuns(number(addSun, "count")));
        } else if (command.equals("cheat remove-cooldown")) {
            show(controller.removeCooldowns());
        } else if (command.equals("cheat add-plant-food")) {
            show(controller.addPlantFoodCheat());
        } else if (command.equals("release the nuke")) {
            show(controller.releaseNuke());
        } else if (spawn != null) {
            show(controller.spawnZombie(spawn.group("type").trim(), number(spawn, "x"),
                number(spawn, "y")));
        } else if (command.equals("show commands")) {
            showCommands();
        } else {
            System.out.println("invalid command");
        }
        if (controller.isGameFinished()) {
            menuManager.enterMenu("Game Menu");
        }
    }

    @Override
    public void exit() {
        System.out.println("A running battle cannot be exited with menu exit.");
    }

    private Matcher position(String command, String prefix) {
        return getMatcher(command, prefix + "\\((?<x>\\d+),\\s*(?<y>\\d+)\\)");
    }

    private int number(Matcher matcher, String group) {
        return Integer.parseInt(matcher.group(group));
    }

    private void show(ActionResult result) {
        System.out.println(result.getMessage());
    }
}
