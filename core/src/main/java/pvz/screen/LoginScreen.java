package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import controller.ActionResult;
import model.RuntimeSupport;
import pvz.PvzApplication;
import pvz.skin.BorderedTable;

import com.badlogic.gdx.Gdx;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LoginScreen extends BaseUiScreen {
    private static final float PANEL_WIDTH = 620f;
    private static final float PANEL_HEIGHT = 590f;
    private static final float LOGIN_FIELD_WIDTH = 430f;

    private final TextField username;
    private final TextField password;
    private final CheckBox stayLoggedIn;
    private final Label status;
    private final ExecutorService loginExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "pvz-login");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean loginInFlight = new AtomicBoolean();
    private TextButton loginButton;
    private volatile boolean disposed;

    public LoginScreen(PvzApplication app) {
        super(app);
        username = textField("Username");
        password = passwordField("Password");
        stayLoggedIn = theme.stayLoggedInCheckBox();
        status = statusLabel();
        buildUi();
    }

    private void buildUi() {
        BorderedTable panel = theme.dialogPanel();
        panel.padTop(30f).padBottom(16f);
        addHeader(panel);
        addLoginField(panel, "Username", username);
        addLoginField(panel, "Password", password);
        panel.add(stayLoggedIn).left().width(LOGIN_FIELD_WIDTH);
        panel.row().padTop(18f);
        addActions(panel);
        panel.row().padTop(12f);
        panel.add(status).width(500f).height(50f);
        root.add(panel).width(PANEL_WIDTH).height(PANEL_HEIGHT);
        stage.setKeyboardFocus(username);
    }

    private void addHeader(Table panel) {
        Image logo = theme.pvzLogo();
        if (logo != null) {
            panel.add(logo).width(300f).height(51f);
            panel.row().padTop(4f);
        }
        panel.add(theme.title("WELCOME BACK"));
        panel.row().padTop(18f);
    }

    private void addLoginField(Table panel, String title, TextField field) {
        panel.add(theme.fieldLabel(title)).left().width(LOGIN_FIELD_WIDTH);
        panel.row().padTop(4f);
        if (field == password) {
            panel.add(theme.passwordFieldWithToggle(field)).width(LOGIN_FIELD_WIDTH).height(FIELD_HEIGHT);
        } else {
            panel.add(field).width(LOGIN_FIELD_WIDTH).height(FIELD_HEIGHT);
        }
        panel.row().padTop(13f);
    }

    private void addActions(Table panel) {
        loginButton = theme.primaryButton("LOGIN");
        TextButton forgot = theme.tertiaryButton("Forgot password");
        TextButton register = theme.secondaryButton("Create account");
        UiActions.onClick(loginButton, this::login);
        UiActions.onClick(forgot, app::showForgotPassword);
        UiActions.onClick(register, app::showRegister);
        panel.add(loginButton).width(270f).height(62f);
        panel.row().padTop(10f);
        Table secondary = new Table();
        secondary.add(forgot).width(215f).height(54f).padRight(10f);
        secondary.add(register).width(215f).height(54f);
        panel.add(secondary);
    }

    private void login() {
        if (!loginInFlight.compareAndSet(false, true)) {
            return;
        }

        String usernameValue = username.getText().trim();
        String passwordValue = password.getText();
        boolean keepLoggedIn = stayLoggedIn.isChecked();
        loginButton.setDisabled(true);
        status.setText("Logging in...");

        loginExecutor.submit(() -> {
            ActionResult result;
            try {
                result = app.services().auth().login(usernameValue, passwordValue, keepLoggedIn);
            } catch (RuntimeException exception) {
                RuntimeSupport.log("Login", "Login worker failed.", exception);
                result = ActionResult.failure("Login failed unexpectedly. See the log file for details.");
            }
            ActionResult completed = result;
            Gdx.app.postRunnable(() -> finishLogin(completed));
        });
    }

    private void finishLogin(ActionResult result) {
        loginInFlight.set(false);
        if (disposed) {
            return;
        }
        loginButton.setDisabled(false);
        if (!result.isSuccessful()) {
            theme.showError(status, result.getMessage());
            return;
        }
        try {
            app.showMainMenu();
        } catch (RuntimeException exception) {
            RuntimeSupport.log("Login", "Could not open the main menu after login.", exception);
            theme.showError(status, "Login succeeded, but the main menu could not be opened.");
        }
    }

    @Override
    public void dispose() {
        disposed = true;
        loginExecutor.shutdownNow();
        super.dispose();
    }
}
