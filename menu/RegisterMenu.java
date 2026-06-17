public class RegisterMenu extends Menu {
    public RegisterMenu(MenuManager menuManager) {
        super("Register Menu", menuManager);
        this.parentMenu = null;
    }

    @Override
    public void showCommands() { }

    @Override
    protected void processSpecificCommand(String command) {
        Matcher registerMatcher = getMatcher(command, "register -u (?<username>\\S+) -p (?<password>\\S+) (?<passwordConfirm>\\S+) -n (?<nickname>\\S+) -e (?<email>\\S+) -g (?<gender>\\S+)");
        Matcher questionMatcher = getMatcher(command, "pick question -q (?<qNum>\\d+) -a (?<ans>.+) -c (?<ansConf>.+)");

        if (registerMatcher != null) {
        } else if (questionMatcher != null) {
        } else {
            System.out.println("invalid command");
        }
    }
}