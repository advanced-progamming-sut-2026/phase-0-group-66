public abstract class Menu {
    protected String name;
    protected Menu parentMenu;
    protected MenuManager menuManager;

    public Menu(String name, MenuManager menuManager) {
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
        if (command.equals("menu show current")) {
            System.out.println(this.name);
        } else if (command.equals("menu exit")) {
            exit();
        } else if (command.startsWith("menu enter ")) {
            String targetMenu = command.substring(11).trim();
            handleMenuEnter(targetMenu);
        } else {
            processSpecificCommand(command);
        }
    }

    protected void handleMenuEnter(String targetMenu) {
        if (targetMenu.equals("Register Menu") || targetMenu.equals("Login Menu")) {
            menuManager.enterMenu(targetMenu);
        } else {
            System.out.println("invalid navigation");
        }
    }

    public void exit() {
        if (parentMenu != null) {
            menuManager.setCurrentMenu(parentMenu);
        } else {
            System.out.println("Exiting application...");
            System.exit(0);
        }
    }

    public void enter() {
    }

    public abstract void showCommands();
    protected abstract void processSpecificCommand(String command);

    protected Matcher getMatcher(String input, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        return matcher.matches() ? matcher : null;
    }
}