package controller;

import model.User;

public class SettingsController {
    private final AuthController authController;

    public SettingsController(AuthController authController) {
        this.authController = authController;
    }

    public ActionResult changeDifficulty(int level) {
        User user = authController.getCurrentUser();
        if (user == null) {
            return ActionResult.failure("You must login first.");
        }
        if (level < 1 || level > 5) {
            return ActionResult.failure("Difficulty level must be between 1 and 5.");
        }
        user.setDifficultyLevel(level);
        ActionResult saveResult = authController.saveCurrentState();
        if (!saveResult.isSuccessful()) {
            return saveResult;
        }
        return ActionResult.success("Difficulty changed to " + level + ".");
    }
}
