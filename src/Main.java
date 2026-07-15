public class Main {
    public static void main(String[] args) {
        MenuManager manager = new MenuManager();

        RegisterMenu registerMenu = new RegisterMenu(manager);
        LoginMenu loginMenu = new LoginMenu(manager);
        MainMenu mainMenu = new MainMenu(manager);
        GameMenu gameMenu = new GameMenu(manager);
        CollectionMenu collectionMenu = new CollectionMenu(manager);
        GreenhouseMenu greenhouseMenu = new GreenhouseMenu(manager);
        ShopMenu shopMenu = new ShopMenu(manager);

        loginMenu.setParentMenu(registerMenu);
        gameMenu.setParentMenu(mainMenu);
        collectionMenu.setParentMenu(gameMenu);
        greenhouseMenu.setParentMenu(gameMenu);
        shopMenu.setParentMenu(greenhouseMenu);

        manager.registerMenu(registerMenu);
        manager.registerMenu(loginMenu);
        manager.registerMenu(mainMenu);
        manager.registerMenu(gameMenu);
        manager.registerMenu(collectionMenu);
        manager.registerMenu(greenhouseMenu);
        manager.registerMenu(shopMenu);

        manager.setCurrentMenu(registerMenu);

        Scanner scanner = new Scanner(System.in);
        while (true) {
            String input = scanner.nextLine();
            manager.getCurrentMenu().handleCommand(input);
        }
    }
}