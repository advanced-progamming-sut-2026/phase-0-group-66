package controller;

public class AppController {
    private final AuthController authController;

    public AppController(AuthController authController) {
        this.authController = authController;
    }

    public boolean startApplication() {
        return authController.restoreSession();
    }

    public void saveAndExit() {
        ActionResult result = authController.saveCurrentState();
        if (!result.isSuccessful()) {
            System.err.println(result.getMessage());
        }
    }
}
