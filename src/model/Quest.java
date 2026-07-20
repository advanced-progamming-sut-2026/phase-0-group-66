package model;

public class Quest {
    private final QuestDefinition definition;
    private final QuestProgress progress;

    public Quest(QuestDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Quest definition cannot be null.");
        }
        this.definition = definition;
        this.progress = new QuestProgress();
    }

    public void updateProgress(int value) {
        progress.addProgress(value, definition.getTarget());
    }

    public boolean isCompleted() {
        return progress.isCompleted(definition.getTarget());
    }

    public boolean claimReward() {
        if (!isCompleted() || progress.isRewardClaimed()) {
            return false;
        }
        progress.claim();
        return true;
    }

    public QuestDefinition getDefinition() { return definition; }
    public String getTitle() { return definition.getTitle(); }
    public int getProgress() { return progress.getProgress(); }
    public int getTargetProgress() { return definition.getTarget(); }
    public boolean isRewardClaimed() { return progress.isRewardClaimed(); }
}
