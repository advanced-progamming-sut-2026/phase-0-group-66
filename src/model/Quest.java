package model;

public class Quest {
    private String title;
    private String category;
    private String completionCondition;
    private String rewardType;
    private String priority;
    private String variable;
    private int progress;
    private boolean completed;

    public void updateProgress(int value) {
    }

    public boolean isCompleted() {
        return false;
    }

    public void claimReward() {
    }
}
