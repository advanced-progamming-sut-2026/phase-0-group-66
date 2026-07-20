package menu;

import controller.NewsController;
import view.NewsView;

public class NewsMenu extends Menu {
    private final NewsController controller;
    private final NewsView view;

    public NewsMenu(MenuManager menuManager, NewsController controller, NewsView view) {
        super("News Menu", menuManager);
        this.controller = controller;
        this.view = view;
    }

    @Override
    public void showCommands() {
        System.out.println("menu news show-unread");
        System.out.println("menu news show-all");
    }

    @Override
    protected void processSpecificCommand(String command) {
        if (command.equals("menu news show-unread")) {
            view.showNews(controller.showUnreadNews());
        } else if (command.equals("menu news show-all")) {
            view.showNews(controller.showAllNews());
        } else if (command.equals("show commands")) {
            showCommands();
        } else {
            System.out.println("invalid command");
        }
    }
}
