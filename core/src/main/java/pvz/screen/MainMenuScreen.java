package pvz.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import controller.ActionResult;
import model.User;
import pvz.PvzApplication;

public final class MainMenuScreen extends BaseUiScreen {
    public MainMenuScreen(PvzApplication app) {
        super(app);
        buildUi();
    }

    private void buildUi() {
        User user = app.services().auth().getCurrentUser();
        Table layout = new Table();
        layout.add(new Label("PLANTS VS ZOMBIES 2", app.assets().skin(), "big_outline")).colspan(2);
        layout.row().padTop(10f);
        layout.add(new Label("Welcome, " + user.getNickname(), app.assets().skin(), "medium")).colspan(2);
        layout.row().padTop(5f);
        layout.add(walletLabel(user)).colspan(2);
        layout.row().padTop(24f);

        addMenuRow(layout, "Adventure", "Collection");
        addMenuRow(layout, "Greenhouse", "Travel Log");
        addMenuRow(layout, "Settings", "News");
        addMenuRow(layout, "Profile", "Leaderboard");

        TextButton logout = new TextButton("Logout", app.assets().skin(), "brown");
        TextButton exit = new TextButton("Exit Game", app.assets().skin(), "purple");
        UiActions.onClick(logout, this::logout);
        UiActions.onClick(exit, Gdx.app::exit);
        layout.add(logout).width(230f).height(60f).pad(8f);
        layout.add(exit).width(230f).height(60f).pad(8f);
        root.add(layout);
    }

    private Label walletLabel(User user) {
        String text = "Coins: " + user.getWallet().getCoins() + "   |   Gems: " + user.getWallet().getGems()
            + "   |   Difficulty: " + user.getDifficultyLevel();
        return new Label(text, app.assets().skin(), "secondary");
    }

    private void addMenuRow(Table table, String leftTitle, String rightTitle) {
        table.add(menuButton(leftTitle)).width(270f).height(64f).pad(8f);
        table.add(menuButton(rightTitle)).width(270f).height(64f).pad(8f);
        table.row();
    }

    private TextButton menuButton(String title) {
        TextButton button = new TextButton(title, app.assets().skin(), "green");
        UiActions.onClick(button, () -> app.showPlaceholder(title));
        return button;
    }

    private void logout() {
        ActionResult result = app.services().auth().logout();
        if (result.isSuccessful()) {
            app.showLogin();
        }
    }
}
