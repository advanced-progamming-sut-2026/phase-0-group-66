package menu;

public class NetworkMenu extends Menu {
    public NetworkMenu(MenuManager menuManager) {
        super("Network Menu", menuManager);
    }

    @Override
    public void showCommands() {
        System.out.println("show commands");
        System.out.println("No network commands are available in this phase.");
    }

    @Override
    protected void processSpecificCommand(String command) {
        if (command.equals("show commands")) {
            showCommands();
        } else {
            System.out.println("invalid command");
        }
    }
}
