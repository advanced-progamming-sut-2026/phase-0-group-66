package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import controller.ActionResult;
import pvz.PvzApplication;

public final class RegisterScreen extends BaseUiScreen {
    private final TextField username;
    private final TextField password;
    private final TextField passwordConfirm;
    private final TextField nickname;
    private final TextField email;
    private final SelectBox<String> gender;
    private final Label status;

    public RegisterScreen(PvzApplication app) {
        super(app);
        username = textField("Username");
        password = passwordField("Password");
        passwordConfirm = passwordField("Confirm password");
        nickname = textField("Nickname");
        email = textField("Email");
        gender = new SelectBox<>(app.assets().skin());
        gender.setItems("MALE", "FEMALE");
        status = statusLabel();
        buildUi();
    }

    private void buildUi() {
        Table form = new Table();
        form.defaults().center();
        form.add(new Label("CREATE ACCOUNT", app.assets().skin(), "big_outline"));
        form.row().padTop(18f);
        addField(form, "Username", username);
        addField(form, "Password", password);
        addField(form, "Confirm password", passwordConfirm);
        addField(form, "Nickname", nickname);
        addField(form, "Email", email);
        form.add(new Label("Gender", app.assets().skin(), "secondary")).left();
        form.row().padTop(4f);
        form.add(gender).width(FIELD_WIDTH).height(48f);
        form.row().padTop(16f);

        TextButton register = new TextButton("Continue", app.assets().skin(), "green");
        TextButton login = new TextButton("I already have an account", app.assets().skin(), "brown");
        UiActions.onClick(register, this::submitRegistration);
        UiActions.onClick(login, app::showLogin);

        form.add(register).width(250f).height(62f);
        form.row().padTop(10f);
        form.add(login).width(330f).height(58f);
        form.row().padTop(12f);
        form.add(status).width(520f);
        root.add(form);
        stage.setKeyboardFocus(username);
    }

    private void submitRegistration() {
        ActionResult result = app.services().auth().register(
            username.getText().trim(),
            password.getText(),
            passwordConfirm.getText(),
            nickname.getText().trim(),
            email.getText().trim(),
            gender.getSelected()
        );
        status.setText(result.getMessage());
        if (result.isSuccessful()) {
            app.showSecurityQuestion();
        }
    }
}
