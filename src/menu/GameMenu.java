package menu;

import java.util.regex.Matcher;

public class GameMenu extends Menu {
    public GameMenu(MenuManager menuManager) {
        super("Game Menu", menuManager);
    }

    @Override
    protected void handleMenuEnter(String targetMenu) {
        if (targetMenu.equals("Collection Menu")) {
            menuManager.enterMenu("Collection Menu");
        } else {
            super.handleMenuEnter(targetMenu);
        }
    }

    @Override
    public void showCommands() { }

    @Override
    protected void processSpecificCommand(String command) {
        Matcher chapterMatcher = getMatcher(command, "menu enter chapter -c (?<chapter>.+)");
        Matcher cheatMatcher = getMatcher(command, "menu cheat add (?<n>\\d+) (?<type>coin|diamond)");

        if (chapterMatcher != null) {
        } else if (command.equals("menu greenhouse")) {
            menuManager.enterMenu("Greenhouse Menu");
        } else if (command.equals("menu travel-log")) {
            menuManager.enterMenu("Quest Menu");
        } else if (command.equals("menu leaderboard")) {
            menuManager.enterMenu("Leaderboard Menu");
        } else if (command.equals("menu coin-wallet") || command.equals("menu gem-wallet")) {
        } else if (cheatMatcher != null) {
        } else {
            System.out.println("invalid command");
        }
    }
}
