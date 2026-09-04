package network.server;

import model.UserRepository;
import network.protocol.Phase3Protocol;

import java.io.IOException;
import java.net.BindException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/** Standalone entry point for the Phase 3 server. */
public final class PvzServerMain {
    private PvzServerMain() {
    }

    public static void main(String[] args) {
        int port = resolvePort(args);
        Path dataDirectory = resolveDataDirectory(args);
        try {
            migrateLegacyUsers(dataDirectory);
            UserRepository repository = new UserRepository(dataDirectory);
            PvzServer server = new PvzServer(port, repository);
            Runtime.getRuntime().addShutdownHook(new Thread(server::close, "pvz-server-shutdown"));
            System.out.println("PvZ Phase 3 server listening on port " + port);
            System.out.println("Server user data: " + dataDirectory.toAbsolutePath().normalize());
            server.start();
        } catch (BindException exception) {
            System.err.println("Could not start PvZ server on port " + port
                + ": the port is already in use. Stop the existing server or pass another port.");
            System.exit(1);
        } catch (IOException | IllegalArgumentException exception) {
            System.err.println("Could not start PvZ server: " + exception.getMessage());
            System.exit(1);
        }
    }

    private static void migrateLegacyUsers(Path dataDirectory) throws IOException {
        Path target = dataDirectory.resolve("users.dat");
        Path legacy = Paths.get("data", "users.dat");
        if (Files.exists(target) || !Files.isRegularFile(legacy)) {
            return;
        }
        Files.createDirectories(dataDirectory);
        Files.copy(legacy, target, StandardCopyOption.COPY_ATTRIBUTES);
        System.out.println("Migrated previous-phase users into server storage.");
    }

    private static int resolvePort(String[] args) {
        String value = args.length >= 1 ? args[0] : System.getProperty("pvz.server.port");
        if (value == null || value.isBlank()) {
            return Phase3Protocol.DEFAULT_PORT;
        }
        return Integer.parseInt(value.trim());
    }

    private static Path resolveDataDirectory(String[] args) {
        String value = args.length >= 2 ? args[1] : System.getProperty("pvz.server.data");
        if (value == null || value.isBlank()) {
            return Paths.get("server-data");
        }
        return Paths.get(value.trim());
    }
}
