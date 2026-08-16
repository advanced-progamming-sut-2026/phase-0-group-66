package pvz.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import controller.ActionResult;
import model.News;
import model.User;
import pvz.PvzApplication;
import pvz.ui.UiTheme;

public final class MainMenuScreen extends BaseUiScreen {
    private static final float TILE_WIDTH = 245f;
    private static final float TILE_HEIGHT = 112f;

    private final User user;

    public MainMenuScreen(PvzApplication app) {
        super(app);
        user = app.services().auth().getCurrentUser();
        buildUi();
    }

    private void buildUi() {
        Table screen = new Table();
        screen.top();
        screen.add().height(38f);
        screen.row();
        screen.add(buildTopHud()).growX().pad(0f, 24f, 0f, 24f);
        screen.row();
        screen.add(buildMenuArea()).expand().center().padTop(16f);
        screen.row();
        screen.add(buildFooter()).growX().pad(0f, 24f, 16f, 24f);
        root.add(screen).grow();
    }

    private Table buildTopHud() {
        Table hud = new Table();
        Image logo = theme.pvzLogo();
        if (logo != null) {
            hud.add(logo).width(250f).height(43f).left();
        }
        hud.add(buildPlayerCard()).expandX().left().padLeft(18f);
        hud.add(buildCurrencyBadge(UiTheme.COIN_ICON, Integer.toString(user.getWallet().getCoins())))
            .width(145f).height(54f).padRight(8f);
        hud.add(buildCurrencyBadge(UiTheme.GEM_ICON, Integer.toString(user.getWallet().getGems())))
            .width(145f).height(54f).padRight(8f);
        hud.add(buildDifficultyBadge()).width(165f).height(54f);
        return hud;
    }

    private Table buildPlayerCard() {
        Table card = theme.insetPanel(8f);
        Image player = theme.image(UiTheme.PLAYER_ICON);
        if (player != null) {
            card.add(player).size(38f).padRight(8f);
        }
        Table text = new Table();
        text.add(theme.heading(user.getNickname())).left();
        text.row();
        text.add(theme.fieldLabel("@" + user.getUsername())).left();
        card.add(text).left();
        return card;
    }

    private Table buildCurrencyBadge(String iconId, String value) {
        Table badge = theme.insetPanel(7f);
        Image icon = theme.image(iconId);
        if (icon != null) {
            badge.add(icon).size(36f).padRight(7f);
        }
        badge.add(theme.heading(value));
        return badge;
    }

    private Stack buildDifficultyBadge() {
        Stack stack = new Stack();
        Image background = theme.image(UiTheme.DIFFICULTY_BG);
        if (background != null) {
            stack.add(background);
        } else {
            stack.add(theme.insetPanel(4f));
        }
        Table text = new Table();
        text.add(theme.fieldLabel("DIFFICULTY"));
        text.row();
        text.add(theme.heading(Integer.toString(user.getDifficultyLevel())));
        stack.add(text);
        return stack;
    }

    private Table buildMenuArea() {
        Table area = new Table();
        area.add(theme.title("MAIN MENU")).colspan(4).padBottom(12f);
        area.row();
        addMenuRow(area,
            menuTile("Adventure", UiTheme.ADVENTURE_ICON, () -> open("Adventure"), 0),
            menuTile("Collection", UiTheme.ALMANAC_ICON, () -> open("Collection"), 0),
            menuTile("Greenhouse", UiTheme.GREENHOUSE_ICON, () -> open("Greenhouse"), 0),
            menuTile("Travel Log", UiTheme.QUEST_ICON, () -> open("Travel Log"), 0));
        addMenuRow(area,
            menuTile("Settings", UiTheme.SETTINGS_ICON, () -> open("Settings"), 0),
            menuTile("News", UiTheme.NEWS_ICON, () -> open("News"), unreadNewsCount()),
            menuTile("Profile", UiTheme.PLAYER_ICON, () -> open("Profile"), 0),
            menuTile("Leaderboard", UiTheme.LEADERBOARD_ICON, () -> open("Leaderboard"), 0));
        return area;
    }

    private void addMenuRow(Table table, Button first, Button second, Button third, Button fourth) {
        table.add(first).width(TILE_WIDTH).height(TILE_HEIGHT).pad(6f);
        table.add(second).width(TILE_WIDTH).height(TILE_HEIGHT).pad(6f);
        table.add(third).width(TILE_WIDTH).height(TILE_HEIGHT).pad(6f);
        table.add(fourth).width(TILE_WIDTH).height(TILE_HEIGHT).pad(6f);
        table.row();
    }

    private Button menuTile(String title, String iconId, Runnable action, long notificationCount) {
        Button.ButtonStyle style = new Button.ButtonStyle();
        Drawable background = theme.drawable(UiTheme.MAIN_MENU_TILE);
        if (background == null) {
            background = theme.skin().getDrawable("image_ui_dialog_asset_inner_bkgd_10");
        }
        style.up = background;
        style.down = background;
        Button button = new Button(style);
        button.add(buildTileContent(title, iconId)).grow();
        if (notificationCount > 0) {
            button.addActor(buildNotificationBadge(notificationCount));
        }
        UiActions.onClick(button, action);
        return button;
    }

    private Table buildTileContent(String title, String iconId) {
        Table content = new Table();
        Image icon = theme.image(iconId);
        if (icon != null) {
            content.add(icon).size(52f).padRight(10f);
        }
        content.add(theme.heading(title));
        return content;
    }

    private Table buildNotificationBadge(long count) {
        Table layer = new Table();
        layer.setFillParent(true);
        layer.top().right();
        Stack badge = new Stack();
        Image dot = theme.image(UiTheme.RED_DOT);
        if (dot != null) {
            badge.add(dot);
        }
        Label number = theme.heading(Long.toString(count));
        badge.add(number);
        layer.add(badge).size(34f).pad(7f);
        return layer;
    }

    private Table buildFooter() {
        Table footer = new Table();
        footer.add(theme.bodyLabel("Welcome back, " + user.getNickname() + ".")).expandX().left();
        TextButton logout = theme.secondaryButton("Logout");
        TextButton exit = theme.tertiaryButton("Exit Game");
        UiActions.onClick(logout, this::logout);
        UiActions.onClick(exit, Gdx.app::exit);
        footer.add(logout).width(150f).height(48f).padRight(8f);
        footer.add(exit).width(150f).height(48f);
        return footer;
    }

    private long unreadNewsCount() {
        return user.getNews().stream().filter(News::isUnread).count();
    }

    private void open(String title) {
        app.showPlaceholder(title);
    }

    private void logout() {
        ActionResult result = app.services().auth().logout();
        if (result.isSuccessful()) {
            app.showLogin();
        }
    }
}
