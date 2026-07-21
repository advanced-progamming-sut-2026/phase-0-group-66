import controller.AppController;
import controller.AuthController;
import controller.CollectionController;
import controller.GameController;
import controller.GreenhouseController;
import controller.LeaderboardController;
import controller.MiniGameController;
import controller.NewsController;
import controller.ProfileController;
import controller.QuestController;
import controller.SettingsController;
import controller.ShopController;
import menu.BattleMenu;
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
import menu.PlantSelectionMenu;
import menu.ProfileMenu;
import menu.QuestMenu;
import menu.RegisterMenu;
import menu.SettingsMenu;
import menu.ShopMenu;
import model.GameData;
import model.UserRepository;
import view.CollectionView;
import view.GameView;
import view.GreenhouseView;
import view.LeaderboardView;
import view.LoginView;
import view.MiniGameView;
import view.NewsView;
import view.ProfileView;
import view.QuestView;
import view.RegisterView;
import view.SettingsView;
import view.ShopView;

import java.io.IOException;
import java.nio.file.Paths;
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
        GameData gameData = GameData.loadDefault();
        UserRepository repository = new UserRepository(Paths.get("data"));
        AuthController authController = new AuthController(repository);
        AppController appController = new AppController(authController);
        MenuManager manager = new MenuManager();
        MenuSet menus = createMenus(manager, authController, gameData);

        configureParents(menus);
        registerMenus(manager, menus);
        boolean restoredSession = appController.startApplication();
        manager.setCurrentMenu(restoredSession ? menus.mainMenu : menus.registerMenu);
        runCommandLoop(manager, appController);
    }

    private static MenuSet createMenus(MenuManager manager, AuthController authController,
                                       GameData gameData) {
        ProfileController profileController = new ProfileController(authController);
        SettingsController settingsController = new SettingsController(authController);
        QuestController questController = new QuestController(authController,
                gameData.getQuestFactory(), gameData.getPlantFactory());
        GameController gameController = new GameController(authController, gameData,
                new GameView(), questController);
        CollectionController collectionController = new CollectionController(authController,
                gameData.getPlantFactory(), gameData.getZombieFactory(), gameData.getArmorFactory());
        GreenhouseController greenhouseController = new GreenhouseController(authController,
                gameData.getPlantFactory());
        ShopController shopController = new ShopController(authController,
                gameData.getPlantFactory());
        NewsController newsController = new NewsController(authController);
        LeaderboardController leaderboardController = new LeaderboardController(
                authController.getUserRepository());
        MiniGameController miniGameController = new MiniGameController(authController,
                questController);

        return new MenuSet(
                new RegisterMenu(manager, authController, new RegisterView()),
                new LoginMenu(manager, authController, new LoginView()),
                new MainMenu(manager, authController, newsController),
                new GameMenu(manager, gameController),
                new PlantSelectionMenu(manager, gameController),
                new BattleMenu(manager, gameController),
                new CollectionMenu(manager, collectionController, new CollectionView()),
                new GreenhouseMenu(manager, greenhouseController, new GreenhouseView()),
                new ShopMenu(manager, shopController, new ShopView()),
                new SettingsMenu(manager, settingsController, new SettingsView()),
                new NewsMenu(manager, newsController, new NewsView()),
                new ProfileMenu(manager, profileController, new ProfileView()),
                new QuestMenu(manager, questController, new QuestView()),
                new LeaderboardMenu(manager, leaderboardController, new LeaderboardView()),
                new MiniGameMenu(manager, miniGameController, new MiniGameView())
        );
    }

    private static void configureParents(MenuSet menus) {
        menus.loginMenu.setParentMenu(menus.registerMenu);
        menus.gameMenu.setParentMenu(menus.mainMenu);
        menus.settingsMenu.setParentMenu(menus.mainMenu);
        menus.newsMenu.setParentMenu(menus.mainMenu);
        menus.profileMenu.setParentMenu(menus.mainMenu);
        menus.leaderboardMenu.setParentMenu(menus.mainMenu);
        menus.plantSelectionMenu.setParentMenu(menus.gameMenu);
        menus.battleMenu.setParentMenu(menus.plantSelectionMenu);
        menus.collectionMenu.setParentMenu(menus.gameMenu);
        menus.greenhouseMenu.setParentMenu(menus.gameMenu);
        menus.questMenu.setParentMenu(menus.gameMenu);
        menus.shopMenu.setParentMenu(menus.greenhouseMenu);
        menus.miniGameMenu.setParentMenu(menus.questMenu);
    }

    private static void registerMenus(MenuManager manager, MenuSet menus) {
        for (Menu menu : menus.allMenus()) {
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

    private record MenuSet(RegisterMenu registerMenu, LoginMenu loginMenu, MainMenu mainMenu,
                           GameMenu gameMenu, PlantSelectionMenu plantSelectionMenu,
                           BattleMenu battleMenu, CollectionMenu collectionMenu,
                           GreenhouseMenu greenhouseMenu, ShopMenu shopMenu,
                           SettingsMenu settingsMenu, NewsMenu newsMenu,
                           ProfileMenu profileMenu, QuestMenu questMenu,
                           LeaderboardMenu leaderboardMenu, MiniGameMenu miniGameMenu) {
        private List<Menu> allMenus() {
            return List.of(registerMenu, loginMenu, mainMenu, gameMenu, plantSelectionMenu,
                    battleMenu, collectionMenu, greenhouseMenu, shopMenu, settingsMenu,
                    newsMenu, profileMenu, questMenu, leaderboardMenu, miniGameMenu);
        }
    }
}
