package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import controller.ActionResult;
import controller.SettingsController;
import pvz.PvzApplication;

public final class SettingsScreen extends AuthenticatedUiScreen {
    private final SettingsController controller;
    private final Label difficultyValue;
    private final Label speedValue;
    private final Label status;

    public SettingsScreen(PvzApplication app) {
        super(app);
        controller = app.services().settings();
        difficultyValue = theme.heading("");
        speedValue = theme.heading("");
        status = statusLabel();
        buildUi();
        refreshValues();
    }

    private void buildUi() {
        Table panel = theme.dialogPanel();
        panel.add(titleBar("SETTINGS")).growX().padBottom(16f);
        panel.row();
        panel.add(buildSettingsBody()).width(820f).height(420f);
        panel.row();
        panel.add(status).width(780f).height(36f).padTop(8f);
        panel.row();

        TextButton back = theme.secondaryButton("Back");
        UiActions.onClick(back, app::showMainMenu);
        panel.add(back).width(190f).height(50f).padTop(8f);
        root.add(panel).width(940f).height(620f).center();
    }

    private Table buildSettingsBody() {
        Table body = theme.insetPanel(18f);
        body.top();
        addNumberSetting(body, "Difficulty", "1 = easiest, 5 = hardest", 1, 5,
            difficultyValue, this::changeDifficulty);
        addNumberSetting(body, "Game speed", "1 = normal, 3 = fastest", 1, 3,
            speedValue, this::changeGameSpeed);

        CheckBox grid = theme.checkBox(" Show red grid during battles", user.isGridVisible());
        CheckBox debug = theme.checkBox(" Show debug controls during battles", user.isDebugMode());
        UiActions.onClick(grid, () -> changeGrid(grid.isChecked()));
        UiActions.onClick(debug, () -> changeDebug(debug.isChecked()));

        body.add(theme.heading("Battle display")).left().colspan(2).padTop(12f).padBottom(8f);
        body.row();
        body.add(grid).left().colspan(2).padBottom(10f);
        body.row();
        body.add(debug).left().colspan(2);
        return body;
    }

    private void addNumberSetting(Table table, String title, String description, int from, int to,
                                  Label currentValue, IntAction action) {
        Table header = new Table();
        header.add(theme.heading(title)).left();
        header.add().expandX();
        header.add(theme.fieldLabel("Current:"));
        header.add(currentValue).width(42f);
        table.add(header).growX().colspan(2).padBottom(2f);
        table.row();
        table.add(theme.bodyLabel(description)).left().colspan(2).padBottom(8f);
        table.row();

        Table buttons = new Table();
        for (int value = from; value <= to; value++) {
            int selected = value;
            TextButton button = theme.primaryButton(Integer.toString(value));
            UiActions.onClick(button, () -> action.run(selected));
            buttons.add(button).width(82f).height(48f).padRight(8f);
        }
        table.add(buttons).left().colspan(2).padBottom(14f);
        table.row();
    }

    private void changeDifficulty(int value) {
        handleResult(controller.changeDifficulty(value));
        refreshValues();
    }

    private void changeGameSpeed(int value) {
        handleResult(controller.changeGameSpeed(value));
        refreshValues();
    }

    private void changeGrid(boolean visible) {
        handleResult(controller.changeGridVisible(visible));
    }

    private void changeDebug(boolean enabled) {
        handleResult(controller.changeDebugMode(enabled));
    }

    private void refreshValues() {
        difficultyValue.setText(Integer.toString(user.getDifficultyLevel()));
        speedValue.setText(Integer.toString(user.getGameSpeed()));
    }

    private void handleResult(ActionResult result) {
        if (result.isSuccessful()) {
            theme.showSuccess(status, result.getMessage());
        } else {
            theme.showError(status, result.getMessage());
        }
    }

    @FunctionalInterface
    private interface IntAction {
        void run(int value);
    }
}
