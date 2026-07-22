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
        System.out.println("minigame command <game-specific command>");
        System.out.println("advance minigame -t <ticks>");
        System.out.println("show minigame current | show minigame board | show minigame help");
    }

    @Override
    protected void processSpecificCommand(String command) {
        Matcher start = getMatcher(command,
            "start minigame -n (?<gameName>.+?) -l (?<level>[1-3])");
        Matcher action = getMatcher(command, "minigame command (?<gameCommand>.+)");
        Matcher advance = getMatcher(command, "advance minigame -t (?<ticks>\\d+)");
        if (command.equals("show minigames status")) {
            view.showMiniGames(controller.showMiniGamesStatus());
        } else if (start != null) {
            show(controller.startMiniGame(start.group("gameName"),
                Integer.parseInt(start.group("level"))));
        } else if (action != null) {
            show(controller.executeCommand(action.group("gameCommand")));
        } else if (advance != null) {
            show(controller.advanceTime(Integer.parseInt(advance.group("ticks"))));
        } else if (command.equals("show minigame current")) {
            view.showMessage(controller.currentStatus());
        } else if (command.equals("show minigame board")) {
            view.showMessage(controller.currentBoard());
        } else if (command.equals("show minigame help")) {
            view.showMessage(controller.currentHelp());
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
