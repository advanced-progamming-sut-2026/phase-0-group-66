package menu;

import java.util.regex.Matcher;

public class GreenhouseMenu extends Menu {
    public GreenhouseMenu(MenuManager menuManager) {
        super("Greenhouse Menu", menuManager);
    }

    @Override
    public void showCommands() { }

    @Override
    protected void processSpecificCommand(String command) {
        Matcher plantPotMatcher = getMatcher(command, "plant pot at \\((?<x>\\d+),\\s*(?<y>\\d+)\\)");
        Matcher collectMatcher = getMatcher(command, "collect \\((?<x>\\d+),\\s*(?<y>\\d+)\\)");
        Matcher growMatcher = getMatcher(command, "grow \\((?<x>\\d+),\\s*(?<y>\\d+)\\)");

        if (command.equals("show greenhouse")) {
        } else if (command.equals("enter shop")) {
            menuManager.enterMenu("Shop Menu");
        } else if (plantPotMatcher != null) {
        } else if (collectMatcher != null) {
        } else if (growMatcher != null) {
        } else {
            System.out.println("invalid command");
        }
    }
}
