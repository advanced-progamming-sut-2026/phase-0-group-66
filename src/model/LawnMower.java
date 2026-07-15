package model;

public class LawnMower {
    private final int row;
    private boolean activated;

    public LawnMower(int row) {
        this.row = row;
    }

    public int getRow() {
        return row;
    }

    public boolean isActivated() {
        return activated;
    }

    public void trigger() {
        activated = true;
    }

    public void clearLane() {
        trigger();
    }
}
