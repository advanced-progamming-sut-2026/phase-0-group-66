package menu;

import controller.ActionResult;
import controller.AuthController;

import view.RegisterView;

import java.util.regex.Matcher;

public class RegisterMenu extends Menu {
    private final AuthController authController;
    private final RegisterView registerView;

    public RegisterMenu(MenuManager menuManager, AuthController authController,
                        RegisterView registerView) {
        super("Register Menu", menuManager);
        this.authController = authController;
        this.registerView = registerView;
    }


    @Override
    protected void handleMenuEnter(String targetMenu) {
        if (targetMenu.equals("Login Menu")) {
            menuManager.enterMenu(targetMenu);
        } else {
            super.handleMenuEnter(targetMenu);
        }
    }

    @Override
    public void showCommands() {
        registerView.showRegisterForm();
    }

    @Override
    protected void processSpecificCommand(String command) {
        Matcher registerMatcher = getMatcher(command,
            "register -u (?<username>\\S+) -p (?<password>\\S+) (?<passwordConfirm>\\S+) "
                + "-n (?<nickname>\\S+) -e (?<email>\\S+) -g (?<gender>\\S+)");
        Matcher questionMatcher = getMatcher(command,
            "pick question -q (?<qNum>\\d+) -a (?<ans>.+) -c (?<ansConf>.+)");

        if (registerMatcher != null) {
            ActionResult result = authController.register(
                registerMatcher.group("username"),
                registerMatcher.group("password"),
                registerMatcher.group("passwordConfirm"),
                registerMatcher.group("nickname"),
                registerMatcher.group("email"),
                registerMatcher.group("gender"));
            registerView.showMessage(result.getMessage());
        } else if (questionMatcher != null) {
            int questionNumber = Integer.parseInt(questionMatcher.group("qNum"));
            ActionResult result = authController.pickSecurityQuestion(
                questionNumber,
                questionMatcher.group("ans"),
                questionMatcher.group("ansConf"));
            registerView.showMessage(result.getMessage());
            if (result.isSuccessful()) {
                menuManager.enterMenu("Login Menu");
            }
        } else if (command.equals("show commands")) {
            showCommands();
        } else {
            System.out.println("invalid command");
        }
    }
}
