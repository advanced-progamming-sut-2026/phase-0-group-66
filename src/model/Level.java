package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Level {
    private String levelId;
    private String levelType;
    private int allowedPlantCount;
    private final List<Wave> waves;
    private boolean completed;

    public Level() {
        waves = new ArrayList<>();
    }

    public String getLevelId() {
        return levelId == null ? "unknown-level" : levelId;
    }

    public boolean isCompleted() {
        return completed;
    }

    public List<Wave> getWaves() {
        return Collections.unmodifiableList(waves);
    }

    public void loadLevel() {
    }

    public void startLevel() {
    }

    public void completeLevel() {
        completed = true;
    }
}
