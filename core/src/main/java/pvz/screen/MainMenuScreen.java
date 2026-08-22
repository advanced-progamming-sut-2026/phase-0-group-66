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
    private static final String PROMO_BANNER = "IMAGE_UI_MAINMENU_MAINMENU_CONTENT_OFFLINE";
    private static final String MENU_BUTTON_BACKGROUND = "IMAGE_UI_MAINMENU_BTN_BKGD";
    private static final String NETWORK_ICON = "IMAGE_UI_MAINMENU_MM_ICLOUDICON";

    private static final float LOGO_WIDTH = 520f;
    private static final float LOGO_HEIGHT = 88f;
    private static final float BANNER_WIDTH = 760f;
    private static final float BANNER_HEIGHT = 263f;
    private static final float ICON_BUTTON_SIZE = 88f;

    private final User user;

    public MainMenuScreen(PvzApplication app) {
        super(app);
        user = app.services().auth().getCurrentUser();
        buildUi();
    }

    private void buildUi() {
        Table screen = new Table();
        screen.setFillParent(true);
        screen.top();
        screen.pad(28f, 42f, 26f, 42f);

        addLogo(screen);
        screen.row().padTop(18f);
        screen.add(buildPromoBanner()).width(BANNER_WIDTH).height(BANNER_HEIGHT);
        screen.row().padTop(10f);
        screen.add(buildPagerDots()).height(24f);
        screen.row();
        screen.add().expandY();
        screen.row();
        screen.add(buildBottomBar()).growX();
        screen.row().padTop(12f);
        screen.add(buildAccountFooter()).growX();

        root.add(screen).grow();
    }

    private void addLogo(Table screen) {
        Image logo = theme.pvzLogo();
        if (logo != null) {
            screen.add(logo).width(LOGO_WIDTH).height(LOGO_HEIGHT).center();
            return;
        }
        screen.add(theme.title("PLANTS VS. ZOMBIES 2")).center();
    }

    private Stack buildPromoBanner() {
        Stack banner = new Stack();
        Image promo = theme.image(PROMO_BANNER);
        if (promo != null) {
            banner.add(promo);
        } else {
            Table fallback = new Table();
            fallback.setBackground(
                theme.skin().getDrawable("image_ui_dialog_asset_inner_bkgd_10")
            );
            banner.add(fallback);
        }

        Table actionLayer = new Table();
        actionLayer.setFillParent(true);
        actionLayer.bottom().right();
        TextButton news = theme.tertiaryButton("NEWS");
        UiActions.onClick(news, app::showNews);
        actionLayer.add(news).width(132f).height(46f).padRight(18f).padBottom(16f);
        banner.add(actionLayer);
        return banner;
    }

    private Label buildPagerDots() {
        Label dots = theme.heading("●  ○  ○  ○");
        dots.setFontScale(0.72f);
        return dots;
    }

    private Table buildBottomBar() {
        Table bottom = new Table();

        Table left = new Table();
        left.add(iconButton(NETWORK_ICON, app::showNetwork, 0))
            .size(ICON_BUTTON_SIZE)
            .padRight(14f);
        left.add(iconButton(UiTheme.NEWS_ICON, app::showNews, unreadNewsCount()))
            .size(ICON_BUTTON_SIZE);

        Table right = new Table();
        right.add(iconButton(UiTheme.SETTINGS_ICON, app::showSettings, 0))
            .size(ICON_BUTTON_SIZE)
            .padRight(14f);
        right.add(iconButton(UiTheme.LEADERBOARD_ICON, app::showLeaderboard, 0))
            .size(ICON_BUTTON_SIZE);

        TextButton play = theme.tertiaryButton("PLAY");
        play.getLabel().setFontScale(1.25f);
        UiActions.onClick(play, app::showAdventure);

        bottom.add(left).width(210f).left();
        bottom.add().expandX();
        bottom.add(play).width(310f).height(82f).center();
        bottom.add().expandX();
        bottom.add(right).width(210f).right();
        return bottom;
    }

    private Button iconButton(String iconId, Runnable action, long notificationCount) {
        Button.ButtonStyle style = new Button.ButtonStyle();
        Drawable background = theme.drawable(MENU_BUTTON_BACKGROUND);
        if (background == null) {
            background = theme.skin().getDrawable("image_ui_dialog_asset_inner_bkgd_10");
        }
        style.up = background;
        style.down = background;

        Button button = new Button(style);
        Image icon = theme.image(iconId);
        if (icon != null) {
            button.add(icon).size(62f);
        } else {
            button.add(theme.heading("?"));
        }

        if (notificationCount > 0) {
            button.addActor(buildNotificationBadge(notificationCount));
        }
        UiActions.onClick(button, action);
        return button;
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
        number.setFontScale(0.62f);
        badge.add(number);

        layer.add(badge).size(34f).padTop(-6f).padRight(-6f);
        return layer;
    }

    private Table buildAccountFooter() {
        Table footer = new Table();

        Label welcome = theme.bodyLabel("Welcome back, " + user.getNickname() + ".");
        welcome.setWrap(false);
        footer.add(welcome).width(360f).left();
        footer.add().expandX();

        TextButton logout = theme.secondaryButton("Logout");
        TextButton exit = theme.tertiaryButton("Exit Game");
        UiActions.onClick(logout, this::logout);
        UiActions.onClick(exit, Gdx.app::exit);

        footer.add(logout).width(150f).height(48f).padRight(8f);
        footer.add(exit).width(150f).height(48f);
        return footer;
    }

    private void logout() {
        ActionResult result = app.services().auth().logout();
        if (result.isSuccessful()) {
            app.showLogin();
        }
    }

    private long unreadNewsCount() {
        return user.getNews().stream().filter(News::isUnread).count();
    }
}
