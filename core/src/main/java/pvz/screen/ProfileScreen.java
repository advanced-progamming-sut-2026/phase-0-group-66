package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import controller.ActionResult;
import controller.ProfileController;
import model.GameProgress;
import pvz.PvzApplication;

public final class ProfileScreen extends AuthenticatedUiScreen {
    private static final float EDIT_FIELD_WIDTH = 300f;
    private static final float EDIT_FIELD_HEIGHT = 48f;
    private static final float PASSWORD_FIELD_WIDTH = 215f;

    private final ProfileController controller;
    private final Label usernameValue;
    private final Label nicknameValue;
    private final Label emailValue;
    private final Label status;

    public ProfileScreen(PvzApplication app) {
        super(app);
        controller = app.services().profile();
        usernameValue = theme.bodyLabel("");
        nicknameValue = theme.bodyLabel("");
        emailValue = theme.bodyLabel("");
        status = statusLabel();
        buildUi();
        refreshSummary();
    }

    private void buildUi() {
        Table panel = theme.dialogPanel();
        panel.add(titleBar("PROFILE")).colspan(2).growX().padBottom(16f);
        panel.row();
        panel.add(buildSummary()).width(390f).growY().top().padRight(20f);
        panel.add(buildEditor()).width(520f).growY().top();
        panel.row();
        panel.add(status).colspan(2).width(860f).height(34f).padTop(10f);
        panel.row();

        TextButton back = theme.secondaryButton("Back");
        UiActions.onClick(back, app::showMainMenu);
        panel.add(back).colspan(2).width(180f).height(50f).padTop(8f);
        root.add(panel).width(1030f).height(650f).center();
    }

    private Table buildSummary() {
        Table summary = theme.insetPanel(16f);
        summary.top();
        summary.add(theme.heading("PLAYER INFO")).colspan(2).padBottom(10f);
        summary.row();

        addSummaryRow(summary, "Username", usernameValue);
        addSummaryRow(summary, "Nickname", nicknameValue);
        addSummaryRow(summary, "Email", emailValue);
        addSummaryRow(summary, "Gender", theme.bodyLabel(user.getGender()));
        addSummaryRow(summary, "Coins", theme.bodyLabel(Integer.toString(user.getWallet().getCoins())));
        addSummaryRow(summary, "Gems", theme.bodyLabel(Integer.toString(user.getWallet().getGems())));

        GameProgress progress = user.getProgress();
        addSummaryRow(summary, "Games played", theme.bodyLabel(Integer.toString(progress.getGamesPlayed())));
        addSummaryRow(summary, "Completed levels", theme.bodyLabel(Integer.toString(progress.getCompletedLevels())));
        addSummaryRow(summary, "Best Meow Points", theme.bodyLabel(Integer.toString(progress.getBestMeowPoints())));
        addSummaryRow(summary, "Difficulty", theme.bodyLabel(Integer.toString(user.getDifficultyLevel())));
        return summary;
    }

    private void addSummaryRow(Table table, String title, Label value) {
        value.setWrap(false);
        table.add(theme.fieldLabel(title)).left().pad(5f, 4f, 5f, 8f);
        table.add(value).expandX().right().pad(5f, 8f, 5f, 6f);
        table.row();
    }

    private Table buildEditor() {
        Table editor = new Table();
        editor.top();
        editor.add(theme.heading("EDIT PROFILE")).colspan(2).padBottom(8f);
        editor.row();

        TextField username = textField("New username");
        addEditRow(editor, "Username", username, "Save", () -> changeUsername(username));

        TextField nickname = textField("New nickname");
        addEditRow(editor, "Nickname", nickname, "Save", () -> changeNickname(nickname));

        TextField email = textField("New email");
        addEditRow(editor, "Email", email, "Save", () -> changeEmail(email));

        TextField oldPassword = passwordField("Current password");
        TextField newPassword = passwordField("New password");
        editor.add(theme.fieldLabel("Password")).left().colspan(2).padTop(4f);
        editor.row();
        editor.add(oldPassword).width(PASSWORD_FIELD_WIDTH).height(EDIT_FIELD_HEIGHT).padRight(8f);
        editor.add(newPassword).width(PASSWORD_FIELD_WIDTH).height(EDIT_FIELD_HEIGHT);
        editor.row();

        TextButton passwordButton = theme.primaryButton("Change Password");
        UiActions.onClick(passwordButton, () -> changePassword(oldPassword, newPassword));
        editor.add(passwordButton).colspan(2).width(250f).height(50f).padTop(10f);
        return editor;
    }

    private void addEditRow(Table table, String title, TextField field, String buttonText, Runnable action) {
        table.add(theme.fieldLabel(title)).left().colspan(2).padTop(4f);
        table.row();
        table.add(field).width(EDIT_FIELD_WIDTH).height(EDIT_FIELD_HEIGHT).padRight(8f);
        TextButton button = theme.primaryButton(buttonText);
        UiActions.onClick(button, action);
        table.add(button).width(125f).height(EDIT_FIELD_HEIGHT);
        table.row();
    }

    private void changeUsername(TextField field) {
        ActionResult result = controller.changeUsername(field.getText().trim());
        handleResult(result);
        if (result.isSuccessful()) {
            field.setText("");
            refreshSummary();
        }
    }

    private void changeNickname(TextField field) {
        ActionResult result = controller.changeNickname(field.getText().trim());
        handleResult(result);
        if (result.isSuccessful()) {
            field.setText("");
            refreshSummary();
        }
    }

    private void changeEmail(TextField field) {
        ActionResult result = controller.changeEmail(field.getText().trim());
        handleResult(result);
        if (result.isSuccessful()) {
            field.setText("");
            refreshSummary();
        }
    }

    private void changePassword(TextField oldPassword, TextField newPassword) {
        ActionResult result = controller.changePassword(oldPassword.getText(), newPassword.getText());
        handleResult(result);
        if (result.isSuccessful()) {
            oldPassword.setText("");
            newPassword.setText("");
        }
    }

    private void handleResult(ActionResult result) {
        if (result.isSuccessful()) {
            theme.showSuccess(status, result.getMessage());
        } else {
            theme.showError(status, result.getMessage());
        }
    }

    private void refreshSummary() {
        usernameValue.setText(user.getUsername());
        nicknameValue.setText(user.getNickname());
        emailValue.setText(user.getEmail());
    }
}
