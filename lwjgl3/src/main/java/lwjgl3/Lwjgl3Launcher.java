package lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import model.RuntimeSupport;
import pvz.PvzApplication;

import javax.swing.JOptionPane;

public class Lwjgl3Launcher {
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            RuntimeSupport.log("Crash", "Unhandled exception on " + thread.getName() + ".", throwable);
            showError("The game stopped unexpectedly.\nSee the log file in the PVZ data folder.");
        });
        try {
            if (StartupHelper.startNewJvmIfRequired()) {
                return;
            }
            createApplication();
        } catch (Throwable throwable) {
            RuntimeSupport.log("Startup", "Could not start the game.", throwable);
            showError("The game could not start.\nSee the log file in the PVZ data folder.");
        }
    }

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new PvzApplication(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("phase-0-group-66");
        configuration.useVsync(true);
        configuration.setForegroundFPS(
            Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1
        );
        configuration.setWindowedMode(1280, 720);
        return configuration;
    }

    private static void showError(String message) {
        try {
            JOptionPane.showMessageDialog(null, message, "Plants vs. Zombies 2", JOptionPane.ERROR_MESSAGE);
        } catch (Throwable ignored) {
            // A dialog is best effort; the persistent log remains available.
        }
    }
}
