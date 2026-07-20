package controller;

import model.GameProgress;
import model.LeaderboardEntry;
import model.User;
import model.UserRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class LeaderboardController {
    private final UserRepository repository;

    public LeaderboardController(UserRepository repository) {
        this.repository = repository;
    }

    public List<LeaderboardEntry> getLeaderboard(String column, String order) {
        ArrayList<LeaderboardEntry> entries = new ArrayList<>();
        for (User user : repository.getAllUsers()) {
            GameProgress progress = user.getProgress();
            entries.add(new LeaderboardEntry(user.getUsername(),
                progress.getLastChapterNumber(), progress.getLastLevelNumber(),
                progress.getCompletedMiniGames(), progress.getCompletedDailyQuests(),
                progress.getCompletedOtherQuests(), progress.getBestMeowPoints()));
        }
        Comparator<LeaderboardEntry> comparator = comparator(column);
        if ("desc".equalsIgnoreCase(order)) {
            comparator = comparator.reversed();
        }
        entries.sort(comparator.thenComparing(LeaderboardEntry::username));
        return List.copyOf(entries);
    }

    private Comparator<LeaderboardEntry> comparator(String column) {
        String normalized = column == null ? "score" : column.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "username" -> Comparator.comparing(LeaderboardEntry::username);
            case "chapter" -> Comparator.comparingInt(LeaderboardEntry::chapter)
                .thenComparingInt(LeaderboardEntry::level);
            case "level" -> Comparator.comparingInt(LeaderboardEntry::level);
            case "minigames" -> Comparator.comparingInt(LeaderboardEntry::miniGames);
            case "daily", "dailyquests" -> Comparator.comparingInt(LeaderboardEntry::dailyQuests);
            case "quests", "otherquests" -> Comparator.comparingInt(LeaderboardEntry::otherQuests);
            case "score", "meowpoints" -> Comparator.comparingInt(LeaderboardEntry::bestScore);
            default -> throw new IllegalArgumentException("Unknown leaderboard column.");
        };
    }
}
