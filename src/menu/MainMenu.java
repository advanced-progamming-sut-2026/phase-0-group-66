package menu;

import controller.ActionResult;
import controller.AuthController;
import controller.NewsController;

import java.util.Arrays;
import java.util.List;

public class MainMenu extends Menu {
    private final AuthController authController;
    private final NewsController newsController;

    public MainMenu(MenuManager menuManager, AuthController authController,
                    NewsController newsController) {
        super("Main Menu", menuManager);
        this.authController = authController;
        this.newsController = newsController;
    }

    @Override
    protected void handleMenuEnter(String targetMenu) {
        if (!authController.isAuthenticated()) {
            System.out.println("You must login first.");
            return;
        }
        List<String> validDestinations = Arrays.asList(
            "Game Menu", "Settings Menu", "News Menu", "Profile Menu", "Leaderboard Menu");
        if (validDestinations.contains(targetMenu)) {
            menuManager.enterMenu(targetMenu);
        } else {
            super.handleMenuEnter(targetMenu);
        }
    }

    @Override
    public void exit() {
        System.out.println("You must use 'menu logout' to leave Main Menu.");
    }

    @Override
    public void showCommands() {
        String newsLabel = newsController.hasUnreadNews() ? "News Menu [NEW]" : "News Menu";
        System.out.println("menu enter <Game Menu|Settings Menu|" + newsLabel
            + "|Profile Menu|Leaderboard Menu>");
        System.out.println("menu logout");
    }

    @Override
    protected void processSpecificCommand(String command) {
        if (command.equals("menu logout")) {
            ActionResult result = authController.logout();
            System.out.println(result.getMessage());
            menuManager.enterMenu("Register Menu");
        } else if (command.equals("show commands")) {
            showCommands();
        } else {
            System.out.println("invalid command");
        }
    }
}
