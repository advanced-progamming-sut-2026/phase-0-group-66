package menu;

import controller.ActionResult;
import controller.AuthController;

import view.LoginView;

import java.util.regex.Matcher;

public class LoginMenu extends Menu {
    private final AuthController authController;
    private final LoginView loginView;

    public LoginMenu(MenuManager menuManager, AuthController authController,
                     LoginView loginView) {
        super("Login Menu", menuManager);
        this.authController = authController;
        this.loginView = loginView;
    }

    @Override
    protected void handleMenuEnter(String targetMenu) {
        if (targetMenu.equals("Main Menu")) {
            if (authController.isAuthenticated()) {
                menuManager.enterMenu("Main Menu");
            } else {
                System.out.println("You must login first.");
            }
        } else {
            super.handleMenuEnter(targetMenu);
        }
    }

    @Override
    public void showCommands() {
        loginView.showLoginForm();
    }

    @Override
    protected void processSpecificCommand(String command) {
        Matcher loginMatcher = getMatcher(command,
            "login -u (?<username>\\S+) -p (?<password>\\S+)(?<stay> -stay-logged-in)?");
        Matcher forgetMatcher = getMatcher(command,
            "forget password -u (?<username>\\S+) -e (?<email>\\S+)");
        Matcher answerMatcher = getMatcher(command, "answer -a (?<answer>.+)");
        Matcher resetMatcher = getMatcher(command,
            "reset password -p (?<password>\\S+) (?<passwordConfirm>\\S+)");

        ActionResult result;
        if (loginMatcher != null) {
            result = authController.login(
                loginMatcher.group("username"),
                loginMatcher.group("password"),
                loginMatcher.group("stay") != null);
            loginView.showMessage(result.getMessage());
            if (result.isSuccessful()) {
                menuManager.enterMenu("Main Menu");
            }
        } else if (forgetMatcher != null) {
            result = authController.forgetPassword(
                forgetMatcher.group("username"), forgetMatcher.group("email"));
            loginView.showMessage(result.getMessage());
        } else if (answerMatcher != null) {
            result = authController.answerSecurityQuestion(answerMatcher.group("answer"));
            loginView.showMessage(result.getMessage());
        } else if (resetMatcher != null) {
            result = authController.resetPassword(
                resetMatcher.group("password"), resetMatcher.group("passwordConfirm"));
            loginView.showMessage(result.getMessage());
        } else if (command.equals("show commands")) {
            showCommands();
        } else {
            System.out.println("invalid command");
        }
    }
}
