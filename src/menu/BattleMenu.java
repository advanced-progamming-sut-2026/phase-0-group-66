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
        System.out.println("advance time -t <count> ticks | start zombie waves");
        System.out.println("show special status");
        System.out.println("show map | show sun amount | show plants status | show score status");
        System.out.println("show tile status -l (<x>, <y>) | zombies info");
        System.out.println("cheat add -n <count> suns | cheat remove-cooldown");
        System.out.println("cheat add-plant-food");
        System.out.println("release the nuke | cheat spawn-zombie -t <type> -l (<x>, <y>)");
    }

    @Override
    protected void processSpecificCommand(String command) {
        boolean handled = processPlantingCommand(command)
            || processPositionCommand(command)
            || processTimeCommand(command)
            || processDisplayCommand(command)
            || processCheatCommand(command)
            || processSimpleCommand(command);
        if (!handled) {
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

    private boolean processPlantingCommand(String command) {
        Matcher matcher = getMatcher(command,
            "plant plant -t (?<type>.+?) -l \\((?<x>\\d+),\\s*(?<y>\\d+)\\)");
        if (matcher == null) {
            return false;
        }
        show(controller.plantPlant(matcher.group("type").trim(),
            number(matcher, "x"), number(matcher, "y")));
        return true;
    }

    private boolean processPositionCommand(String command) {
        Matcher matcher = position(command, "pluck plant -l ");
        if (matcher != null) {
            show(controller.pluckPlant(number(matcher, "x"), number(matcher, "y")));
            return true;
        }
        matcher = position(command, "collect sun -l ");
        if (matcher != null) {
            show(controller.collectSun(number(matcher, "x"), number(matcher, "y")));
            return true;
        }
        matcher = position(command, "feed plant -l ");
        if (matcher != null) {
            show(controller.feedPlant(number(matcher, "x"), number(matcher, "y")));
            return true;
        }
        matcher = position(command, "show tile status -l ");
        if (matcher != null) {
            controller.showTileStatus(number(matcher, "x"), number(matcher, "y"));
            return true;
        }
        return false;
    }

    private boolean processTimeCommand(String command) {
        Matcher matcher = getMatcher(command, "advance time -t (?<ticks>\\d+) ticks");
        if (matcher != null) {
            show(controller.advanceTime(number(matcher, "ticks")));
            return true;
        }
        if (command.equals("start zombie waves")) {
            show(controller.startZombieWaves());
            return true;
        }
        return false;
    }

    private boolean processDisplayCommand(String command) {
        switch (command) {
            case "show plant-food amount" -> System.out.println(controller.plantFoodAmount());
            case "show special status" -> System.out.println(controller.specialStatus());
            case "show score status" -> System.out.println(controller.scoreStatus());
            case "show map" -> controller.printMap();
            case "show sun amount" -> System.out.println(controller.sunAmount());
            case "show plants status" -> controller.showPlantStatus();
            case "zombies info" -> controller.showZombieInfo();
            default -> {
                return false;
            }
        }
        return true;
    }

    private boolean processCheatCommand(String command) {
        Matcher matcher = getMatcher(command, "cheat add -n (?<count>\\d+) suns");
        if (matcher != null) {
            show(controller.addSuns(number(matcher, "count")));
            return true;
        }
        matcher = getMatcher(command,
            "cheat spawn-zombie -t (?<type>.+?) -l \\((?<x>\\d+),\\s*(?<y>\\d+)\\)");
        if (matcher != null) {
            show(controller.spawnZombie(matcher.group("type").trim(),
                number(matcher, "x"), number(matcher, "y")));
            return true;
        }
        return false;
    }

    private boolean processSimpleCommand(String command) {
        switch (command) {
            case "cheat remove-cooldown" -> show(controller.removeCooldowns());
            case "cheat add-plant-food" -> show(controller.addPlantFoodCheat());
            case "release the nuke" -> show(controller.releaseNuke());
            case "show commands" -> showCommands();
            default -> {
                return false;
            }
        }
        return true;
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
