package model;

import java.io.Serializable;

public class GreenhouseSlot implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int x;
    private final int y;
    private boolean unlocked;
    private String plantName;
    private boolean marigold;
    private long plantedAtMillis;
    private long readyAtMillis;

    public GreenhouseSlot(int x, int y, boolean unlocked) {
        this.x = x;
        this.y = y;
        this.unlocked = unlocked;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public boolean isUnlocked() { return unlocked; }
    public boolean isEmpty() { return plantName == null; }
    public String getPlantName() { return plantName; }
    public boolean isMarigold() { return marigold; }
    public long getReadyAtMillis() { return readyAtMillis; }

    public boolean isReady(long nowMillis) {
        return !isEmpty() && nowMillis >= readyAtMillis;
    }

    public long remainingMillis(long nowMillis) {
        return isEmpty() ? 0 : Math.max(0, readyAtMillis - nowMillis);
    }

    public void unlock() { unlocked = true; }

    public void plant(String name, boolean isMarigold, long nowMillis, long growthMillis) {
        plantName = name;
        marigold = isMarigold;
        plantedAtMillis = nowMillis;
        readyAtMillis = nowMillis + growthMillis;
    }

    public void makeReady(long nowMillis) {
        if (!isEmpty()) {
            readyAtMillis = nowMillis;
        }
    }

    public void clear() {
        plantName = null;
        marigold = false;
        plantedAtMillis = 0;
        readyAtMillis = 0;
    }

    public String status(long nowMillis) {
        if (!unlocked) {
            return "LOCKED";
        }
        if (isEmpty()) {
            return "EMPTY";
        }
        if (isReady(nowMillis)) {
            return plantName + " READY";
        }
        long minutes = (remainingMillis(nowMillis) + 59_999L) / 60_000L;
        return plantName + " " + minutes + "m";
    }
}
