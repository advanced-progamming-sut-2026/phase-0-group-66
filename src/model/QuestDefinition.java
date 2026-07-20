package model;

import java.util.Locale;

public final class QuestDefinition {
    private final int id;
    private final String title;
    private final QuestCategory category;
    private final String description;
    private final QuestEventType eventType;
    private final int target;
    private final RewardType rewardType;
    private final int rewardAmount;
    private final QuestPriority priority;
    private final String parameter;

    public QuestDefinition(int id, String title, QuestCategory category, String description,
                           QuestEventType eventType, int target, RewardType rewardType,
                           int rewardAmount, QuestPriority priority, String parameter) {
        if (id <= 0 || target <= 0 || rewardAmount < 0) {
            throw new IllegalArgumentException("Invalid quest numeric data.");
        }
        this.id = id;
        this.title = requireText(title, "Quest title");
        this.category = category;
        this.description = requireText(description, "Quest description");
        this.eventType = eventType;
        this.target = target;
        this.rewardType = rewardType;
        this.rewardAmount = rewardAmount;
        this.priority = priority;
        this.parameter = parameter == null ? "" : parameter.trim();
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public QuestCategory getCategory() { return category; }
    public String getDescription() { return description; }
    public QuestEventType getEventType() { return eventType; }
    public int getTarget() { return target; }
    public RewardType getRewardType() { return rewardType; }
    public int getRewardAmount() { return rewardAmount; }
    public QuestPriority getPriority() { return priority; }
    public String getParameter() { return parameter; }
    public String getNormalizedTitle() { return normalize(title); }

    @Override
    public String toString() {
        return id + ". " + title + " [" + category + ", " + priority + "] "
            + description + " | reward=" + rewardAmount + " " + rewardType;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
            .replace(" ", "").replace("-", "").replace("_", "");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
        return value.trim();
    }
}
