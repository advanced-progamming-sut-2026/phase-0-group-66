import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MenuManager {
    private Menu currentMenu;
    private Map<String, Menu> menus;

    public MenuManager() {
        menus = new HashMap<>();
    }

    public void registerMenu(Menu menu) {
        menus.put(menu.getName(), menu);
    }

    public void setCurrentMenu(Menu menu) {
        this.currentMenu = menu;
        this.currentMenu.enter();
    }

    public Menu getCurrentMenu() {
        return currentMenu;
    }

    public void enterMenu(String menuName) {
        Menu nextMenu = menus.get(menuName);
        if (nextMenu != null) {
            setCurrentMenu(nextMenu);
        } else {
            System.out.println("Menu not found!");
        }
    }
}