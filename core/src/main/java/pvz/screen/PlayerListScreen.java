package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import model.User;
import pvz.PvzApplication;

public final class PlayerListScreen extends AuthenticatedUiScreen {
    public PlayerListScreen(PvzApplication app) {
        super(app);
        buildUi();
    }

    private void buildUi() {
        Table panel = theme.dialogPanel();
        panel.add(theme.settingsTitle("Player List")).width(780f).height(58f);
        panel.row().padTop(14f);

        Table players = new Table();
        players.top();
        for (User account : app.services().auth().getUserRepository().getAllUsers()) {
            TextButton player = theme.secondaryButton(account.getNickname());
            player.getLabel().setFontScale(0.9f);
            UiActions.onClick(player, () -> select(account.getUsername()));
            players.add(player).growX().height(62f).padBottom(10f);
            players.row();
        }
        panel.add(players).width(780f).height(390f).grow();
        panel.row().padTop(14f);

        TextButton back = theme.secondaryButton("Back");
        TextButton create = theme.primaryButton("CREATE");
        UiActions.onClick(back, app::showMainMenu);
        UiActions.onClick(create, app::showRegister);
        Table actions = new Table();
        actions.add(back).width(190f).height(56f).padRight(12f);
        actions.add(create).width(220f).height(56f);
        panel.add(actions).height(56f);

        root.add(panel).width(900f).height(620f).center();
    }

    private void select(String username) {
        if (app.services().auth().selectUser(username).isSuccessful()) {
            app.showMainMenu();
        }
    }
}
