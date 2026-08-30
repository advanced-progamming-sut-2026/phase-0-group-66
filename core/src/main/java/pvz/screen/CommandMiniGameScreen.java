package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Align;
import pvz.PvzApplication;

/**
 * Graphical command surface for the two phase-two mini-games without a
 * dedicated board renderer yet.
 */
public final class CommandMiniGameScreen extends MiniGamePlayScreen {
    private final Label board;
    private final Label progress;
    private final TextField commandField;

    public CommandMiniGameScreen(PvzApplication app, String miniGameName) {
        super(app);
        board = theme.bodyLabel("");
        progress = theme.settingsLabel("");
        commandField = theme.textField("swap 1 1 1 2");
        buildUi(miniGameName);
        refreshFromSession();
    }

    @Override
    protected void handleEscape() {
        app.returnToMiniGames();
    }

    private void buildUi(String miniGameName) {
        Table screen = new Table();
        screen.top().pad(24f, 42f, 16f, 42f);
        screen.add(titleBar(miniGameName + " - LEVEL " + session.getLevel()))
            .colspan(2).width(1190f).height(56f).padBottom(12f);
        screen.row();

        board.setAlignment(Align.left);
        board.setWrap(false);
        ScrollPane boardPane = new ScrollPane(board, theme.skin());
        boardPane.setFadeScrollBars(false);
        boardPane.setScrollingDisabled(false, false);

        Table controls = theme.settingsCardPanel(14f);
        controls.top();
        controls.add(theme.heading("COMMANDS")).width(290f).height(42f);
        controls.row().padTop(6f);
        Label help = theme.bodyLabel(miniGames.currentHelp());
        help.setAlignment(Align.left);
        help.setWrap(true);
        controls.add(help).width(290f).height(122f);
        controls.row().padTop(10f);
        controls.add(theme.fieldLabel("Command")).left();
        controls.row().padTop(4f);
        controls.add(commandField).width(290f).height(52f);
        controls.row().padTop(8f);

        TextButton execute = theme.primaryButton("EXECUTE");
        UiActions.onClick(execute, this::submitCommand);
        controls.add(execute).width(290f).height(48f);
        controls.row().padTop(8f);
        controls.add(progress).width(290f).height(44f);
        controls.row().padTop(10f);

        TextButton back = theme.secondaryButton("Back to Mini Games");
        UiActions.onClick(back, app::returnToMiniGames);
        controls.add(back).width(290f).height(48f);

        screen.add(boardPane).width(850f).height(548f).top().padRight(14f);
        screen.add(controls).width(320f).height(548f).top();
        screen.row().padTop(8f);
        message.setAlignment(Align.center);
        screen.add(message).colspan(2).width(1050f).height(32f);
        root.add(screen).grow();
    }

    private void submitCommand() {
        String command = commandField.getText().trim();
        if (command.isEmpty()) {
            theme.showError(message, "Enter a mini-game command first.");
            return;
        }
        execute(command);
        commandField.setText("");
    }

    @Override
    protected void refreshFromSession() {
        board.setText(miniGames.currentBoard());
        progress.setText(shortStatus());
    }
}
