package view;

public class MainMenuView {
    public void showOptions() {
        System.out.println("Game, Settings, News, Profile, and Leaderboard are available.");
    }

    public void showWalletInfo(int coins, int gems) {
        System.out.println("Coins: " + coins + ", Gems: " + gems);
    }
}
