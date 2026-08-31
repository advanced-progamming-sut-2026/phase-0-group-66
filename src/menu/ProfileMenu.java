package menu;

import controller.ActionResult;
import controller.ProfileController;

import view.ProfileView;

import java.util.regex.Matcher;

public class ProfileMenu extends Menu {
    private final ProfileController profileController;
    private final ProfileView profileView;

    public ProfileMenu(MenuManager menuManager, ProfileController profileController,
                       ProfileView profileView) {
        super("Profile Menu", menuManager);
        this.profileController = profileController;
        this.profileView = profileView;
    }

    @Override
    public void showCommands() {
        System.out.println("menu profile change-username -u <username>");
        System.out.println("menu profile change-nickname -n <nickname>");
        System.out.println("menu profile change-email -e <email>");
        System.out.println("menu profile change-password -p <new_password> -o <old_password>");
        System.out.println("menu profile show-info");
    }

    @Override
    protected void processSpecificCommand(String command) {
        Matcher changeUserMatcher = getMatcher(command,
            "menu profile change-username -u (?<username>\\S+)");
        Matcher changeNickMatcher = getMatcher(command,
            "menu profile change-nickname -n (?<nickname>\\S+)");
        Matcher changeEmailMatcher = getMatcher(command,
            "menu profile change-email -e (?<email>\\S+)");
        Matcher changePassMatcher = getMatcher(command,
            "menu profile change-password -p (?<newPass>\\S+) -o (?<oldPass>\\S+)");

        ActionResult result;
        if (changeUserMatcher != null) {
            result = profileController.changeUsername(changeUserMatcher.group("username"));
        } else if (changeNickMatcher != null) {
            result = profileController.changeNickname(changeNickMatcher.group("nickname"));
        } else if (changeEmailMatcher != null) {
            result = profileController.changeEmail(changeEmailMatcher.group("email"));
        } else if (changePassMatcher != null) {
            result = profileController.changePassword(
                changePassMatcher.group("oldPass"), changePassMatcher.group("newPass"));
        } else if (command.equals("menu profile show-info")) {
            result = profileController.showProfile();
            if (result.isSuccessful()) {
                profileView.showProfileInfo(profileController.getCurrentUser());
                return;
            }
        } else if (command.equals("show commands")) {
            showCommands();
            return;
        } else {
            System.out.println("invalid command");
            return;
        }
        profileView.showMessage(result.getMessage());
    }
}
