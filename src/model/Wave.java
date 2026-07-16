package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Wave {
    private int waveNumber;
    private int difficultyCost;
    private int delay;
    private final List<Zombie> zombies;
    private boolean started;
    private int initialEffectiveHealth;

    public Wave() {
        this(1, 1000, 0);
    }

    public Wave(int waveNumber, int difficultyCost, int delay) {
        if (waveNumber <= 0 || difficultyCost < 1000 || delay < 0) {
            throw new IllegalArgumentException("Invalid wave settings.");
        }
        this.waveNumber = waveNumber;
        this.difficultyCost = difficultyCost;
        this.delay = delay;
        this.zombies = new ArrayList<>();
    }

    public void startWave() {
        started = true;
        initialEffectiveHealth = getRemainingEffectiveHealth();
    }

    public void spawnZombies() {
    }

    public void populate(ZombieFactory zombieFactory, int rows, double spawnColumn, Random random) {
        if (started || !zombies.isEmpty()) {
            throw new IllegalStateException("Wave is already populated.");
        }
        List<ZombieDefinition> definitions = zombieFactory.getAllDefinitions().stream()
            .filter(definition -> definition.getWavePointCost() > 0
                && definition.getWavePointCost() <= difficultyCost)
            .toList();
        if (definitions.isEmpty()) {
            throw new IllegalStateException("No zombie can be used for this wave.");
        }
        List<ZombieDefinition> selected = chooseExactCost(definitions, difficultyCost, random);
        for (ZombieDefinition definition : selected) {
            Zombie zombie = zombieFactory.createZombie(definition.getAlias());
            zombie.setPosition(new BoardPosition(random.nextInt(rows), spawnColumn));
            zombies.add(zombie);
        }
    }

    public boolean isFinished() {
        if (!started) {
            return false;
        }
        for (Zombie zombie : zombies) {
            if (!zombie.isDead()) {
                return false;
            }
        }
        return true;
    }

    public boolean hasLostAtLeastSeventyFivePercentHealth() {
        if (!started || initialEffectiveHealth <= 0) {
            return false;
        }
        return getRemainingEffectiveHealth() * 4 <= initialEffectiveHealth;
    }

    public int getRemainingEffectiveHealth() {
        int result = 0;
        for (Zombie zombie : zombies) {
            if (!zombie.isDead()) {
                result += zombie.getEffectiveHealth();
            }
        }
        return result;
    }

    public int getWaveNumber() {
        return waveNumber;
    }

    public int getDifficultyCost() {
        return difficultyCost;
    }

    public int getDelay() {
        return delay;
    }

    public boolean isStarted() {
        return started;
    }

    public List<Zombie> getZombies() {
        return Collections.unmodifiableList(zombies);
    }

    private List<ZombieDefinition> chooseExactCost(List<ZombieDefinition> definitions,
                                                    int target, Random random) {
        boolean[] reachable = new boolean[target + 1];
        int[] previousCost = new int[target + 1];
        int[] previousDefinition = new int[target + 1];
        reachable[0] = true;
        List<ZombieDefinition> shuffled = new ArrayList<>(definitions);
        Collections.shuffle(shuffled, random);
        for (int cost = 0; cost <= target; cost++) {
            if (!reachable[cost]) {
                continue;
            }
            for (int index = 0; index < shuffled.size(); index++) {
                int next = cost + shuffled.get(index).getWavePointCost();
                if (next <= target && !reachable[next]) {
                    reachable[next] = true;
                    previousCost[next] = cost;
                    previousDefinition[next] = index;
                }
            }
        }
        if (!reachable[target]) {
            throw new IllegalStateException("Wave cost cannot be built exactly: " + target);
        }
        ArrayList<ZombieDefinition> result = new ArrayList<>();
        int current = target;
        while (current > 0) {
            int definitionIndex = previousDefinition[current];
            result.add(shuffled.get(definitionIndex));
            current = previousCost[current];
        }
        Collections.shuffle(result, random);
        return result;
    }
}
