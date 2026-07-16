package view;

import model.QuestDefinition;

import java.util.List;

public class QuestView {
    public void showQuestDefinitions(List<QuestDefinition> quests) {
        System.out.println("Quests (" + quests.size() + "):");
        if (quests.isEmpty()) {
            System.out.println("- none");
            return;
        }
        for (QuestDefinition quest : quests) {
            System.out.println("- " + quest.getTitle());
            System.out.println("  category: " + quest.getCategory());
            System.out.println("  priority: " + quest.getPriority());
            System.out.println("  condition: " + quest.getCompletionCondition());
            System.out.println("  reward: " + quest.getRewardDescription());
            if (!quest.getVariables().isBlank()) {
                System.out.println("  variables: " + quest.getVariables());
            }
        }
    }

    public void showMessage(String message) {
        System.out.println(message);
    }
}
