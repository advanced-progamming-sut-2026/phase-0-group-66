package pvz.ui;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;

import java.util.function.Consumer;

/** Maps visible top-to-bottom cells to the model's bottom-to-top row indexes. */
public final class MiniGameGridInputActor extends WidgetGroup {
    private final int rows;
    private final int columns;
    private final Consumer<Cell> cellClick;

    public MiniGameGridInputActor(int rows, int columns, Consumer<Cell> cellClick) {
        if (rows <= 0 || columns <= 0) {
            throw new IllegalArgumentException("Grid dimensions must be positive.");
        }
        this.rows = rows;
        this.columns = columns;
        this.cellClick = cellClick;
        setTouchable(Touchable.enabled);
        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y,
                                     int pointer, int button) {
                if (getWidth() <= 0f || getHeight() <= 0f
                    || x < 0f || y < 0f || x >= getWidth() || y >= getHeight()) {
                    return false;
                }
                int column = Math.min(columns - 1, (int) (x / cellWidth()));
                int visualRow = Math.min(rows - 1, (int) (y / cellHeight()));
                int row = rows - 1 - visualRow;
                if (cellClick != null) {
                    cellClick.accept(new Cell(row, column));
                }
                return true;
            }
        });
    }

    @Override
    public void layout() {
        // The actor is intentionally empty; its bounds are the complete input surface.
    }

    private float cellWidth() {
        return getWidth() / columns;
    }

    private float cellHeight() {
        return getHeight() / rows;
    }

    public record Cell(int row, int column) { }
}
