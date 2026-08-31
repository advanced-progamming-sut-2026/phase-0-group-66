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
    private final Consumer<Cell> cellHover;

    public MiniGameGridInputActor(int rows, int columns, Consumer<Cell> cellClick) {
        this(rows, columns, cellClick, null);
    }

    public MiniGameGridInputActor(int rows, int columns, Consumer<Cell> cellClick,
                                  Consumer<Cell> cellHover) {
        if (rows <= 0 || columns <= 0) {
            throw new IllegalArgumentException("Grid dimensions must be positive.");
        }
        this.rows = rows;
        this.columns = columns;
        this.cellClick = cellClick;
        this.cellHover = cellHover;
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

            @Override
            public boolean mouseMoved(InputEvent event, float x, float y) {
                if (cellHover != null && x >= 0f && y >= 0f
                    && x < getWidth() && y < getHeight()) {
                    cellHover.accept(cellAt(x, y));
                    return true;
                }
                return false;
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer,
                             com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                if (cellHover != null) {
                    cellHover.accept(null);
                }
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

    private Cell cellAt(float x, float y) {
        int column = Math.min(columns - 1, (int) (x / cellWidth()));
        int visualRow = Math.min(rows - 1, (int) (y / cellHeight()));
        return new Cell(rows - 1 - visualRow, column);
    }

    public record Cell(int row, int column) { }
}
