package menu;

import controller.LeaderboardController;
import view.LeaderboardView;

import java.util.regex.Matcher;

public class LeaderboardMenu extends Menu {
    private final LeaderboardController controller;
    private final LeaderboardView view;

    public LeaderboardMenu(MenuManager menuManager, LeaderboardController controller,
                           LeaderboardView view) {
        super("Leaderboard Menu", menuManager);
        this.controller = controller;
        this.view = view;
    }

    @Override
    public void showCommands() {
        System.out.println("show leaderboard");
        System.out.println("leaderboard sort -c <column> -o <asc|desc>");
    }

    @Override
    protected void processSpecificCommand(String command) {
        Matcher sort = getMatcher(command,
            "leaderboard sort -c (?<column>\\S+) -o (?<order>asc|desc)");
        try {
            if (command.equals("show leaderboard")) {
                view.showLeaderboard(controller.getLeaderboard("score", "desc"));
            } else if (sort != null) {
                view.showLeaderboard(controller.getLeaderboard(sort.group("column"),
                    sort.group("order")));
            } else if (command.equals("show commands")) {
                showCommands();
            } else {
                System.out.println("invalid command");
            }
        } catch (IllegalArgumentException exception) {
            System.out.println(exception.getMessage());
        }
    }
}
