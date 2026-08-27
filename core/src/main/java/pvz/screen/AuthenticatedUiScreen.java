package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import model.User;
import pvz.PvzApplication;
import pvz.ui.UiTheme;

public abstract class AuthenticatedUiScreen extends BaseUiScreen {
    protected final User user;

    protected AuthenticatedUiScreen(PvzApplication app) {
        super(app);
        user = app.services().auth().getCurrentUser();
    }

    @Override
    protected void handleEscape() {
        app.showMainMenu();
    }

    protected Table titleBar(String title) {
        Table bar = new Table();
        bar.add(theme.title(title)).expandX().left();
        bar.add(currencyBadge(UiTheme.COIN_ICON, user.getWallet().getCoins()))
            .width(118f).height(46f).padRight(8f);
        bar.add(currencyBadge(UiTheme.GEM_ICON, user.getWallet().getGems()))
            .width(118f).height(46f);
        return bar;
    }

    private Table currencyBadge(String iconId, int value) {
        Table badge = theme.settingsBadgePanel(5f);
        Image icon = theme.image(iconId);
        if (icon != null) {
            badge.add(icon).size(30f).padRight(5f);
        }
        badge.add(theme.settingsLabel(Integer.toString(value))).right().width(72f);
        return badge;
    }
}
