package model;

import java.io.Serializable;
import java.util.Objects;

public final class GridPosition implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int row;
    private final int column;

    public GridPosition(int row, int column) {
        this.row = row;
        this.column = column;
    }

    public static GridPosition fromCommandCoordinates(int x, int y) {
        return new GridPosition(y, x);
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GridPosition)) {
            return false;
        }
        GridPosition position = (GridPosition) other;
        return row == position.row && column == position.column;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, column);
    }

    @Override
    public String toString() {
        return "(" + column + ", " + row + ")";
    }
}
