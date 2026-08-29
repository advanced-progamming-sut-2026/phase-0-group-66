package pvz.app;

import controller.AuthController;
import controller.CollectionController;
import controller.GameController;
import controller.GreenhouseController;
import controller.LeaderboardController;
import controller.MiniGameController;
import controller.NetworkMatchController;
import controller.NewsController;
import controller.QuestController;
import controller.ProfileController;
import controller.SettingsController;
import controller.ShopController;
import model.AdventureFactory;
import model.GameData;
import model.UserRepository;
import network.client.PvzNetworkClient;
import network.client.RemoteUserRepository;
import network.protocol.Phase3Protocol;
import view.GameView;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public final class PvzServices {
    private static final String USER_DATA_PROPERTY = "pvz.user.data";
    private static final String NETWORK_ENABLED_PROPERTY = "pvz.network.enabled";
    private static final String SERVER_HOST_PROPERTY = "pvz.server.host";
    private static final String SERVER_PORT_PROPERTY = "pvz.server.port";

    private final AuthController authController;
    private final ProfileController profileController;
    private final CollectionController collectionController;
    private final SettingsController settingsController;
    private final NewsController newsController;
    private final QuestController questController;
    private final GameController gameController;
    private final GreenhouseController greenhouseController;
    private final ShopController shopController;
    private final LeaderboardController leaderboardController;
    private final MiniGameController miniGameController;
    private final NetworkMatchController networkMatchController;
    private final AdventureFactory adventureFactory;
    private final GameData gameData;
    public PvzServices() throws IOException {
        Path userDataDirectory = resolveUserDataDirectory();
        boolean networkEnabled = resolveNetworkEnabled();
        if (!networkEnabled) {
            migrateLegacyUserData(userDataDirectory);
        }
        PvzNetworkClient networkClient = networkEnabled ? createNetworkClient() : null;
        UserRepository repository = networkEnabled
            ? new RemoteUserRepository(userDataDirectory, networkClient)
            : new UserRepository(userDataDirectory);
        authController = new AuthController(repository);
        networkMatchController = new NetworkMatchController(authController, networkClient);
        leaderboardController = new LeaderboardController(repository);
        gameData = GameData.loadDefault();
        profileController = new ProfileController(authController);
        collectionController = new CollectionController(
            authController,
            gameData.getPlantFactory(),
            gameData.getZombieFactory(),
            gameData.getArmorFactory()
        );
        settingsController = new SettingsController(authController);
        newsController = new NewsController(authController);
        questController = new QuestController(
            authController,
            gameData.getQuestFactory(),
            gameData.getPlantFactory()
        );
        miniGameController = new MiniGameController(authController, questController, networkClient);
        gameController = new GameController(
            authController,
            gameData,
            new GameView(),
            questController
        );
        greenhouseController = new GreenhouseController(
            authController,
            gameData.getPlantFactory()
        );
        shopController = new ShopController(
            authController,
            gameData.getPlantFactory()
        );
        adventureFactory = new AdventureFactory();
    }

    public AuthController auth() {
        return authController;
    }

    public ProfileController profile() {
        return profileController;
    }

    public CollectionController collection() {
        return collectionController;
    }

    public SettingsController settings() {
        return settingsController;
    }

    public NewsController news() {
        return newsController;
    }

    public QuestController quests() {
        return questController;
    }

    public GameController game() {
        return gameController;
    }

    public LeaderboardController leaderboard() {
        return leaderboardController;
    }

    public MiniGameController miniGames() {
        return miniGameController;
    }

    public NetworkMatchController network() {
        return networkMatchController;
    }

    public GreenhouseController greenhouse() {
        return greenhouseController;
    }

    public ShopController shop() {
        return shopController;
    }

    public AdventureFactory adventure() {
        return adventureFactory;
    }

    public GameData gameData() {
        return gameData;
    }


    private boolean resolveNetworkEnabled() {
        return Boolean.parseBoolean(System.getProperty(NETWORK_ENABLED_PROPERTY, "true"));
    }

    private PvzNetworkClient createNetworkClient() {
        String host = System.getProperty(SERVER_HOST_PROPERTY, Phase3Protocol.DEFAULT_HOST).trim();
        String portText = System.getProperty(SERVER_PORT_PROPERTY,
            Integer.toString(Phase3Protocol.DEFAULT_PORT)).trim();
        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException exception) {
            port = Phase3Protocol.DEFAULT_PORT;
        }
        return new PvzNetworkClient(host, port);
    }

    private Path resolveUserDataDirectory() {
        String configured = System.getProperty(USER_DATA_PROPERTY);
        if (configured != null && !configured.isBlank()) {
            return Paths.get(configured.trim()).toAbsolutePath().normalize();
        }
        return Paths.get("data").toAbsolutePath().normalize();
    }

    private void migrateLegacyUserData(Path targetDirectory) throws IOException {
        Path legacyDirectory = Paths.get("src", "data").toAbsolutePath().normalize();
        copyIfMissing(legacyDirectory.resolve("users.dat"), targetDirectory.resolve("users.dat"));
        copyIfMissing(legacyDirectory.resolve("session.txt"), targetDirectory.resolve("session.txt"));
    }

    private void copyIfMissing(Path source, Path target) throws IOException {
        if (!Files.isRegularFile(source) || Files.exists(target)) {
            return;
        }
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
    }
}
