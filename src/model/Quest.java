package model;

public class Quest {
    private final QuestDefinition definition;
    private final int targetProgress;
    private int progress;
    private boolean completed;
    private boolean rewardClaimed;

    public Quest(QuestDefinition definition) {
        this(definition, definition == null ? 1 : definition.inferDefaultTarget());
    }

    public Quest(QuestDefinition definition, int targetProgress) {
        if (definition == null) {
            throw new IllegalArgumentException("Quest definition cannot be null.");
        }
        if (targetProgress <= 0) {
            throw new IllegalArgumentException("Quest target must be positive.");
        }
        this.definition = definition;
        this.targetProgress = targetProgress;
    }

    public void updateProgress(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Quest progress cannot be negative.");
        }
        progress = Math.min(targetProgress, progress + value);
        completed = progress >= targetProgress;
    }

    public boolean isCompleted() {
        return completed;
    }

    public boolean claimReward() {
        if (!completed || rewardClaimed) {
            return false;
        }
        rewardClaimed = true;
        return true;
    }

    public QuestDefinition getDefinition() {
        return definition;
    }

    public String getTitle() {
        return definition.getTitle();
    }

    public int getProgress() {
        return progress;
    }

    public int getTargetProgress() {
        return targetProgress;
    }

    public boolean isRewardClaimed() {
        return rewardClaimed;
    }
}
