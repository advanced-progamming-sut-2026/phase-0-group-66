package model;

import java.io.Serializable;

public class QuestProgress implements Serializable {
    private static final long serialVersionUID = 1L;

    private int progress;
    private boolean rewardClaimed;

    public int getProgress() { return progress; }
    public boolean isRewardClaimed() { return rewardClaimed; }

    public void addProgress(int amount, int target) {
        if (amount > 0 && !rewardClaimed) {
            progress = Math.min(target, progress + amount);
        }
    }

    public boolean isCompleted(int target) {
        return progress >= target;
    }

    public void claim() {
        rewardClaimed = true;
    }
}
