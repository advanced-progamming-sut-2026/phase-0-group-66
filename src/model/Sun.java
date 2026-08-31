package model;

public class Sun {
    public static final int SKY_FALL_TICKS = 5 * Game.TICKS_PER_SECOND;

    private int amount;
    private final GridPosition position;
    private SunType type;
    private int remainingFallTicks;
    private boolean collected;
    private boolean falling;

    public Sun() {
        this(50, new GridPosition(0, 0), SunType.NORMAL, false);
    }

    public Sun(int amount, GridPosition position) {
        this(amount, position, SunType.PLANT_GENERATED, false);
    }

    public Sun(int amount, GridPosition position, SunType type, boolean falling) {
        if (amount < 0 || position == null) {
            throw new IllegalArgumentException("Invalid sun data.");
        }
        this.amount = amount;
        this.position = position;
        this.type = type == null ? SunType.NORMAL : type;
        this.falling = falling;
        this.remainingFallTicks = falling ? SKY_FALL_TICKS : 0;
    }

    public static Sun falling(SunType type, GridPosition position) {
        SunType actualType = type == null ? SunType.NORMAL : type;
        return new Sun(actualType.getDefaultAmount(), position, actualType, true);
    }

    public int collect() {
        if (collected) {
            return 0;
        }
        collected = true;
        return amount;
    }

    public boolean tick() {
        if (!falling || collected) {
            return false;
        }
        remainingFallTicks--;
        if (remainingFallTicks <= 0) {
            falling = false;
            return true;
        }
        return false;
    }

    public void expire() {
        collected = true;
        remainingFallTicks = 0;
    }

    public int getAmount() {
        return amount;
    }

    public GridPosition getPosition() {
        return position;
    }

    public SunType getType() {
        return type;
    }

    public int getRemainingFallTicks() {
        return remainingFallTicks;
    }

    public boolean isCollected() {
        return collected;
    }

    public boolean isFalling() {
        return falling;
    }

    public boolean isPlantGenerated() {
        return type == SunType.PLANT_GENERATED;
    }
}
