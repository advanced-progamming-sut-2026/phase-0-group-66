package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import controller.ActionResult;
import controller.ProfileController;
import model.GameProgress;
import pvz.PvzApplication;
import pvz.ui.UiTheme;

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
        panel.add(buildSummary()).width(390f).top().padRight(20f);
        panel.add(buildEditor()).width(520f).top();
        panel.row();
        panel.add(status).colspan(2).width(860f).height(34f).padTop(10f);
        panel.row();

        TextButton back = theme.secondaryButton("Back");
        UiActions.onClick(back, app::showMainMenu);
        panel.add(back).colspan(2).width(180f).height(50f).padTop(8f);
        addScrollable(panel);
    }

    private Table buildSummary() {
        Table summary = theme.settingsCardPanel(14f);
        summary.top().left();
        summary.add(buildIdentityHeader()).growX().height(78f);
        summary.row();

        summary.add(buildWalletBadges()).growX().padTop(8f).padBottom(10f);
        summary.row();
        summary.add(sectionTitle("ACCOUNT")).growX().padBottom(4f);
        summary.row();
        addSummaryRow(summary, "Username", usernameValue);
        addSummaryRow(summary, "Nickname", nicknameValue);
        addSummaryRow(summary, "Email", emailValue);
        addSummaryRow(summary, "Gender", theme.bodyLabel(user.getGender()));

        GameProgress progress = user.getProgress();
        summary.add(sectionTitle("PROGRESS")).growX().padTop(8f).padBottom(4f);
        summary.row();
        summary.add(buildProgressGrid(progress)).growX();
        return summary;
    }

    private Table buildIdentityHeader() {
        Table header = theme.settingsBadgePanel(10f);
        Image playerIcon = theme.image(UiTheme.PLAYER_ICON);
        if (playerIcon != null) {
            header.add(playerIcon).size(62f).padRight(10f);
        }
        Table copy = new Table();
        copy.top().left();
        copy.add(theme.heading("PLAYER PROFILE")).left().padBottom(3f);
        copy.row();
        copy.add(theme.settingsLabel("Your garden account")).left();
        header.add(copy).expandX().left();
        return header;
    }

    private Table buildWalletBadges() {
        Table wallet = new Table();
        wallet.add(walletBadge(UiTheme.COIN_ICON, "Coins", user.getWallet().getCoins()))
            .width(168f).height(58f).padRight(6f);
        wallet.add(walletBadge(UiTheme.GEM_ICON, "Gems", user.getWallet().getGems()))
            .width(168f).height(58f);
        return wallet;
    }

    private Table walletBadge(String iconId, String title, int value) {
        Table badge = theme.settingsBadgePanel(7f);
        Image icon = theme.image(iconId);
        if (icon != null) {
            badge.add(icon).size(28f).padRight(5f);
        }
        Table text = new Table();
        text.top().left();
        text.add(theme.settingsLabel(title)).left();
        text.row();
        Label amount = theme.heading(Integer.toString(value));
        amount.setFontScale(0.72f);
        text.add(amount).left();
        badge.add(text).expandX().left();
        return badge;
    }

    private Label sectionTitle(String title) {
        Label label = theme.settingsLabel(title);
        label.setFontScale(0.72f);
        return label;
    }

    private Table buildProgressGrid(GameProgress progress) {
        Table grid = new Table();
        grid.add(metricCard("Games played", progress.getGamesPlayed()))
            .width(168f).height(70f).padRight(6f).padBottom(6f);
        grid.add(metricCard("Completed levels", progress.getCompletedLevels()))
            .width(168f).height(70f).padBottom(6f);
        grid.row();
        grid.add(metricCard("Best Meow Points", progress.getBestMeowPoints()))
            .width(168f).height(70f).padRight(6f);
        grid.add(metricCard("Difficulty", user.getDifficultyLevel()))
            .width(168f).height(70f);
        return grid;
    }

    private Table metricCard(String title, int value) {
        Table card = theme.settingsBadgePanel(8f);
        Label caption = theme.settingsLabel(title);
        caption.setFontScale(0.55f);
        caption.setWrap(false);
        card.add(caption).left().growX();
        card.row();
        Label amount = theme.heading(Integer.toString(value));
        amount.setFontScale(0.78f);
        card.add(amount).left().padTop(2f);
        return card;
    }

    private void addSummaryRow(Table table, String title, Label value) {
        value.setWrap(false);
        table.add(theme.fieldLabel(title)).left().pad(5f, 4f, 5f, 8f);
        table.add(value).expandX().right().pad(5f, 8f, 5f, 6f);
        table.row();
    }

    private Table buildEditor() {
        Table editor = theme.settingsCardPanel(14f);
        editor.top().left();
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
