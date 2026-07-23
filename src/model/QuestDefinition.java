package model;

import java.util.Locale;

/** Immutable quest definition with a structured completion condition. */
public final class QuestDefinition {
    private final int id;
    private final String key;
    private final String title;
    private final QuestCategory category;
    private final String description;
    private final QuestCondition condition;
    private final RewardType rewardType;
    private final int rewardAmount;
    private final QuestPriority priority;

    public QuestDefinition(int id, String key, String title, QuestCategory category,
                           String description, QuestCondition condition,
                           RewardType rewardType, int rewardAmount, QuestPriority priority) {
        if (id <= 0 || rewardAmount < 0) {
            throw new IllegalArgumentException("Invalid quest numeric data.");
        }
        this.id = id;
        this.key = requireText(key, "Quest key");
        this.title = requireText(title, "Quest title");
        this.category = requireValue(category, "Quest category");
        this.description = requireText(description, "Quest description");
        this.condition = requireValue(condition, "Quest condition");
        this.rewardType = requireValue(rewardType, "Reward type");
        this.rewardAmount = rewardAmount;
        this.priority = requireValue(priority, "Quest priority");
    }

    public int getId() { return id; }
    public String getKey() { return key; }
    public String getTitle() { return title; }
    public QuestCategory getCategory() { return category; }
    public String getDescription() { return description; }
    public QuestCondition getCondition() { return condition; }
    public QuestEventType getEventType() { return condition.getEvent(); }
    public int getTarget() { return condition.getTarget(); }
    public RewardType getRewardType() { return rewardType; }
    public int getRewardAmount() { return rewardAmount; }
    public QuestPriority getPriority() { return priority; }
    public String getParameter() { return condition.getQualifier(); }
    public String getNormalizedTitle() { return normalize(title); }
    public String getNormalizedKey() { return normalize(key); }

    @Override
    public String toString() {
        return id + ". " + title + " [" + category + ", " + priority + "] "
            + description + " | reward=" + rewardAmount + " " + rewardType;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
            .replace(" ", "").replace("-", "").replace("_", "");
    }

    private static <T> T requireValue(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null.");
        }
        return value;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
        return value.trim();
    }
}
