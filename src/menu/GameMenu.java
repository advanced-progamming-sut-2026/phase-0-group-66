package menu;

import controller.ActionResult;
import controller.GameController;

import java.util.List;
import java.util.regex.Matcher;

public class GameMenu extends Menu {
    private final GameController controller;

    public GameMenu(MenuManager menuManager, GameController controller) {
        super("Game Menu", menuManager);
        if (controller == null) {
            throw new IllegalArgumentException("Game controller cannot be null.");
        }
        this.controller = controller;
    }

    @Override
    protected void handleMenuEnter(String targetMenu) {
        if (targetMenu.equals("Collection Menu")) {
            menuManager.enterMenu("Collection Menu");
            return;
        }
        Matcher withLevel = getMatcher(targetMenu,
            "chapter -c (?<chapter>.+?) -l (?<level>\\d+)");
        Matcher withoutLevel = getMatcher(targetMenu, "chapter -c (?<chapter>.+)");
        if (withLevel != null) {
            showResult(controller.startLevel(withLevel.group("chapter").trim(),
                integer(withLevel, "level")));
        } else if (withoutLevel != null) {
            showResult(controller.startLevel(withoutLevel.group("chapter").trim()));
        } else {
            super.handleMenuEnter(targetMenu);
        }
    }

    @Override
    public void showCommands() {
        System.out.println("show chapters");
        System.out.println("show levels -c <chapter_name>");
        System.out.println("menu enter chapter -c <chapter_name> -l <1-4>");
        System.out.println("show all plants | show available plants | show selected plants");
        System.out.println("add plant -t <type> | remove plant -t <type> | start game");
        System.out.println("plant plant -t <type> -l (<x>, <y>)");
        System.out.println("pluck plant -l (<x>, <y>) | collect sun -l (<x>, <y>)");
        System.out.println("advance time -t <count> ticks");
        System.out.println("show map | show sun amount | show plants status");
        System.out.println("show tile status -l (<x>, <y>) | zombies info");
        System.out.println("cheat add -n <count> suns | cheat remove-cooldown | release the nuke");
        System.out.println("cheat spawn-zombie -t <type> -l (<x>, <y>)");
    }

    @Override
    protected void processSpecificCommand(String command) {
        if (command.equals("show commands")) {
            showCommands();
        } else if (handleInformationCommand(command)) {
            return;
        } else if (handleSelectionCommand(command)) {
            return;
        } else if (handleGameActionCommand(command)) {
            return;
        } else if (handleCheatCommand(command)) {
            return;
        } else if (!handleNavigationCommand(command)) {
            System.out.println("invalid command");
        }
    }

    private boolean handleInformationCommand(String command) {
        Matcher levels = getMatcher(command, "show levels -c (?<chapter>.+)");
        Matcher tile = positionMatcher(command, "show tile status -l ");
        if (command.equals("show chapters")) {
            showLines(controller.getChapterDescriptions());
        } else if (levels != null) {
            showLines(controller.getLevelDescriptions(levels.group("chapter").trim()));
        } else if (command.equals("show all plants")) {
            showLines(controller.getAllPlants());
        } else if (command.equals("show available plants")) {
            showLines(controller.getAvailablePlants());
        } else if (command.equals("show selected plants")) {
            showLines(controller.getSelectedPlants());
        } else if (command.equals("show map")) {
            controller.printMap();
        } else if (command.equals("show sun amount")) {
            System.out.println(controller.sunAmount());
        } else if (command.equals("show plants status")) {
            controller.showPlantStatus();
        } else if (tile != null) {
            controller.showTileStatus(integer(tile, "x"), integer(tile, "y"));
        } else if (command.equals("zombies info")) {
            controller.showZombieInfo();
        } else {
            return false;
        }
        return true;
    }

    private boolean handleSelectionCommand(String command) {
        Matcher addPlant = getMatcher(command, "add plant -t (?<type>.+)");
        Matcher removePlant = getMatcher(command, "remove plant -t (?<type>.+)");
        if (addPlant != null) {
            showResult(controller.selectPlant(addPlant.group("type").trim()));
        } else if (removePlant != null) {
            showResult(controller.removePlantSelection(removePlant.group("type").trim()));
        } else if (command.equals("start game")) {
            showResult(controller.startGame());
        } else {
            return false;
        }
        return true;
    }

    private boolean handleGameActionCommand(String command) {
        Matcher plant = plantMatcher(command);
        Matcher pluck = positionMatcher(command, "pluck plant -l ");
        Matcher collect = positionMatcher(command, "collect sun -l ");
        Matcher advance = getMatcher(command, "advance time -t (?<ticks>\\d+) ticks");
        if (plant != null) {
            showResult(controller.plantPlant(plant.group("type").trim(),
                integer(plant, "x"), integer(plant, "y")));
        } else if (pluck != null) {
            showResult(controller.pluckPlant(integer(pluck, "x"), integer(pluck, "y")));
        } else if (collect != null) {
            showResult(controller.collectSun(integer(collect, "x"), integer(collect, "y")));
        } else if (advance != null) {
            showResult(controller.advanceTime(integer(advance, "ticks")));
        } else {
            return false;
        }
        return true;
    }

    private boolean handleCheatCommand(String command) {
        Matcher addSun = getMatcher(command, "cheat add -n (?<count>\\d+) suns");
        Matcher spawn = spawnMatcher(command);
        Matcher wallet = getMatcher(command,
            "menu cheat add (?<count>\\d+) (?<type>coin|diamond)");
        if (addSun != null) {
            showResult(controller.addSuns(integer(addSun, "count")));
        } else if (command.equals("cheat remove-cooldown")) {
            showResult(controller.removeCooldowns());
        } else if (command.equals("release the nuke")) {
            showResult(controller.releaseNuke());
        } else if (spawn != null) {
            showResult(controller.spawnZombie(spawn.group("type").trim(),
                integer(spawn, "x"), integer(spawn, "y")));
        } else if (wallet != null) {
            showResult(controller.addWalletCurrency(integer(wallet, "count"),
                wallet.group("type")));
        } else {
            return false;
        }
        return true;
    }

    private boolean handleNavigationCommand(String command) {
        if (command.equals("menu greenhouse")) {
            menuManager.enterMenu("Greenhouse Menu");
        } else if (command.equals("menu travel-log")) {
            menuManager.enterMenu("Quest Menu");
        } else if (command.equals("menu leaderboard")) {
            menuManager.enterMenu("Leaderboard Menu");
        } else if (command.equals("menu coin-wallet")) {
            System.out.println(controller.walletAmount("coin"));
        } else if (command.equals("menu gem-wallet")) {
            System.out.println(controller.walletAmount("diamond"));
        } else {
            return false;
        }
        return true;
    }

    private Matcher plantMatcher(String command) {
        return getMatcher(command,
            "plant plant -t (?<type>.+?) -l \\((?<x>\\d+),\\s*(?<y>\\d+)\\)");
    }

    private Matcher spawnMatcher(String command) {
        return getMatcher(command,
            "cheat spawn-zombie -t (?<type>.+?) -l \\((?<x>\\d+),\\s*(?<y>\\d+)\\)");
    }

    private Matcher positionMatcher(String command, String prefix) {
        return getMatcher(command, prefix + "\\((?<x>\\d+),\\s*(?<y>\\d+)\\)");
    }

    private int integer(Matcher matcher, String group) {
        return Integer.parseInt(matcher.group(group));
    }

    private void showResult(ActionResult result) {
        System.out.println(result.getMessage());
    }

    private void showLines(List<String> lines) {
        if (lines.isEmpty()) {
            System.out.println("No items.");
            return;
        }
        for (String line : lines) {
            System.out.println(line);
        }
    }
}
