package model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.LinkedHashSet;

public class QuestLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private final LinkedHashMap<Integer, QuestProgress> progressByQuest = new LinkedHashMap<>();
    private final LinkedHashSet<Integer> dailyQuestIds = new LinkedHashSet<>();
    private String dailyDate = LocalDate.now().toString();

    public QuestProgress getProgress(QuestDefinition definition) {
        resetDailyIfNeeded();
        if (definition.getCategory() == QuestCategory.DAILY) {
            dailyQuestIds.add(definition.getId());
        }
        return progressByQuest.computeIfAbsent(definition.getId(), ignored -> new QuestProgress());
    }

    public Map<Integer, QuestProgress> getAllProgress() {
        return Map.copyOf(progressByQuest);
    }

    private void resetDailyIfNeeded() {
        String today = LocalDate.now().toString();
        if (!today.equals(dailyDate)) {
            for (Integer questId : dailyQuestIds) {
                progressByQuest.remove(questId);
            }
            dailyDate = today;
        }
    }
}
