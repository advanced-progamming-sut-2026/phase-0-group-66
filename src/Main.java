import controller.AppController;
import controller.AuthController;
import controller.ProfileController;
import controller.SettingsController;
import menu.CollectionMenu;
import menu.GameMenu;
import menu.GreenhouseMenu;
import menu.LeaderboardMenu;
import menu.LoginMenu;
import menu.MainMenu;
import menu.Menu;
import menu.MenuManager;
import menu.MiniGameMenu;
import menu.NewsMenu;
import menu.ProfileMenu;
import menu.QuestMenu;
import menu.RegisterMenu;
import menu.SettingsMenu;
import menu.ShopMenu;
import model.UserRepository;
import view.LoginView;
import view.ProfileView;
import view.RegisterView;
import view.SettingsView;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try {
            runApplication();
        } catch (IOException exception) {
            System.err.println("Could not start the application: " + exception.getMessage());
        }
    }

    private static void runApplication() throws IOException {
        UserRepository repository = new UserRepository(Paths.get("data"));
        AuthController authController = new AuthController(repository);
        AppController appController = new AppController(authController);
        MenuManager manager = new MenuManager();
        MenuSet menus = createMenus(manager, authController);

        configureParents(menus);
        registerMenus(manager, menus);
        boolean restoredSession = appController.startApplication();
        manager.setCurrentMenu(restoredSession ? menus.mainMenu : menus.registerMenu);
        runCommandLoop(manager, appController);
    }

    private static MenuSet createMenus(MenuManager manager, AuthController authController) {
        ProfileController profileController = new ProfileController(authController);
        SettingsController settingsController = new SettingsController(authController);
        RegisterMenu registerMenu = new RegisterMenu(manager, authController, new RegisterView());
        LoginMenu loginMenu = new LoginMenu(manager, authController, new LoginView());
        MainMenu mainMenu = new MainMenu(manager, authController);
        GameMenu gameMenu = new GameMenu(manager);
        CollectionMenu collectionMenu = new CollectionMenu(manager);
        GreenhouseMenu greenhouseMenu = new GreenhouseMenu(manager);
        ShopMenu shopMenu = new ShopMenu(manager);
        SettingsMenu settingsMenu = new SettingsMenu(manager, settingsController, new SettingsView());
        NewsMenu newsMenu = new NewsMenu(manager);
        ProfileMenu profileMenu = new ProfileMenu(manager, profileController, new ProfileView());
        QuestMenu questMenu = new QuestMenu(manager);
        LeaderboardMenu leaderboardMenu = new LeaderboardMenu(manager);
        MiniGameMenu miniGameMenu = new MiniGameMenu(manager);
        return new MenuSet(registerMenu, loginMenu, mainMenu, gameMenu, collectionMenu,
                greenhouseMenu, shopMenu, settingsMenu, newsMenu, profileMenu, questMenu,
                leaderboardMenu, miniGameMenu);
    }

    private static void configureParents(MenuSet menus) {
        menus.loginMenu.setParentMenu(menus.registerMenu);
        menus.gameMenu.setParentMenu(menus.mainMenu);
        menus.settingsMenu.setParentMenu(menus.mainMenu);
        menus.newsMenu.setParentMenu(menus.mainMenu);
        menus.profileMenu.setParentMenu(menus.mainMenu);
        menus.leaderboardMenu.setParentMenu(menus.mainMenu);
        menus.collectionMenu.setParentMenu(menus.gameMenu);
        menus.greenhouseMenu.setParentMenu(menus.gameMenu);
        menus.questMenu.setParentMenu(menus.gameMenu);
        menus.shopMenu.setParentMenu(menus.greenhouseMenu);
        menus.miniGameMenu.setParentMenu(menus.questMenu);
    }

    private static void registerMenus(MenuManager manager, MenuSet menus) {
        List<Menu> allMenus = Arrays.asList(
                menus.registerMenu, menus.loginMenu, menus.mainMenu, menus.gameMenu,
                menus.collectionMenu, menus.greenhouseMenu, menus.shopMenu, menus.settingsMenu,
                menus.newsMenu, menus.profileMenu, menus.questMenu, menus.leaderboardMenu,
                menus.miniGameMenu);
        for (Menu menu : allMenus) {
            manager.registerMenu(menu);
        }
    }

    private static void runCommandLoop(MenuManager manager, AppController appController) {
        Runtime.getRuntime().addShutdownHook(new Thread(appController::saveAndExit));
        try (Scanner scanner = new Scanner(System.in)) {
            while (manager.isRunning() && scanner.hasNextLine()) {
                manager.getCurrentMenu().handleCommand(scanner.nextLine());
            }
        } finally {
            appController.saveAndExit();
        }
    }

    private static final class MenuSet {
        private final RegisterMenu registerMenu;
        private final LoginMenu loginMenu;
        private final MainMenu mainMenu;
        private final GameMenu gameMenu;
        private final CollectionMenu collectionMenu;
        private final GreenhouseMenu greenhouseMenu;
        private final ShopMenu shopMenu;
        private final SettingsMenu settingsMenu;
        private final NewsMenu newsMenu;
        private final ProfileMenu profileMenu;
        private final QuestMenu questMenu;
        private final LeaderboardMenu leaderboardMenu;
        private final MiniGameMenu miniGameMenu;

        private MenuSet(RegisterMenu registerMenu, LoginMenu loginMenu, MainMenu mainMenu,
                        GameMenu gameMenu, CollectionMenu collectionMenu,
                        GreenhouseMenu greenhouseMenu, ShopMenu shopMenu,
                        SettingsMenu settingsMenu, NewsMenu newsMenu, ProfileMenu profileMenu,
                        QuestMenu questMenu, LeaderboardMenu leaderboardMenu,
                        MiniGameMenu miniGameMenu) {
            this.registerMenu = registerMenu;
            this.loginMenu = loginMenu;
            this.mainMenu = mainMenu;
            this.gameMenu = gameMenu;
            this.collectionMenu = collectionMenu;
            this.greenhouseMenu = greenhouseMenu;
            this.shopMenu = shopMenu;
            this.settingsMenu = settingsMenu;
            this.newsMenu = newsMenu;
            this.profileMenu = profileMenu;
            this.questMenu = questMenu;
            this.leaderboardMenu = leaderboardMenu;
            this.miniGameMenu = miniGameMenu;
        }
    }
}
