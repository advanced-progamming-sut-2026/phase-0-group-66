package menu;

public class NewsMenu extends Menu {
    public NewsMenu(MenuManager menuManager) {
        super("News Menu", menuManager);
    }

    @Override
    public void showCommands() { }

    @Override
    protected void processSpecificCommand(String command) {
        if (command.equals("menu news show-unread")) {
        } else if (command.equals("menu news show-all")) {
        } else {
            System.out.println("invalid command");
        }
    }
}
