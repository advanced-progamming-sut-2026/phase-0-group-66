package menu;

import controller.ActionResult;
import controller.MiniGameController;
import view.MiniGameView;

import java.util.regex.Matcher;

public class MiniGameMenu extends Menu {
    private final MiniGameController controller;
    private final MiniGameView view;

    public MiniGameMenu(MenuManager menuManager, MiniGameController controller,
                        MiniGameView view) {
        super("MiniGame Menu", menuManager);
        this.controller = controller;
        this.view = view;
    }

    @Override
    public void showCommands() {
        System.out.println("show minigames status");
        System.out.println("start minigame -n <name> -l <1-3>");
        System.out.println("minigame action -a <action> -n <amount>");
        System.out.println("show minigame current");
    }

    @Override
    protected void processSpecificCommand(String command) {
        Matcher start = getMatcher(command,
            "start minigame -n (?<gameName>.+?) -l (?<level>[1-3])");
        Matcher action = getMatcher(command,
            "minigame action -a (?<action>\\S+) -n (?<amount>\\d+)");
        if (command.equals("show minigames status")) {
            view.showMiniGames(controller.showMiniGamesStatus());
        } else if (start != null) {
            show(controller.startMiniGame(start.group("gameName"),
                Integer.parseInt(start.group("level"))));
        } else if (action != null) {
            show(controller.performAction(action.group("action"),
                Integer.parseInt(action.group("amount"))));
        } else if (command.equals("show minigame current")) {
            view.showMessage(controller.currentStatus());
        } else if (command.equals("show commands")) {
            showCommands();
        } else {
            view.showMessage("invalid command");
        }
    }

    private void show(ActionResult result) {
        view.showMessage(result.getMessage());
    }
}
