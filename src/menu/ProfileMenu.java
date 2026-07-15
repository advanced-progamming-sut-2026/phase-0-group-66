import java.util.regex.Matcher;

public class ProfileMenu extends Menu {
    public ProfileMenu(MenuManager menuManager) {
        super("Profile Menu", menuManager);
    }

    @Override
    public void showCommands() { }

    @Override
    protected void processSpecificCommand(String command) {
        Matcher changeUserMatcher = getMatcher(command, "menu profile change-username -u (?<username>\\S+)");
        Matcher changeNickMatcher = getMatcher(command, "menu profile change-nickname -u (?<nickname>\\S+)");
        Matcher changeEmailMatcher = getMatcher(command, "menu profile change-email -e (?<email>\\S+)");
        Matcher changePassMatcher = getMatcher(command, "menu profile change-password -p (?<newPass>\\S+) -o (?<oldPass>\\S+)");

        if (changeUserMatcher != null) {
            String newUsername = changeUserMatcher.group("username");
        } else if (changeNickMatcher != null) {
            String newNickname = changeNickMatcher.group("nickname");
        } else if (changeEmailMatcher != null) {
            String newEmail = changeEmailMatcher.group("email");
        } else if (changePassMatcher != null) {
            String newPass = changePassMatcher.group("newPass");
            String oldPass = changePassMatcher.group("oldPass");
        } else if (command.equals("menu profile show-info")) {
        } else {
            System.out.println("invalid command");
        }
    }
}