package menu;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Menu {
    private final String name;
    protected Menu parentMenu;
    protected final MenuManager menuManager;

    protected Menu(String name, MenuManager menuManager) {
        this.name = name;
        this.menuManager = menuManager;
    }

    public String getName() {
        return name;
    }

    public void setParentMenu(Menu parentMenu) {
        this.parentMenu = parentMenu;
    }

    public void handleCommand(String command) {
        String normalizedCommand = command == null ? "" : command.trim();
        if (normalizedCommand.equals("menu show current")) {
            System.out.println(name);
        } else if (normalizedCommand.equals("menu exit")) {
            exit();
        } else if (normalizedCommand.startsWith("menu enter ")) {
            String targetMenu = normalizedCommand.substring("menu enter ".length()).trim();
            handleMenuEnter(targetMenu);
        } else {
            processSpecificCommand(normalizedCommand);
        }
    }

    protected void handleMenuEnter(String targetMenu) {
        System.out.println("invalid navigation");
    }

    public void exit() {
        if (parentMenu != null) {
            menuManager.setCurrentMenu(parentMenu);
        } else {
            System.out.println("Exiting application...");
            menuManager.stop();
        }
    }

    public void enter() {
    }

    public abstract void showCommands();

    protected abstract void processSpecificCommand(String command);

    protected Matcher getMatcher(String input, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(input);
        return matcher.matches() ? matcher : null;
    }
}
