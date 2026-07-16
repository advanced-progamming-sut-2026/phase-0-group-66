package model;

import java.util.Locale;

public final class QuestDefinition {
    private final String title;
    private final String category;
    private final String completionCondition;
    private final String rewardDescription;
    private final String priority;
    private final String variables;

    public QuestDefinition(String title, String category, String completionCondition,
                           String rewardDescription, String priority, String variables) {
        this.title = requireText(title, "Quest title");
        this.category = requireText(category, "Quest category");
        this.completionCondition = requireText(completionCondition, "Quest condition");
        this.rewardDescription = requireText(rewardDescription, "Quest reward");
        this.priority = requireText(priority, "Quest priority");
        this.variables = variables == null ? "" : variables.trim();
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getCompletionCondition() {
        return completionCondition;
    }

    public String getRewardDescription() {
        return rewardDescription;
    }

    public String getPriority() {
        return priority;
    }

    public String getVariables() {
        return variables;
    }

    public int inferDefaultTarget() {
        Integer conditionTarget = firstNumber(completionCondition);
        if (conditionTarget != null) {
            return conditionTarget;
        }
        Integer variableTarget = firstNumber(variables);
        return variableTarget == null ? 1 : variableTarget;
    }

    public String getNormalizedTitle() {
        return normalize(title);
    }

    public boolean isInCategory(String requestedCategory) {
        return normalize(category).equals(normalize(requestedCategory));
    }

    @Override
    public String toString() {
        return title + " [category=" + category + ", priority=" + priority
            + ", reward=" + rewardDescription + "]";
    }


    private static Integer firstNumber(String text) {
        if (text == null) {
            return null;
        }
        StringBuilder digits = new StringBuilder();
        boolean started = false;
        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            if (Character.isDigit(current)) {
                digits.append(Character.getNumericValue(current));
                started = true;
            } else if (started) {
                break;
            }
        }
        return digits.length() == 0 ? null : Integer.parseInt(digits.toString());
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
