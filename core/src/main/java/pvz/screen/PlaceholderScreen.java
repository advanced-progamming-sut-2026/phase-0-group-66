package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import pvz.PvzApplication;

public final class PlaceholderScreen extends BaseUiScreen {
    public PlaceholderScreen(PvzApplication app, String title) {
        super(app);
        Table content = new Table();
        content.add(new Label(title.toUpperCase(), app.assets().skin(), "big_outline"));
        content.row().padTop(18f);
        content.add(new Label("This screen is the next Phase 2 step.", app.assets().skin(), "secondary"));
        content.row().padTop(24f);
        TextButton back = new TextButton("Back to Main Menu", app.assets().skin(), "green");
        UiActions.onClick(back, app::showMainMenu);
        content.add(back).width(280f).height(62f);
        root.add(content);
    }
}
