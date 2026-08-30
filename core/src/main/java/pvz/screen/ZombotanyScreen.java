package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import model.PlantDefinition;
import model.ZombotanySession;
import pvz.PvzApplication;
import pvz.ui.MiniGameGridInputActor;
import pvz.ui.MiniGameUnitLayer;

import java.util.List;

public final class ZombotanyScreen extends MiniGamePlayScreen {
    private static final int ROWS = 5;
    private static final int COLS = 9;
    private static final float BOARD_WIDTH = 855f;
    private static final float BOARD_HEIGHT = 470f;
    private static final String[] PLANTS = {
        "Sunflower", "Peashooter", "Wall-nut", "Snow Pea", "Repeater",
        "Cabbage-pult", "Cherry Bomb", "Squash"
    };

    private final ZombotanySession zombotany;
    private final MiniGameUnitLayer units;
    private final Group sunLayer;
    private final Label sunLabel;
    private final Label progress;
    private final Table plantTray;
    private TextButton startButton;
    private String selectedPlant;
    private boolean feedMode;

    public ZombotanyScreen(PvzApplication app) {
        super(app);
        zombotany = (ZombotanySession) session;
        units = new MiniGameUnitLayer(app);
        sunLayer = new Group();
        sunLabel = theme.heading("");
        progress = theme.settingsLabel("");
        plantTray = new Table();
        selectedPlant = null;
        buildUi();
        refreshFromSession();
    }

    private void buildUi() {
        Table screen = new Table();
        screen.top().pad(32f, 34f, 18f, 34f);
        screen.add(titleBar("ZOMBOTANY - LEVEL " + session.getLevel()))
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
        panel.add(currency).width(280f).height(52f).padBottom(6f);
        panel.row();
        panel.add(theme.heading("PLANT SELECTION")).padBottom(3f);
        panel.row();
        Label hint = theme.bodyLabel("Choose plants before START. During battle, select a plant and click a tile to grow it.");
        hint.setAlignment(Align.left);
        hint.setWrap(true);
        hint.setFontScale(0.68f);
        panel.add(hint).width(280f).height(70f).left();
        panel.row();
        panel.add(plantTray).width(280f).height(270f).top();
        panel.row().padTop(4f);
        startButton = theme.tertiaryButton("START BATTLE");
        startButton.setVisible(!zombotany.isBattleStarted());
        UiActions.onClick(startButton, () -> execute("start"));
        panel.add(startButton).width(280f).height(46f).padBottom(4f);
        panel.row();
        TextButton feed = theme.primaryButton("FEED PLANT");
        feed.getLabel().setFontScale(0.72f);
        UiActions.onClick(feed, () -> {
            feedMode = !feedMode;
            theme.showSuccess(message, feedMode ? "Click a plant to feed it." : shortStatus());
        });
        panel.add(feed).width(280f).height(42f).padBottom(4f);
        panel.row();
        progress.setAlignment(Align.center);
        panel.add(progress).width(280f).height(44f).padBottom(4f);
        panel.row();
        TextButton back = theme.secondaryButton("Back to Mini Games");
        UiActions.onClick(back, app::returnToMiniGames);
        panel.add(back).width(255f).height(48f);
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
        board.add(sunLayer);
        board.add(new MiniGameGridInputActor(ROWS, COLS,
            cell -> clickCell(cell.row(), cell.column())));
        return board;
    }

    private void clickCell(int row, int column) {
        if (session.isFinished() || !zombotany.isBattleStarted()) {
            return;
        }
        if (zombotany.getSunViews().stream().anyMatch(sun -> sun.row() == row
            && sun.column() == column)) {
            execute("collect " + (column + 1) + " " + (row + 1));
        } else if (feedMode) {
            execute("feed " + (column + 1) + " " + (row + 1));
            feedMode = false;
        } else if (selectedPlant != null) {
            execute("plant " + commandPlantName(selectedPlant) + " "
                + (column + 1) + " " + (row + 1));
        }
    }

    private void rebuildPlantTray() {
        plantTray.clearChildren();
        List<String> selected = zombotany.getSelectedPlantViews();
        for (String plant : PLANTS) {
            boolean active = selected.contains(plant);
            TextButton button = theme.primaryButton((active ? "REMOVE " : "ADD ") + plant);
            button.getLabel().setFontScale(0.52f);
            button.setDisabled(zombotany.isBattleStarted());
            UiActions.onClick(button, () -> {
                if (active) {
                    execute("remove " + commandPlantName(plant));
                    if (plant.equals(selectedPlant)) {
                        selectedPlant = null;
                    }
                } else {
                    execute("select " + commandPlantName(plant));
                    selectedPlant = plant;
                }
            });
            plantTray.add(button).width(280f).height(29f).padBottom(2f);
            plantTray.row();
        }
    }

    private String commandPlantName(String plant) {
        return PlantDefinition.normalizeKey(plant);
    }

    private void rebuildSuns() {
        sunLayer.clearChildren();
        float cellWidth = BOARD_WIDTH / COLS;
        float cellHeight = BOARD_HEIGHT / ROWS;
        for (ZombotanySession.SunView sun : zombotany.getSunViews()) {
            Image image = theme.image("IMAGE_UI_HUD_INGAME_SUN");
            if (image == null) {
                continue;
            }
            image.setScaling(Scaling.fit);
            float size = Math.min(cellWidth, cellHeight) * 0.68f;
            image.setBounds(sun.column() * cellWidth + (cellWidth - size) * 0.5f,
                (ROWS - 1 - sun.row()) * cellHeight + (cellHeight - size) * 0.5f,
                size, size);
            sunLayer.addActor(image);
        }
    }

    @Override
    protected void refreshFromSession() {
        units.setPlants(zombotany.getPlantViews());
        units.setZombies(zombotany.getZombieViews());
        sunLabel.setText(Integer.toString(zombotany.getSun()));
        rebuildPlantTray();
        rebuildSuns();
        progress.setText("Kills " + zombotany.getKills() + " / " + session.getTarget()
            + "   |   " + shortStatus());
        startButton.setVisible(!zombotany.isBattleStarted());
    }
}
