public class MenuController {
    private MenuManager menuManager;

    public MenuController(MenuManager menuManager) {
        this.menuManager = menuManager;
    }

    public void enterMenu(String menuName) {
        menuManager.enterMenu(menuName);
    }

    public void exitMenu() {
        menuManager.getCurrentMenu().exit();
    }

    public void showCurrentMenu() {
        System.out.println(menuManager.getCurrentMenu().getName());
    }
}