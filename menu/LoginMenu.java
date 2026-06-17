public class LoginMenu extends Menu {
    public LoginMenu(MenuManager menuManager) {
        super("Login Menu", menuManager);
    }

    @Override
    protected void handleMenuEnter(String targetMenu) {
        if (targetMenu.equals("Main Menu")) {
            menuManager.enterMenu("Main Menu");
        } else {
            super.handleMenuEnter(targetMenu);
        }
    }

    @Override
    public void showCommands() { }

    @Override
    protected void processSpecificCommand(String command) {
        Matcher loginMatcher = getMatcher(command, "login -u (?<username>\\S+) -p (?<password>\\S+)(?<stay> -stay-logged-in)?");
        Matcher forgetMatcher = getMatcher(command, "forget password -u (?<username>\\S+) -e (?<email>\\S+)");
        Matcher answerMatcher = getMatcher(command, "answer -a (?<answer>.+)");

        if (loginMatcher != null) {
        } else if (forgetMatcher != null) {
        } else if (answerMatcher != null) {
        } else {
            System.out.println("invalid command");
        }
    }
}