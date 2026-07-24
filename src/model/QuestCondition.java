package model;

public final class QuestCondition {
    private final QuestEventType event;
    private final int target;
    private final String qualifier;

    public QuestCondition(QuestEventType event, int target, String qualifier) {
        if (event == null) {
            throw new IllegalArgumentException("Quest event cannot be null.");
        }
        if (target <= 0) {
            throw new IllegalArgumentException("Quest target must be positive.");
        }
        this.event = event;
        this.target = target;
        this.qualifier = qualifier == null ? "" : qualifier.trim();
    }

    public QuestEventType getEvent() { return event; }
    public int getTarget() { return target; }
    public String getQualifier() { return qualifier; }
}
