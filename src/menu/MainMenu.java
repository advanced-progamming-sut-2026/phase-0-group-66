public class MainMenu extends Menu {
    public MainMenu(MenuManager menuManager) {
        super("Main Menu", menuManager);
    }

    @Override
    protected void handleMenuEnter(String targetMenu) {
        List<String> validDestinations = Arrays.asList("Game Menu", "Settings Menu", "News Menu", "Profile Menu", "Leaderboard Menu");
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
    public void showCommands() { }

    @Override
    protected void processSpecificCommand(String command) {
        if (command.equals("menu logout")) {
            menuManager.enterMenu("Register Menu");
        } else {
            System.out.println("invalid command");
        }
    }
}