package model;

import java.io.Serializable;

public final class BoardPosition implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int row;
    private final double column;

    public BoardPosition(int row, double column) {
        this.row = row;
        this.column = column;
    }

    public int getRow() {
        return row;
    }

    public double getColumn() {
        return column;
    }

    public BoardPosition moveHorizontal(double amount) {
        return new BoardPosition(row, column + amount);
    }

    public BoardPosition withRow(int newRow) {
        return new BoardPosition(newRow, column);
    }

    @Override
    public String toString() {
        return "(" + formatColumn(column + 1) + ", " + (row + 1) + ")";
    }

    private String formatColumn(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001) {
            return Long.toString(Math.round(value));
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value)
            .replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
