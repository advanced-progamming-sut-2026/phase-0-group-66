import java.util.regex.Matcher;

public class SettingsMenu extends Menu {
    public SettingsMenu(MenuManager menuManager) {
        super("Settings Menu", menuManager);
    }

    @Override
    public void showCommands() { }

    @Override
    protected void processSpecificCommand(String command) {
        Matcher diffMatcher = getMatcher(command, "menu settings change-difficulty -l (?<level>\\d+)");

        if (diffMatcher != null) {
            int level = Integer.parseInt(diffMatcher.group("level"));
        } else {
            System.out.println("invalid command");
        }
    }
}