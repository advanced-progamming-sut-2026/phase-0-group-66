import java.util.regex.Matcher;

public class MiniGameMenu extends Menu {
    public MiniGameMenu(MenuManager menuManager) {
        super("MiniGame Menu", menuManager);
    }

    @Override
    public void showCommands() { }

    @Override
    protected void processSpecificCommand(String command) {
        Matcher startGameMatcher = getMatcher(command, "start minigame -n (?<gameName>\\S+) -l (?<level>\\d+)");

        if (command.equals("show minigames status")) {
        } else if (startGameMatcher != null) {
            String gameName = startGameMatcher.group("gameName");
            int level = Integer.parseInt(startGameMatcher.group("level"));

        } else {
            System.out.println("invalid command");
        }
    }
}