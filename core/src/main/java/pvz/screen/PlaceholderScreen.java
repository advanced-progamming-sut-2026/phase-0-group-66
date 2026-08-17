package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import pvz.PvzApplication;

public final class PlaceholderScreen extends BaseUiScreen {
    public PlaceholderScreen(PvzApplication app, String title) {
        this(app, title, "Back to Main Menu", app::showMainMenu);
    }

    public PlaceholderScreen(
        PvzApplication app,
        String title,
        String backText,
        Runnable backAction
    ) {
        super(app);

        Table content = theme.settingsCardPanel(24f);
        content.add(new Label(title.toUpperCase(), app.assets().skin(), "big_outline"));
        content.row().padTop(18f);
        content.add(new Label("This screen is the next Phase 2 step.", app.assets().skin(), "secondary"));
        content.row().padTop(24f);

        TextButton back = new TextButton(backText, app.assets().skin(), "green");
        UiActions.onClick(back, backAction);
        content.add(back).width(300f).height(62f);
        root.add(content).width(620f).pad(40f);
    }
}
