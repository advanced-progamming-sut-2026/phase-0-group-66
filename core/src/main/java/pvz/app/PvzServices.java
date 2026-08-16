package pvz.app;

import controller.AuthController;
import controller.NewsController;
import controller.ProfileController;
import controller.SettingsController;
import model.GameData;
import model.UserRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public final class PvzServices {
    private static final String USER_DATA_PROPERTY = "pvz.user.data";

    private final AuthController authController;
    private final ProfileController profileController;
    private final SettingsController settingsController;
    private final NewsController newsController;
    private final GameData gameData;

    public PvzServices() throws IOException {
        Path userDataDirectory = resolveUserDataDirectory();
        migrateLegacyUserData(userDataDirectory);
        UserRepository repository = new UserRepository(userDataDirectory);
        authController = new AuthController(repository);
        profileController = new ProfileController(authController);
        settingsController = new SettingsController(authController);
        newsController = new NewsController(authController);
        gameData = GameData.loadDefault();
    }

    public AuthController auth() {
        return authController;
    }

    public ProfileController profile() {
        return profileController;
    }

    public SettingsController settings() {
        return settingsController;
    }

    public NewsController news() {
        return newsController;
    }

    public GameData gameData() {
        return gameData;
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
