package menu;

import controller.ActionResult;
import controller.QuestController;
import view.QuestView;

import java.util.regex.Matcher;

public class QuestMenu extends Menu {
    private final QuestController controller;
    private final QuestView view;

    public QuestMenu(MenuManager menuManager, QuestController controller, QuestView view) {
        super("Quest Menu", menuManager);
        this.controller = controller;
        this.view = view;
    }

    @Override
    public void showCommands() {
        System.out.println("travel log page <daily/main/epic/all>");
        System.out.println("quest claim -i <quest_id>");
        System.out.println("menu enter MiniGame Menu");
    }

    @Override
    protected void handleMenuEnter(String targetMenu) {
        if (targetMenu.equals("MiniGame Menu")) {
            menuManager.enterMenu(targetMenu);
        } else {
            super.handleMenuEnter(targetMenu);
        }
    }

    @Override
    protected void processSpecificCommand(String command) {
        Matcher page = getMatcher(command, "travel log page (?<pageName>daily|main|epic|all)");
        Matcher claim = getMatcher(command, "quest claim -i (?<id>\\d+)");
        if (page != null) {
            view.showQuests(controller.getQuestsPage(page.group("pageName")));
        } else if (claim != null) {
            ActionResult result = controller.claimReward(Integer.parseInt(claim.group("id")));
            view.showMessage(result.getMessage());
        } else if (command.equals("show commands")) {
            showCommands();
        } else {
            view.showMessage("invalid command");
        }
    }
}
