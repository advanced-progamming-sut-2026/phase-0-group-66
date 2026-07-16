package menu;

import controller.QuestController;
import model.QuestDefinition;
import view.QuestView;

import java.util.List;
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
    }

    @Override
    protected void processSpecificCommand(String command) {
        Matcher pageMatcher = getMatcher(command, "travel log page (?<pageName>.+)");
        if (pageMatcher == null) {
            view.showMessage("invalid command");
            return;
        }
        List<QuestDefinition> quests = controller.getQuestsPage(pageMatcher.group("pageName").trim());
        view.showQuestDefinitions(quests);
    }
}
