package model;

import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Greenhouse implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int COLUMNS = 4;
    public static final int ROWS = 3;
    public static final int MAX_SLOTS = COLUMNS * ROWS;
    public static final long MARIGOLD_GROWTH_MILLIS = 2L * 60L * 60L * 1000L;
    public static final long PLANT_GROWTH_MILLIS = 8L * 60L * 60L * 1000L;

    private final ArrayList<GreenhouseSlot> slots = new ArrayList<>();

    public Greenhouse() {
        for (int y = 1; y <= ROWS; y++) {
            for (int x = 1; x <= COLUMNS; x++) {
                slots.add(new GreenhouseSlot(x, y, y == 1));
            }
        }
    }

    public GreenhouseSlot getSlot(int x, int y) {
        validate(x, y);
        return slots.get((y - 1) * COLUMNS + (x - 1));
    }

    public List<GreenhouseSlot> getSlots() {
        return Collections.unmodifiableList(slots);
    }

    public int getUnlockedSlotCount() {
        return (int) slots.stream().filter(GreenhouseSlot::isUnlocked).count();
    }

    public boolean unlockNextSlot() {
        for (GreenhouseSlot slot : slots) {
            if (!slot.isUnlocked()) {
                slot.unlock();
                return true;
            }
        }
        return false;
    }

    public String render(long nowMillis) {
        StringBuilder output = new StringBuilder();
        for (int y = 1; y <= ROWS; y++) {
            for (int x = 1; x <= COLUMNS; x++) {
                output.append('[').append(x).append(',').append(y).append(':')
                    .append(getSlot(x, y).status(nowMillis)).append("] ");
            }
            output.append(System.lineSeparator());
        }
        return output.toString();
    }

    private void validate(int x, int y) {
        if (x < 1 || x > COLUMNS || y < 1 || y > ROWS) {
            throw new IllegalArgumentException("Greenhouse position is outside the 4x3 grid.");
        }
    }

    private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
        input.defaultReadObject();
        if (slots.size() <= MAX_SLOTS) {
            return;
        }

        ArrayList<GreenhouseSlot> legacySlots = new ArrayList<>(slots);
        slots.clear();
        for (int y = 1; y <= ROWS; y++) {
            for (int x = 1; x <= COLUMNS; x++) {
                for (GreenhouseSlot slot : legacySlots) {
                    if (slot.getX() == x && slot.getY() == y) {
                        slots.add(slot);
                        break;
                    }
                }
            }
        }
    }
}
