public class AppController {
    private AuthController authController;
    private MenuController menuController;

    public void startApplication() {
        loadInitialData();
    }
    public void saveAndExit() {
        System.out.println("Saving data and exiting...");
        System.exit(0);
    }
    public void loadInitialData() {
    }
}