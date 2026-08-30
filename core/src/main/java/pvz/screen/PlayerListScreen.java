package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import controller.ActionResult;
import model.User;
import pvz.PvzApplication;

public final class PlayerListScreen extends AuthenticatedUiScreen {
    private final Label status;

    public PlayerListScreen(PvzApplication app) {
        super(app);
        status = statusLabel();
        buildUi();
    }

    private void buildUi() {
        Table panel = theme.dialogPanel();
        panel.add(theme.settingsTitle("Player List")).width(680f).height(52f);
        panel.row().padTop(10f);

        Table players = new Table();
        players.top();
        for (User account : app.services().auth().getUserRepository().getAllUsers()) {
            Table row = new Table();
            TextButton player = theme.secondaryButton(account.getNickname());
            player.getLabel().setFontScale(0.9f);
            UiActions.onClick(player, () -> select(account.getUsername()));
            TextButton delete = theme.tertiaryButton("DELETE");
            delete.getLabel().setFontScale(0.7f);
            UiActions.onClick(delete, () -> confirmDelete(account));
            row.add(player).width(540f).height(56f).padRight(10f);
            row.add(delete).width(120f).height(56f);
            players.add(row).growX().height(56f).padBottom(8f);
            players.row();
        }
        panel.add(players).width(680f).height(330f).grow();
        panel.row().padTop(4f);
        panel.add(status).width(680f).height(24f);
        panel.row().padTop(8f);

        TextButton back = theme.secondaryButton("Back");
        TextButton create = theme.primaryButton("CREATE");
        UiActions.onClick(back, app::showMainMenu);
        UiActions.onClick(create, app::showRegister);
        Table actions = new Table();
        actions.add(back).width(170f).height(50f).padRight(10f);
        actions.add(create).width(200f).height(50f);
        panel.add(actions).height(56f);

        addScrollable(panel);
    }

    private void select(String username) {
        if (app.services().auth().selectUser(username).isSuccessful()) {
            app.showMainMenu();
        }
    }

    private void confirmDelete(User account) {
        Dialog dialog = new Dialog("DELETE PLAYER", theme.skin()) {
            @Override
            protected void result(Object value) {
                super.result(value);
                if (Boolean.TRUE.equals(value)) {
                    delete(account.getUsername());
                }
            }
        };
        dialog.text("Delete " + account.getNickname() + " permanently?");
        dialog.button("DELETE", Boolean.TRUE);
        dialog.button("CANCEL", Boolean.FALSE);
        dialog.show(stage);
    }

    private void delete(String username) {
        ActionResult result = app.services().auth().deleteUser(username);
        if (!result.isSuccessful()) {
            theme.showError(status, result.getMessage());
            return;
        }
        if (!app.services().auth().isAuthenticated()) {
            app.showLogin();
            return;
        }
        app.showPlayerList();
    }
}
