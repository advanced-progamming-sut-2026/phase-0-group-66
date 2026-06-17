import java.util.regex.Matcher;

public class LeaderboardMenu extends Menu {
    public LeaderboardMenu(MenuManager menuManager) {
        super("Leaderboard Menu", menuManager);
    }

    @Override
    public void showCommands() { }

    @Override
    protected void processSpecificCommand(String command) {
        Matcher sortMatcher = getMatcher(command, "leaderboard sort -c (?<column>\\S+) -o (?<order>asc|desc)");

        if (command.equals("show leaderboard")) {
        } else if (sortMatcher != null) {
            String column = sortMatcher.group("column");
            String order = sortMatcher.group("order");
        } else {
            System.out.println("invalid command");
        }
    }
}