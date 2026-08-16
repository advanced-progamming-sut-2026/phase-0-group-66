package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import controller.ActionResult;
import pvz.PvzApplication;

public final class LoginScreen extends BaseUiScreen {
    private final TextField username;
    private final TextField password;
    private final CheckBox stayLoggedIn;
    private final Label status;

    public LoginScreen(PvzApplication app) {
        super(app);
        username = textField("Username");
        password = passwordField("Password");
        stayLoggedIn = new CheckBox(" Stay logged in", app.assets().skin());
        status = statusLabel();
        buildUi();
    }

    private void buildUi() {
        Table form = new Table();
        form.add(new Label("WELCOME BACK", app.assets().skin(), "big_outline"));
        form.row().padTop(24f);
        addField(form, "Username", username);
        addField(form, "Password", password);
        form.add(stayLoggedIn).left();
        form.row().padTop(18f);

        TextButton login = new TextButton("Login", app.assets().skin(), "green");
        TextButton forgot = new TextButton("Forgot password", app.assets().skin(), "purple");
        TextButton register = new TextButton("Create account", app.assets().skin(), "brown");
        UiActions.onClick(login, this::login);
        UiActions.onClick(forgot, app::showForgotPassword);
        UiActions.onClick(register, app::showRegister);

        form.add(login).width(240f).height(62f);
        form.row().padTop(10f);
        Table secondary = new Table();
        secondary.add(forgot).width(215f).height(56f).padRight(10f);
        secondary.add(register).width(205f).height(56f);
        form.add(secondary);
        form.row().padTop(14f);
        form.add(status).width(520f);
        root.add(form);
        stage.setKeyboardFocus(username);
    }

    private void login() {
        ActionResult result = app.services().auth().login(
            username.getText().trim(),
            password.getText(),
            stayLoggedIn.isChecked()
        );
        status.setText(result.getMessage());
        if (result.isSuccessful()) {
            app.showMainMenu();
        }
    }
}
