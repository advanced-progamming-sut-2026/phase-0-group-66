package pvz;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.utils.GdxRuntimeException;
import pvz.app.AudioSettings;
import pvz.app.DisplaySettings;
import pvz.app.PvzServices;
import pvz.assets.PvzAssets;
import pvz.screen.AdventureScreen;
import pvz.screen.BattleScreen;
import pvz.screen.ChapterLevelsScreen;
import pvz.screen.ForgotPasswordScreen;
import pvz.screen.LevelBriefingScreen;
import pvz.screen.LoginScreen;
import pvz.screen.MainMenuScreen;
import pvz.screen.NewsScreen;
import pvz.screen.PlaceholderScreen;
import pvz.screen.PlantSelectionScreen;
import pvz.screen.ProfileScreen;
import pvz.screen.RegisterScreen;
import pvz.screen.SecurityQuestionScreen;
import pvz.screen.SettingsScreen;
import model.Chapter;
import model.Level;

import java.io.IOException;

public final class PvzApplication extends Game {
    private PvzAssets assets;
    private PvzServices services;
    private DisplaySettings displaySettings;
    private AudioSettings audioSettings;

    @Override
    public void create() {
        displaySettings = new DisplaySettings();
        displaySettings.apply();
        audioSettings = new AudioSettings();

        try {
            assets = new PvzAssets();
            services = new PvzServices();
        } catch (IOException exception) {
            throw new GdxRuntimeException("Could not initialize PVZ data.", exception);
        }

        if (services.auth().restoreSession()) {
            showMainMenu();
        } else {
            showRegister();
        }
    }

    public PvzAssets assets() {
        return assets;
    }

    public PvzServices services() {
        return services;
    }

    public DisplaySettings displaySettings() {
        return displaySettings;
    }

    public AudioSettings audioSettings() {
        return audioSettings;
    }

    public void showRegister() {
        changeScreen(new RegisterScreen(this));
    }

    public void showSecurityQuestion() {
        changeScreen(new SecurityQuestionScreen(this));
    }

    public void showLogin() {
        changeScreen(new LoginScreen(this));
    }

    public void showForgotPassword() {
        changeScreen(new ForgotPasswordScreen(this));
    }

    public void showMainMenu() {
        if (!services.auth().isAuthenticated()) {
            showLogin();
            return;
        }
        changeScreen(new MainMenuScreen(this));
    }


    public void showAdventure() {
        if (!services.auth().isAuthenticated()) {
            showLogin();
            return;
        }
        changeScreen(new AdventureScreen(this));
    }

    public void showChapterLevels(Chapter chapter) {
        if (!services.auth().isAuthenticated()) {
            showLogin();
            return;
        }
        changeScreen(new ChapterLevelsScreen(this, chapter));
    }

    public void showLevelBriefing(Chapter chapter, Level level) {
        if (!services.auth().isAuthenticated()) {
            showLogin();
            return;
        }
        changeScreen(new LevelBriefingScreen(this, chapter, level));
    }

    public void showPlantSelection(Chapter chapter, Level level) {
        if (!services.auth().isAuthenticated()) {
            showLogin();
            return;
        }
        changeScreen(new PlantSelectionScreen(this, chapter, level));
    }

    public void showBattle(Chapter chapter, Level level) {
        if (!services.auth().isAuthenticated()) {
            showLogin();
            return;
        }
        changeScreen(new BattleScreen(this, chapter, level));
    }

    public void showProfile() {
        if (!services.auth().isAuthenticated()) {
            showLogin();
            return;
        }
        changeScreen(new ProfileScreen(this));
    }

    public void showSettings() {
        if (!services.auth().isAuthenticated()) {
            showLogin();
            return;
        }
        changeScreen(new SettingsScreen(this));
    }

    public void showNews() {
        if (!services.auth().isAuthenticated()) {
            showLogin();
            return;
        }
        changeScreen(new NewsScreen(this));
    }

    public void showPlaceholder(String title) {
        changeScreen(new PlaceholderScreen(this, title));
    }

    private void changeScreen(Screen next) {
        Screen previous = getScreen();
        setScreen(next);
        if (previous != null) {
            Gdx.app.postRunnable(previous::dispose);
        }
    }

    @Override
    public void dispose() {
        if (services != null) {
            services.auth().saveCurrentState();
        }
        if (getScreen() != null) {
            getScreen().dispose();
        }
        if (assets != null) {
            assets.dispose();
        }
    }
}
