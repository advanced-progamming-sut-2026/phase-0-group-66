package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import model.BeghouledSession;
import model.MiniGamePlantSnapshot;
import pvz.PvzApplication;
import pvz.ui.MiniGameGridInputActor;
import pvz.ui.MiniGameUnitLayer;
import pvz.ui.UiTheme;

public final class BeghouledScreen extends MiniGamePlayScreen {
    private static final int ROWS = 5;
    private static final int COLS = 9;
    private static final float BOARD_WIDTH = 855f;
    private static final float BOARD_HEIGHT = 470f;

    private final BeghouledSession beghouled;
    private final MiniGameUnitLayer units;
    private final com.badlogic.gdx.scenes.scene2d.Group selectionLayer;
    private final Label sunLabel;
    private final Label progress;
    private int selectedRow = -1;
    private int selectedColumn = -1;

    public BeghouledScreen(PvzApplication app) {
        super(app);
        beghouled = (BeghouledSession) session;
        units = new MiniGameUnitLayer(app);
        selectionLayer = new com.badlogic.gdx.scenes.scene2d.Group();
        sunLabel = theme.heading("");
        progress = theme.settingsLabel("");
        buildUi();
        refreshFromSession();
    }

    private void buildUi() {
        Table screen = new Table();
        screen.top().pad(32f, 34f, 18f, 34f);
        screen.add(titleBar("BEGHOULD - LEVEL " + session.getLevel()))
            .colspan(2).width(1205f).height(52f).padBottom(10f);
        screen.row();
        screen.add(buildSidePanel()).width(315f).height(548f).padRight(12f);
        screen.add(buildBoard()).width(BOARD_WIDTH).height(BOARD_HEIGHT).top();
        screen.row();
        message.setAlignment(Align.center);
        screen.add(message).colspan(2).width(1060f).height(28f).padTop(5f);
        root.add(screen).grow();
    }

    private Table buildSidePanel() {
        Table panel = theme.settingsCardPanel(12f);
        panel.top();
        Table currency = theme.settingsBadgePanel(6f);
        Image icon = theme.image("IMAGE_UI_HUD_INGAME_SUN");
        if (icon != null) {
            currency.add(icon).size(34f).padRight(6f);
        }
        currency.add(sunLabel).right().width(210f);
        panel.add(currency).width(280f).height(52f).padBottom(8f);
        panel.row();
        panel.add(theme.heading("MAKE MATCHES")).padBottom(4f);
        panel.row();
        Label hint = theme.bodyLabel("Click two adjacent plants to swap them. Every match of three or more earns sun.");
        hint.setAlignment(Align.left);
        hint.setWrap(true);
        hint.setFontScale(0.72f);
        panel.add(hint).width(280f).height(82f).left();
        panel.row().padTop(8f);
        panel.add(progress).width(280f).height(42f);
        panel.row().padTop(8f);
        panel.add(upgradeButton("Peashooter -> Repeater", "peashooter"))
            .width(280f).height(42f).padBottom(5f);
        panel.row();
        panel.add(upgradeButton("Wall-nut -> Tall-nut", "wallnut"))
            .width(280f).height(42f).padBottom(5f);
        panel.row();
        panel.add(upgradeButton("Cabbage-pult -> Melon-pult", "cabbagepult"))
            .width(280f).height(42f).padBottom(5f);
        panel.row();
        TextButton back = theme.secondaryButton("Back to Mini Games");
        UiActions.onClick(back, app::returnToMiniGames);
        panel.add(back).width(255f).height(48f);
        return panel;
    }

    private TextButton upgradeButton(String text, String plant) {
        TextButton button = theme.primaryButton(text);
        button.getLabel().setFontScale(0.58f);
        button.getLabel().setWrap(true);
        button.getLabel().setAlignment(Align.center);
        UiActions.onClick(button, () -> execute("upgrade " + plant));
        return button;
    }

    private Stack buildBoard() {
        Stack board = new Stack();
        Image background = theme.image("IMAGE_BACKGROUNDS_EGYPT_TEXTURE");
        if (background != null) {
            background.setScaling(Scaling.stretch);
            board.add(background);
        }
        board.add(units);
        board.add(selectionLayer);
        board.add(new MiniGameGridInputActor(ROWS, COLS,
            cell -> selectCell(cell.row(), cell.column())));
        return board;
    }

    private void selectCell(int row, int column) {
        if (session.isFinished()) {
            return;
        }
        if (selectedRow < 0) {
            selectedRow = row;
            selectedColumn = column;
            showSelection();
            theme.showSuccess(message, "Plant selected. Choose an adjacent plant.");
            return;
        }
        if (selectedRow == row && selectedColumn == column) {
            selectedRow = -1;
            selectedColumn = -1;
            selectionLayer.clearChildren();
            message.setText(shortStatus());
            return;
        }
        int firstRow = selectedRow;
        int firstColumn = selectedColumn;
        selectedRow = -1;
        selectedColumn = -1;
        selectionLayer.clearChildren();
        execute("swap " + (firstColumn + 1) + " " + (firstRow + 1) + " "
            + (column + 1) + " " + (row + 1));
    }

    private void showSelection() {
        selectionLayer.clearChildren();
        Image highlight = theme.image(UiTheme.DIVIDER);
        if (highlight == null) {
            return;
        }
        highlight.setScaling(Scaling.stretch);
        highlight.setColor(0.86f, 1f, 0.30f, 0.38f);
        float cellWidth = BOARD_WIDTH / COLS;
        float cellHeight = BOARD_HEIGHT / ROWS;
        highlight.setBounds(selectedColumn * cellWidth + 3f,
            selectedRow * cellHeight + 3f, cellWidth - 6f, cellHeight - 6f);
        selectionLayer.addActor(highlight);
    }

    @Override
    protected void refreshFromSession() {
        units.setPlants(beghouled.getPlantViews().stream()
            .map(plant -> new MiniGamePlantSnapshot(plant.type(), plant.row(), plant.column(),
                plant.health(), plant.damage()))
            .toList());
        units.setZombies(beghouled.getZombieViews());
        sunLabel.setText(Integer.toString(beghouled.getSun()));
        progress.setText("Matches " + beghouled.getMatches() + " / " + session.getTarget()
            + "   |   " + shortStatus());
    }
}
