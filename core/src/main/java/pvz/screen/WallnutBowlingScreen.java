package pvz.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import model.WallnutBowlingSession;
import pvz.PvzApplication;
import pvz.ui.MiniGameGridInputActor;
import pvz.ui.MiniGameUnitLayer;
import pvz.ui.UiTheme;

import java.util.List;
import java.util.Map;

public final class WallnutBowlingScreen extends MiniGamePlayScreen {
    private static final int ROWS = 5;
    private static final int COLS = 9;
    private static final float BOARD_WIDTH = 860f;
    private static final float BOARD_HEIGHT = 470f;
    private static final String NORMAL_ART = "IMAGE_PLANT_WALLNUT_WALLNUT_100X106";
    private static final String EXPLOSIVE_ART = "IMAGE_PLANT_EXPLODEONUT_EXPLODEONUT_98X124";

    private final WallnutBowlingSession bowling;
    private final MiniGameUnitLayer units;
    private final Group nutLayer;
    private final Table conveyor;
    private final Label progress;
    private String selectedNut = "NORMAL";

    public WallnutBowlingScreen(PvzApplication app) {
        super(app);
        bowling = (WallnutBowlingSession) session;
        units = new MiniGameUnitLayer(app);
        nutLayer = new Group();
        conveyor = new Table();
        progress = theme.settingsLabel("");
        buildUi();
        refreshFromSession();
    }

    private void buildUi() {
        Table screen = new Table();
        screen.top().pad(32f, 36f, 18f, 36f);
        screen.add(titleBar("WALL-NUT BOWLING - LEVEL " + session.getLevel()))
            .colspan(2).width(1200f).height(52f).padBottom(10f);
        screen.row();
        screen.add(buildSidePanel()).width(305f).height(548f).padRight(12f);
        screen.add(buildBoard()).width(BOARD_WIDTH).height(BOARD_HEIGHT).top();
        screen.row();
        message.setAlignment(Align.center);
        screen.add(message).colspan(2).width(1050f).height(28f).padTop(5f);
        root.add(screen).grow();
    }

    private Table buildSidePanel() {
        Table panel = theme.settingsCardPanel(12f);
        panel.top();
        panel.add(theme.heading("CONVEYOR")).padBottom(7f);
        panel.row();
        panel.add(conveyor).width(275f).height(230f).top();
        panel.row().padTop(7f);
        panel.add(theme.heading("BOWL INTO ROW")).padBottom(4f);
        panel.row();

        Table rows = new Table();
        for (int row = 1; row <= 5; row++) {
            int targetRow = row;
            TextButton button = theme.primaryButton("ROW " + row);
            button.getLabel().setFontScale(0.84f);
            UiActions.onClick(button, () -> bowl(targetRow));
            rows.add(button).width(126f).height(45f).pad(3f);
            if (row % 2 == 0) {
                rows.row();
            }
        }
        panel.add(rows).width(275f).height(150f);
        panel.row().padTop(5f);
        progress.setAlignment(Align.center);
        panel.add(progress).width(275f).height(38f);
        panel.row().padTop(7f);
        TextButton back = theme.secondaryButton("Back to Mini Games");
        UiActions.onClick(back, app::returnToMiniGames);
        panel.add(back).width(255f).height(50f);
        return panel;
    }

    private Stack buildBoard() {
        Stack board = new Stack();
        Image background = theme.image("IMAGE_BACKGROUNDS_EGYPT_TEXTURE");
        if (background != null) {
            background.setScaling(Scaling.stretch);
            board.add(background);
        }
        board.add(units);
        board.add(nutLayer);
        board.add(redLineOverlay());
        board.add(new MiniGameGridInputActor(ROWS, COLS,
            cell -> bowl(cell.row() + 1)));
        return board;
    }

    private Table redLineOverlay() {
        Table layer = new Table();
        layer.setFillParent(true);
        layer.left();
        layer.add().width(BOARD_WIDTH * 3f / COLS);
        Image divider = theme.image(UiTheme.DIVIDER);
        if (divider != null) {
            divider.setScaling(Scaling.stretch);
            divider.setColor(new Color(0.82f, 0.06f, 0.03f, 0.96f));
            layer.add(divider).width(6f).growY();
        }
        layer.add().expandX();
        return layer;
    }

    private void bowl(int row) {
        if (session.isFinished()) {
            return;
        }
        execute("bowl " + selectedNut.toLowerCase() + " " + row);
    }

    @Override
    protected void refreshFromSession() {
        units.setPlants(List.of());
        units.setZombies(bowling.getZombieViews());
        rebuildConveyor(bowling.getConveyorCounts());
        rebuildNuts(bowling.getNutViews());
        progress.setText(
            "Kills " + bowling.getKills() + " / " + session.getTarget()
                + "   |   " + shortStatus()
        );
    }

    private void rebuildConveyor(Map<String, Integer> counts) {
        conveyor.clearChildren();
        for (String type : List.of("NORMAL", "GIANT", "EXPLOSIVE")) {
            Table card = theme.settingsBadgePanel(5f);
            Image image = theme.image(nutArt(type));
            if (image != null) {
                image.setScaling(Scaling.fit);
                if ("GIANT".equals(type)) {
                    image.setColor(new Color(0.92f, 0.72f, 0.32f, 1f));
                }
                card.add(image).size(60f).padRight(5f);
            }
            int count = counts.getOrDefault(type, 0);
            TextButton select = type.equals(selectedNut)
                ? theme.tertiaryButton(type + "  x" + count)
                : theme.primaryButton(type + "  x" + count);
            select.getLabel().setFontScale(0.66f);
            select.setDisabled(count <= 0);
            if (count > 0) {
                UiActions.onClick(select, () -> selectNut(type));
            }
            card.add(select).width(180f).height(46f);
            conveyor.add(card).width(270f).height(64f).padBottom(5f);
            conveyor.row();
        }
    }

    private void rebuildNuts(List<WallnutBowlingSession.NutView> nuts) {
        nutLayer.clearChildren();
        float cellWidth = BOARD_WIDTH / COLS;
        float cellHeight = BOARD_HEIGHT / ROWS;
        for (WallnutBowlingSession.NutView nut : nuts) {
            if (!nut.active()) {
                continue;
            }
            Image image = theme.image(nutArt(nut.type()));
            if (image == null) {
                continue;
            }
            image.setScaling(Scaling.fit);
            float size = cellHeight * ("GIANT".equals(nut.type()) ? 0.92f : 0.70f);
            float x = (float) nut.column() * cellWidth - size * 0.45f;
            float y = (ROWS - 1f - (float) nut.row()) * cellHeight
                + (cellHeight - size) * 0.35f;
            image.setBounds(x, y, size, size);
            if ("GIANT".equals(nut.type())) {
                image.setColor(new Color(0.92f, 0.72f, 0.32f, 1f));
            }
            nutLayer.addActor(image);
        }
    }

    private String nutArt(String type) {
        return switch (type) {
            case "EXPLOSIVE" -> EXPLOSIVE_ART;
            default -> NORMAL_ART;
        };
    }

    private void selectNut(String type) {
        selectedNut = type;
        rebuildConveyor(bowling.getConveyorCounts());
        theme.showSuccess(message, type + " selected. Choose a row.");
    }
}
