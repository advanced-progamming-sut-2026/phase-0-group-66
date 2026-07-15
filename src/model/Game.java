package model;

public class Game {
    private String gameState;
    private Chapter currentChapter;
    private Level currentLevel;
    private Board board;
    private Wave currentWave;
    private int sunAmount;
    private int elapsedTicks;

    public void startGame(Level level) {
    }

    public void advanceTime(int ticks) {
    }

    public void startNextWave() {
    }

    public void plant(Plant plant, int row, int col) {
    }

    public void collectSun(int row, int col) {
    }

    public boolean checkWinCondition() {
        return false;
    }

    public boolean checkLoseCondition() {
        return false;
    }
}
