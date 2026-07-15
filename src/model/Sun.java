package model;

public class Sun {
    private int amount;
    private GridPosition position;
    private int remainingTime;
    private boolean collected;

    public int collect() {
        if (collected) {
            return 0;
        }
        collected = true;
        return amount;
    }

    public void expire() {
        remainingTime = 0;
    }

    public GridPosition getPosition() {
        return position;
    }
}
