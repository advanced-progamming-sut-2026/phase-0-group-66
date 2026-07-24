package menu;

import controller.ActionResult;
import controller.AuthController;
import controller.GameController;
import controller.NewsController;

import java.util.Arrays;
import java.util.List;

public class MainMenu extends Menu {
    private final AuthController authController;
    private final NewsController newsController;
    private final GameController gameController;

    public MainMenu(MenuManager menuManager, AuthController authController,
                    NewsController newsController, GameController gameController) {
        super("Main Menu", menuManager);
        this.authController = authController;
        this.newsController = newsController;
        this.gameController = gameController;
    }

    @Override
    protected void handleMenuEnter(String targetMenu) {
        if (!authController.isAuthenticated()) {
            System.out.println("You must login first.");
            return;
        }
        List<String> validDestinations = Arrays.asList(
            "Game Menu", "Settings Menu", "Network Menu", "News Menu",
            "Profile Menu", "Leaderboard Menu");
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
        System.out.println("menu enter <Game Menu|Settings Menu|Network Menu|News Menu|"
            + "Profile Menu|Leaderboard Menu>");
        if (newsController.hasUnreadNews()) {
            System.out.println("News Menu status: NEW");
        }
        System.out.println("start scored game | show score status");
        System.out.println("menu logout");
    }

    @Override
    protected void processSpecificCommand(String command) {
        if (command.equals("menu logout")) {
            ActionResult result = authController.logout();
            System.out.println(result.getMessage());
            menuManager.enterMenu("Register Menu");
        } else if (command.equals("start scored game")) {
            startScoredGame();
        } else if (command.equals("show score status")) {
            System.out.println(gameController.scoreStatus());
        } else if (command.equals("show commands")) {
            showCommands();
        } else {
            System.out.println("invalid command");
        }
    }

    private void startScoredGame() {
        ActionResult result = gameController.startScoredGame();
        System.out.println(result.getMessage());
        if (result.isSuccessful()) {
            menuManager.enterMenu("Plant Selection Menu");
        }
    }
}
