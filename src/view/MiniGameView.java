package view;

import java.util.List;

public class MiniGameView {
    public void showMiniGames(List<String> miniGames) {
        miniGames.forEach(System.out::println);
    }

    public void showMessage(String message) {
        System.out.println(message);
    }
}
