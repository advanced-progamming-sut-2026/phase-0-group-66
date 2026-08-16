package pvz;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.utils.GdxRuntimeException;
import pvz.app.PvzServices;
import pvz.assets.PvzAssets;
import pvz.screen.ForgotPasswordScreen;
import pvz.screen.LoginScreen;
import pvz.screen.MainMenuScreen;
import pvz.screen.PlaceholderScreen;
import pvz.screen.RegisterScreen;
import pvz.screen.SecurityQuestionScreen;

import java.io.IOException;

public final class PvzApplication extends Game {
    private PvzAssets assets;
    private PvzServices services;

    @Override
    public void create() {
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
