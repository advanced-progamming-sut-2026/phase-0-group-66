package menu;

import controller.ActionResult;
import controller.GameController;

import java.util.List;
import java.util.regex.Matcher;

public class GameMenu extends Menu {
    private final GameController controller;

    public GameMenu(MenuManager menuManager, GameController controller) {
        super("Game Menu", menuManager);
        this.controller = controller;
    }

    @Override
    protected void handleMenuEnter(String targetMenu) {
        if (targetMenu.equals("Collection Menu")) {
            menuManager.enterMenu(targetMenu);
            return;
        }
        Matcher withLevel = getMatcher(targetMenu,
            "chapter -c (?<chapter>.+?) -l (?<level>\\d+)");
        Matcher withoutLevel = getMatcher(targetMenu, "chapter -c (?<chapter>.+)");
        ActionResult result = null;
        if (withLevel != null) {
            result = controller.startLevel(withLevel.group("chapter").trim(),
                Integer.parseInt(withLevel.group("level")));
        } else if (withoutLevel != null) {
            result = controller.startLevel(withoutLevel.group("chapter").trim());
        }
        if (result != null) {
            System.out.println(result.getMessage());
            if (result.isSuccessful()) {
                menuManager.enterMenu("Plant Selection Menu");
            }
        } else {
            super.handleMenuEnter(targetMenu);
        }
    }

    @Override
    public void showCommands() {
        System.out.println("show chapters");
        System.out.println("show levels -c <chapter_name>");
        System.out.println("menu enter chapter -c <chapter_name> -l <1-4>");
        System.out.println("start scored game | show score status");
        System.out.println("menu enter Collection Menu");
        System.out.println("menu greenhouse | menu travel-log | menu leaderboard");
        System.out.println("menu coin-wallet | menu gem-wallet");
        System.out.println("menu cheat add <n> <coin|diamond>");
        System.out.println("menu cheat unlock-all-levels");
    }

    @Override
    protected void processSpecificCommand(String command) {
        Matcher levels = getMatcher(command, "show levels -c (?<chapter>.+)");
        Matcher wallet = getMatcher(command,
            "menu cheat add (?<count>\\d+) (?<type>coin|diamond)");
        if (command.equals("start scored game")) {
            ActionResult result = controller.startScoredGame();
            System.out.println(result.getMessage());
            if (result.isSuccessful()) {
                menuManager.enterMenu("Plant Selection Menu");
            }
        } else if (command.equals("show score status")) {
            System.out.println(controller.scoreStatus());
        } else if (command.equals("show chapters")) {
            showLines(controller.getChapterDescriptions());
        } else if (levels != null) {
            showLines(controller.getLevelDescriptions(levels.group("chapter").trim()));
        } else if (command.equals("menu greenhouse")) {
            menuManager.enterMenu("Greenhouse Menu");
        } else if (command.equals("menu travel-log")) {
            menuManager.enterMenu("Quest Menu");
        } else if (command.equals("menu leaderboard")) {
            menuManager.enterMenu("Leaderboard Menu");
        } else if (command.equals("menu coin-wallet")) {
            System.out.println(controller.walletAmount("coin"));
        } else if (command.equals("menu gem-wallet")) {
            System.out.println(controller.walletAmount("diamond"));
        } else if (wallet != null) {
            System.out.println(controller.addWalletCurrency(
                Integer.parseInt(wallet.group("count")), wallet.group("type")).getMessage());
        } else if (command.equals("menu cheat unlock-all-levels")) {
            System.out.println(controller.unlockAllLevels().getMessage());
        } else if (command.equals("show commands")) {
            showCommands();
        } else {
            System.out.println("invalid command");
        }
    }

    private void showLines(List<String> lines) {
        if (lines.isEmpty()) {
            System.out.println("No items.");
        } else {
            lines.forEach(System.out::println);
        }
    }
}
