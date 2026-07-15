package menu;

import controller.ActionResult;
import controller.SettingsController;

import view.SettingsView;

import java.util.regex.Matcher;

public class SettingsMenu extends Menu {
    private final SettingsController settingsController;
    private final SettingsView settingsView;

    public SettingsMenu(MenuManager menuManager, SettingsController settingsController,
                        SettingsView settingsView) {
        super("Settings Menu", menuManager);
        this.settingsController = settingsController;
        this.settingsView = settingsView;
    }

    @Override
    public void showCommands() {
        settingsView.showSettings();
    }

    @Override
    protected void processSpecificCommand(String command) {
        Matcher difficultyMatcher = getMatcher(command,
            "menu settings change-difficulty -l (?<level>\\d+)");
        if (difficultyMatcher != null) {
            int level = Integer.parseInt(difficultyMatcher.group("level"));
            ActionResult result = settingsController.changeDifficulty(level);
            settingsView.showMessage(result.getMessage());
        } else if (command.equals("show commands")) {
            showCommands();
        } else {
            System.out.println("invalid command");
        }
    }
}
