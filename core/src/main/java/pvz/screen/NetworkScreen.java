package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import pvz.PvzApplication;

public final class NetworkScreen extends AuthenticatedUiScreen {
    public NetworkScreen(PvzApplication app) {
        super(app);
        buildUi();
    }

    private void buildUi() {
        Table screen = new Table();
        screen.top().pad(24f, 54f, 16f, 54f);
        screen.add(titleBar("NETWORK")).width(1170f).height(56f).padBottom(24f);
        screen.row();

        Table panel = theme.settingsCardPanel(28f);
        Label title = theme.heading("NETWORK PLAY");
        Label message = theme.bodyLabel("Online play is not available in this build.");
        message.setWrap(true);
        TextButton back = theme.secondaryButton("BACK");
        UiActions.onClick(back, app::showMainMenu);

        panel.add(title).growX().height(52f);
        panel.row().padTop(12f);
        panel.add(message).width(560f).height(72f);
        panel.row().padTop(18f);
        panel.add(back).width(240f).height(52f);

        screen.add(panel).width(680f).height(270f).center();
        root.add(screen).grow();
    }
}
