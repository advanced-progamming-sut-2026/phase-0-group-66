package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import controller.ActionResult;
import pvz.PvzApplication;
import pvz.skin.BorderedTable;

public final class RegisterScreen extends BaseUiScreen {
    private static final float PANEL_WIDTH = 850f;
    private static final float PANEL_HEIGHT = 625f;
    private static final float COLUMN_WIDTH = 350f;

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
        gender = theme.genderSelect();
        status = statusLabel();
        buildUi();
    }

    private void buildUi() {
        BorderedTable panel = theme.dialogPanel();
        panel.defaults().center();

        Image logo = theme.pvzLogo();
        if (logo != null) {
            panel.add(logo).width(300f).height(51f).colspan(2);
            panel.row().padTop(5f);
        }

        panel.add(theme.title("CREATE ACCOUNT")).colspan(2);
        panel.row().padTop(15f);

        Table fields = new Table();
        fields.defaults().top().pad(0f, 9f, 11f, 9f);
        fields.add(fieldBlock("Username", username)).width(COLUMN_WIDTH);
        fields.add(fieldBlock("Nickname", nickname)).width(COLUMN_WIDTH);
        fields.row();
        fields.add(fieldBlock("Password", password)).width(COLUMN_WIDTH);
        fields.add(fieldBlock("Confirm Password", passwordConfirm)).width(COLUMN_WIDTH);
        fields.row();
        fields.add(fieldBlock("Email", email)).width(COLUMN_WIDTH);
        fields.add(genderBlock()).width(COLUMN_WIDTH);

        panel.add(fields).colspan(2);
        panel.row().padTop(8f);

        TextButton register = theme.primaryButton("Continue");
        TextButton login = theme.secondaryButton("I already have an account");
        UiActions.onClick(register, this::submitRegistration);
        UiActions.onClick(login, app::showLogin);

        panel.add(register).width(270f).height(62f).colspan(2);
        panel.row().padTop(9f);
        panel.add(login).width(365f).height(56f).colspan(2);
        panel.row().padTop(8f);
        panel.add(status).width(650f).height(42f).colspan(2);

        root.add(panel).width(PANEL_WIDTH).height(PANEL_HEIGHT);
        stage.setKeyboardFocus(username);
    }

    private Table fieldBlock(String title, TextField field) {
        Table block = new Table();
        block.add(theme.fieldLabel(title)).left().expandX();
        block.row().padTop(3f);
        block.add(field).width(COLUMN_WIDTH).height(FIELD_HEIGHT);
        return block;
    }

    private Table genderBlock() {
        Table block = new Table();
        block.add(theme.fieldLabel("Gender")).left().expandX();
        block.row().padTop(3f);
        block.add(gender).width(COLUMN_WIDTH).height(FIELD_HEIGHT);
        return block;
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
        if (!result.isSuccessful()) {
            theme.showError(status, result.getMessage());
            return;
        }
        theme.showSuccess(status, result.getMessage());
        app.showSecurityQuestion();
    }
}
