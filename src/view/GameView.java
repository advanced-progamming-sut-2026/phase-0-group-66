package view;

import model.Board;

public class GameView {
    public void showMap(Board board) {
        if (board == null) {
            showMessage("No board is available.");
        } else {
            System.out.print(board.render());
        }
    }

    public void showPlantStatus() {
        showMessage("Plant status is available through the game controller.");
    }

    public void showTileStatus(int row, int col) {
        showMessage("Tile status requested for (" + col + ", " + row + ").");
    }

    public void showWaveInfo(int waveNumber) {
        showMessage("Wave " + waveNumber + ".");
    }

    public void showGameOver(boolean win) {
        showMessage(win ? "You won the level." : "You lost the level.");
    }

    public void showMessage(String message) {
        System.out.println(message);
    }

    public void showText(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        System.out.print(text);
        if (!text.endsWith(System.lineSeparator())) {
            System.out.println();
        }
    }

    public void showGameSummary(String summary) {
        showMessage(summary);
    }
}
