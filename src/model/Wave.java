package model;

import java.util.ArrayList;
import java.util.List;

public class Wave {
    private int waveNumber;
    private int difficultyCost;
    private int delay;
    private final List<Zombie> zombies;
    private boolean started;

    public Wave() {
        zombies = new ArrayList<>();
    }

    public void startWave() {
        started = true;
    }

    public void spawnZombies() {
    }

    public boolean isFinished() {
        if (!started) {
            return false;
        }
        for (Zombie zombie : zombies) {
            if (zombie.health > 0) {
                return false;
            }
        }
        return true;
    }
}
