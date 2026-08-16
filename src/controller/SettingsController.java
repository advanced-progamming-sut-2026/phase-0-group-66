package controller;

import model.User;

public class SettingsController {
    private final AuthController authController;

    public SettingsController(AuthController authController) {
        this.authController = authController;
    }

    public ActionResult changeDifficulty(int level) {
        User user = currentUser();
        if (user == null) {
            return ActionResult.failure("You must login first.");
        }
        if (level < 1 || level > 5) {
            return ActionResult.failure("Difficulty level must be between 1 and 5.");
        }
        user.setDifficultyLevel(level);
        return save("Difficulty changed to " + level + ".");
    }

    public ActionResult changeGameSpeed(int speed) {
        User user = currentUser();
        if (user == null) {
            return ActionResult.failure("You must login first.");
        }
        if (speed < 1 || speed > 3) {
            return ActionResult.failure("Game speed must be between 1 and 3.");
        }
        user.setGameSpeed(speed);
        return save("Game speed changed to " + speed + ".");
    }

    public ActionResult changeGridVisible(boolean visible) {
        User user = currentUser();
        if (user == null) {
            return ActionResult.failure("You must login first.");
        }
        user.setGridVisible(visible);
        return save("Battle grid " + (visible ? "enabled." : "disabled."));
    }

    public ActionResult changeDebugMode(boolean enabled) {
        User user = currentUser();
        if (user == null) {
            return ActionResult.failure("You must login first.");
        }
        user.setDebugMode(enabled);
        return save("Debug mode " + (enabled ? "enabled." : "disabled."));
    }

    private User currentUser() {
        return authController.getCurrentUser();
    }

    private ActionResult save(String successMessage) {
        ActionResult result = authController.saveCurrentState();
        return result.isSuccessful() ? ActionResult.success(successMessage) : result;
    }
}
