package pvz.ui;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.utils.Scaling;
import model.VasebreakerSession;
import pvz.PvzApplication;

import java.util.List;
import java.util.function.Consumer;

/**
 * Fixed-grid Vasebreaker board. Model rows/columns are zero-based; only this actor
 * converts them to the screen's bottom-left coordinate system.
 */
public final class VasebreakerBoardActor extends WidgetGroup {
    public static final int ROWS = 5;
    public static final int COLS = 9;

    private static final String NORMAL_VASE =
        "IMAGE_EGGBREAKER_VASE_EGG_BROWN_VASE_EGG_BROWN_151X198";
    private static final String PLANT_VASE =
        "IMAGE_EGGBREAKER_VASE_EGG_GREEN_VASE_EGG_GREEN_151X198";
    private static final String GIANT_VASE =
        "IMAGE_EGGBREAKER_VASE_EGG_GARGANTUAR_VASE_EGG_GARGANTUAR_151X198";

    private final PvzApplication app;
    private final Consumer<Cell> cellClick;
    private final Image background;
    private final MiniGameUnitLayer units;
    private final Group vaseLayer = new Group();
    private final Group gridLayer = new Group();

    public VasebreakerBoardActor(PvzApplication app, Consumer<Cell> cellClick) {
        this.app = app;
        this.cellClick = cellClick;
        background = app.assets().uiTheme().image("IMAGE_BACKGROUNDS_EGYPT_TEXTURE");
        units = new MiniGameUnitLayer(app);
        setTouchable(Touchable.enabled);
        vaseLayer.setTouchable(Touchable.disabled);
        gridLayer.setTouchable(Touchable.disabled);
        units.setTouchable(Touchable.disabled);
        if (background != null) {
            background.setScaling(Scaling.stretch);
            background.setTouchable(Touchable.disabled);
            addActor(background);
        }
        addActor(gridLayer);
        addActor(units);
        addActor(vaseLayer);
        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y,
                                     int pointer, int button) {
                if (x < 0f || y < 0f || x >= getWidth() || y >= getHeight()) {
                    return false;
                }
                int column = Math.min(COLS - 1, (int) (x / cellWidth()));
                int row = ROWS - 1 - Math.min(ROWS - 1, (int) (y / cellHeight()));
                if (cellClick != null) {
                    cellClick.accept(new Cell(column, row));
                }
                return true;
            }
        });
        rebuildGrid();
    }

    public void setState(List<VasebreakerSession.VaseView> vases,
                         List<model.MiniGamePlantSnapshot> plants,
                         List<model.MiniGameUnitSnapshot> zombies) {
        units.setPlants(plants);
        units.setZombies(zombies);
        vaseLayer.clearChildren();
        if (vases != null) {
            for (VasebreakerSession.VaseView vase : vases) {
                Image image = app.assets().uiTheme().image(vaseImage(vase.kind()));
                if (image == null) {
                    continue;
                }
                image.setScaling(Scaling.fit);
                image.setTouchable(Touchable.disabled);
                image.setUserObject(vase);
                vaseLayer.addActor(image);
            }
        }
        invalidateHierarchy();
    }

    @Override
    public void layout() {
        float width = getWidth();
        float height = getHeight();
        if (background != null) {
            background.setBounds(0f, 0f, width, height);
        }
        gridLayer.setBounds(0f, 0f, width, height);
        units.setBounds(0f, 0f, width, height);
        vaseLayer.setBounds(0f, 0f, width, height);

        float cellWidth = cellWidth();
        float cellHeight = cellHeight();
        for (var actor : gridLayer.getChildren()) {
            Object value = actor.getUserObject();
            if (!(value instanceof float[] position)) {
                continue;
            }
            if (position[1] == 0f) {
                actor.setBounds(position[0] * cellWidth - 1f, 0f, 2f, height);
            } else {
                actor.setBounds(0f, position[1] * cellHeight - 1f, width, 2f);
            }
        }
        for (var actor : vaseLayer.getChildren()) {
            Object value = actor.getUserObject();
            if (!(value instanceof VasebreakerSession.VaseView vase)) {
                continue;
            }
            float vaseWidth = cellWidth * 0.82f;
            float vaseHeight = cellHeight * 0.92f;
            actor.setBounds(
                vase.column() * cellWidth + (cellWidth - vaseWidth) * 0.5f,
                (ROWS - 1 - vase.row()) * cellHeight + (cellHeight - vaseHeight) * 0.5f,
                vaseWidth,
                vaseHeight
            );
        }
    }

    private void rebuildGrid() {
        gridLayer.clearChildren();
        Image first = app.assets().uiTheme().image(UiTheme.DIVIDER);
        if (first == null) {
            return;
        }
        first.setColor(1f, 0.92f, 0.45f, 0.20f);
        first.setTouchable(Touchable.disabled);
        first.setUserObject(new float[] {1f, 0f});
        gridLayer.addActor(first);
        for (int column = 1; column < COLS; column++) {
            if (column == 1) {
                continue;
            }
            Image line = app.assets().uiTheme().image(UiTheme.DIVIDER);
            line.setColor(1f, 0.92f, 0.45f, 0.20f);
            line.setTouchable(Touchable.disabled);
            line.setUserObject(new float[] {column, 0f});
            gridLayer.addActor(line);
        }
        for (int row = 1; row < ROWS; row++) {
            Image line = app.assets().uiTheme().image(UiTheme.DIVIDER);
            line.setColor(1f, 0.92f, 0.45f, 0.20f);
            line.setTouchable(Touchable.disabled);
            line.setUserObject(new float[] {0f, row});
            gridLayer.addActor(line);
        }
    }

    private float cellWidth() {
        return getWidth() / COLS;
    }

    private float cellHeight() {
        return getHeight() / ROWS;
    }

    private String vaseImage(String kind) {
        return switch (kind == null ? "" : kind) {
            case "PLANT" -> PLANT_VASE;
            case "GIANT" -> GIANT_VASE;
            default -> NORMAL_VASE;
        };
    }

    public record Cell(int column, int row) { }
}
