package model;

public record LeaderboardEntry(String username, int chapter, int level, int miniGames,
                               int dailyQuests, int otherQuests, int bestScore) {
    @Override
    public String toString() {
        return username + " | chapter=" + chapter + ", level=" + level
            + ", minigames=" + miniGames + ", dailyQuests=" + dailyQuests
            + ", otherQuests=" + otherQuests + ", bestScore=" + bestScore;
    }
}
