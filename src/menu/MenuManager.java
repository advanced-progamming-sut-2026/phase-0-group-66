package menu;

import java.util.LinkedHashMap;
import java.util.Map;

public class MenuManager {
    private final Map<String, Menu> menus;
    private Menu currentMenu;
    private boolean running;

    public MenuManager() {
        menus = new LinkedHashMap<>();
        running = true;
    }

    public void registerMenu(Menu menu) {
        menus.put(menu.getName(), menu);
    }

    public boolean hasMenu(String menuName) {
        return menus.containsKey(menuName);
    }

    public void setCurrentMenu(Menu menu) {
        if (menu == null) {
            throw new IllegalArgumentException("Menu cannot be null.");
        }
        currentMenu = menu;
        currentMenu.enter();
    }

    public Menu getCurrentMenu() {
        return currentMenu;
    }

    public void enterMenu(String menuName) {
        Menu nextMenu = menus.get(menuName);
        if (nextMenu == null) {
            System.out.println("Menu not found!");
            return;
        }
        setCurrentMenu(nextMenu);
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() {
        running = false;
    }
}
