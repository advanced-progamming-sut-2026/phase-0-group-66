package view;

import model.User;

public class ProfileView {
    public void showProfileInfo(User user) {
        System.out.println("username: " + user.getUsername());
        System.out.println("nickname: " + user.getNickname());
        System.out.println("email: " + user.getEmail());
        System.out.println("gender: " + user.getGender());
        System.out.println("difficulty: " + user.getDifficultyLevel());
        System.out.println("games played: " + user.getProgress().getGamesPlayed());
        System.out.println("coins: " + user.getWallet().getCoins());
        System.out.println("gems: " + user.getWallet().getGems());
        System.out.println("completed levels: " + user.getProgress().getCompletedLevels());
        System.out.println("best meow points: " + user.getProgress().getBestMeowPoints());
    }

    public void showEditResult(boolean success) {
        System.out.println(success ? "Profile updated successfully." : "Profile update failed.");
    }

    public void showMessage(String message) {
        System.out.println(message);
    }
}
