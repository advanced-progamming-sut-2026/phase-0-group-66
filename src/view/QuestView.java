package view;

import java.util.List;

public class QuestView {
    public void showQuests(List<String> quests) {
        if (quests.isEmpty()) {
            System.out.println("No quests.");
            return;
        }
        quests.forEach(System.out::println);
    }

    public void showMessage(String message) {
        System.out.println(message);
    }
}
