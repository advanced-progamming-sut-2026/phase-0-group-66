package pvz.app;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public final class DisplaySettings {
    private static final String PREFERENCES_NAME = "pvz2-display-settings";
    private static final String FULLSCREEN_KEY = "fullscreen";
    private static final String VSYNC_KEY = "vsync";
    private static final String WIDTH_KEY = "windowWidth";
    private static final String HEIGHT_KEY = "windowHeight";

    private static final int DEFAULT_WIDTH = 1280;
    private static final int DEFAULT_HEIGHT = 720;

    private final Preferences preferences;
    private boolean fullscreen;
    private boolean vsync;
    private int windowWidth;
    private int windowHeight;

    public DisplaySettings() {
        preferences = Gdx.app.getPreferences(PREFERENCES_NAME);
        fullscreen = preferences.getBoolean(FULLSCREEN_KEY, false);
        vsync = preferences.getBoolean(VSYNC_KEY, true);
        windowWidth = preferences.getInteger(WIDTH_KEY, DEFAULT_WIDTH);
        windowHeight = preferences.getInteger(HEIGHT_KEY, DEFAULT_HEIGHT);
    }

    public void apply() {
        Gdx.graphics.setVSync(vsync);
        applyWindowMode();
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public boolean isVsync() {
        return vsync;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public void setFullscreen(boolean enabled) {
        fullscreen = enabled;
        save();
        applyWindowMode();
    }

    public void setVsync(boolean enabled) {
        vsync = enabled;
        save();
        Gdx.graphics.setVSync(enabled);
    }

    public void setWindowSize(int width, int height) {
        windowWidth = width;
        windowHeight = height;
        fullscreen = false;
        save();
        Gdx.graphics.setWindowedMode(width, height);
    }

    public void resetDefaults() {
        fullscreen = false;
        vsync = true;
        windowWidth = DEFAULT_WIDTH;
        windowHeight = DEFAULT_HEIGHT;
        save();
        Gdx.graphics.setVSync(vsync);
        Gdx.graphics.setWindowedMode(windowWidth, windowHeight);
    }

    public String currentModeText() {
        if (fullscreen) {
            return "Fullscreen  |  " + Gdx.graphics.getWidth() + " x " + Gdx.graphics.getHeight();
        }
        return "Windowed  |  " + Gdx.graphics.getWidth() + " x " + Gdx.graphics.getHeight();
    }

    private void applyWindowMode() {
        if (fullscreen) {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            return;
        }
        Gdx.graphics.setWindowedMode(windowWidth, windowHeight);
    }

    private void save() {
        preferences.putBoolean(FULLSCREEN_KEY, fullscreen);
        preferences.putBoolean(VSYNC_KEY, vsync);
        preferences.putInteger(WIDTH_KEY, windowWidth);
        preferences.putInteger(HEIGHT_KEY, windowHeight);
        preferences.flush();
    }
}
