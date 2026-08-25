package lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import pvz.PvzApplication;

public class Lwjgl3Launcher {
    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) {
            return;
        }
        createApplication();
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
}
