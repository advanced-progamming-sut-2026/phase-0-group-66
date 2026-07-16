package model;

public enum SunType {
    NORMAL(25),
    SPECIAL(100),
    RADIOACTIVE(0),
    PLANT_GENERATED(0);

    private final int defaultAmount;

    SunType(int defaultAmount) {
        this.defaultAmount = defaultAmount;
    }

    public int getDefaultAmount() {
        return defaultAmount;
    }
}
