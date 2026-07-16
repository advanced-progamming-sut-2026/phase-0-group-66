package controller;

import model.QuestCategory;
import model.QuestDefinition;
import model.QuestFactory;

import java.util.List;
import java.util.Locale;

public class QuestController {
    private final QuestFactory questFactory;

    public QuestController(QuestFactory questFactory) {
        this.questFactory = questFactory;
    }

    public List<QuestDefinition> getQuestsPage(String pageName) {
        String requestedPage = normalizePage(pageName);

        if ("ALL".equals(requestedPage)) {
            return questFactory.getAllDefinitions();
        }

        QuestCategory requestedCategory;

        try {
            requestedCategory = QuestCategory.valueOf(requestedPage);
        } catch (IllegalArgumentException exception) {
            return List.of();
        }

        return questFactory.getAllDefinitions()
                .stream()
                .filter(quest -> quest.getCategory() == requestedCategory)
                .toList();
    }

    public QuestFactory getQuestFactory() {
        return questFactory;
    }

    private String normalizePage(String pageName) {
        if (pageName == null) {
            return "";
        }

        return pageName.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
    }
}