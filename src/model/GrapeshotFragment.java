package model;

import java.util.HashSet;
import java.util.Set;

final class GrapeshotFragment {
    private static final double SPEED_TILES_PER_SECOND = 3.0;
    private static final double HIT_RADIUS = 0.48;

    private final int damage;
    private final String sourcePlant;
    private final Set<String> hitZombieIds;
    private double row;
    private double column;
    private double rowDirection;
    private double columnDirection;
    private int remainingTicks;
    private int remainingHits;

    GrapeshotFragment(double row, double column, double rowDirection,
                      double columnDirection, int damage, int lifetimeTicks,
                      int maximumHits, String sourcePlant) {
        if (damage < 0 || lifetimeTicks <= 0 || maximumHits <= 0) {
            throw new IllegalArgumentException("Invalid Grapeshot fragment data.");
        }
        this.row = row;
        this.column = column;
        this.rowDirection = normalizeDirection(rowDirection);
        this.columnDirection = normalizeDirection(columnDirection);
        this.damage = damage;
        this.remainingTicks = lifetimeTicks;
        this.remainingHits = maximumHits;
        this.sourcePlant = sourcePlant == null ? "" : sourcePlant;
        this.hitZombieIds = new HashSet<>();
    }

    void tick(Board board) {
        if (!isActive()) {
            return;
        }
        double step = SPEED_TILES_PER_SECOND / Game.TICKS_PER_SECOND;
        row += rowDirection * step;
        column += columnDirection * step;
        bounceAtEdges(board);
        damageCollidingZombies(board);
        remainingTicks--;
    }

    private void bounceAtEdges(Board board) {
        double maxRow = board.getRows() - 1.0;
        double maxColumn = board.getCols() - 0.001;
        if (row < 0.0) {
            row = -row;
            rowDirection = Math.abs(rowDirection);
        } else if (row > maxRow) {
            row = maxRow - (row - maxRow);
            rowDirection = -Math.abs(rowDirection);
        }
        if (column < 0.0) {
            column = -column;
            columnDirection = Math.abs(columnDirection);
        } else if (column > maxColumn) {
            column = maxColumn - (column - maxColumn);
            columnDirection = -Math.abs(columnDirection);
        }
    }

    private void damageCollidingZombies(Board board) {
        for (Zombie zombie : board.getZombies()) {
            if (zombie.isDead() || zombie.isHypnotized() || zombie.getPosition() == null
                || hitZombieIds.contains(zombie.getRuntimeId())) {
                continue;
            }
            double rowDistance = Math.abs(zombie.getPosition().getRow() - row);
            double columnDistance = Math.abs(zombie.getPosition().getColumn() - column);
            if (rowDistance <= HIT_RADIUS && columnDistance <= HIT_RADIUS) {
                zombie.takeDamage(damage, sourcePlant);
                hitZombieIds.add(zombie.getRuntimeId());
                remainingHits--;
            }
        }
    }

    boolean isActive() {
        return remainingTicks > 0 && remainingHits > 0;
    }

    int getRemainingTicks() {
        return remainingTicks;
    }

    double getRow() {
        return row;
    }

    double getColumn() {
        return column;
    }

    private static double normalizeDirection(double value) {
        if (value > 0.0) {
            return 1.0;
        }
        if (value < 0.0) {
            return -1.0;
        }
        return 0.0;
    }
}
