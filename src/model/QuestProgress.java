package model;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class QuestProgress implements Serializable {
    private static final long serialVersionUID = 1L;

    private int progress;
    private boolean rewardClaimed;
    private LinkedHashMap<String, Integer> bucketProgress = new LinkedHashMap<>();

    public int getProgress() { return progress; }
    public boolean isRewardClaimed() { return rewardClaimed; }
    public Map<String, Integer> getBucketProgress() { return Map.copyOf(buckets()); }

    public void addProgress(int amount, int target) {
        if (amount > 0 && !rewardClaimed) {
            progress = Math.min(target, progress + amount);
        }
    }

    public void updateMaximum(int value, int target) {
        if (!rewardClaimed) {
            progress = Math.max(progress, Math.min(target, Math.max(0, value)));
        }
    }

    public void addBucketProgress(String bucket, int amount, int target) {
        if (amount <= 0 || rewardClaimed) {
            return;
        }
        String key = normalizeBucket(bucket);
        int value = Math.min(target, buckets().getOrDefault(key, 0) + amount);
        buckets().put(key, value);
        updateMaximum(value, target);
    }

    public void updateBucketMaximum(String bucket, int value, int target) {
        if (rewardClaimed) {
            return;
        }
        String key = normalizeBucket(bucket);
        int bounded = Math.min(target, Math.max(0, value));
        int stored = Math.max(buckets().getOrDefault(key, 0), bounded);
        buckets().put(key, stored);
        updateMaximum(stored, target);
    }

    public void resetProgress() {
        if (!rewardClaimed) {
            progress = 0;
            buckets().clear();
        }
    }

    public boolean isCompleted(int target) { return progress >= target; }

    public void claim() { rewardClaimed = true; }

    private LinkedHashMap<String, Integer> buckets() {
        if (bucketProgress == null) {
            bucketProgress = new LinkedHashMap<>();
        }
        return bucketProgress;
    }

    private String normalizeBucket(String value) {
        String normalized = PlantDefinition.normalizeKey(value);
        return normalized.isEmpty() ? "default" : normalized;
    }
}
