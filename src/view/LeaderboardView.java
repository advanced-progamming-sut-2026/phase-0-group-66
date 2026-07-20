package view;

import model.LeaderboardEntry;

import java.util.List;

public class LeaderboardView {
    public void showLeaderboard(List<LeaderboardEntry> entries) {
        if (entries.isEmpty()) {
            System.out.println("No leaderboard entries.");
            return;
        }
        int rank = 1;
        for (LeaderboardEntry entry : entries) {
            System.out.println(rank++ + ". " + entry);
        }
    }
}
