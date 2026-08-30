package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import controller.ActionResult;
import model.QuestCategory;
import model.QuestDefinition;
import model.QuestPriority;
import model.QuestProgress;
import pvz.PvzApplication;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class QuestScreen extends AuthenticatedUiScreen {
    private final Label status;
    private String category = "ALL";

    public QuestScreen(PvzApplication app) {
        super(app);
        status = statusLabel();
        buildUi();
    }

    @Override
    protected void handleEscape() {
        app.showAdventure();
    }

    private void buildUi() {
        Table screen = new Table();
        screen.top();
        screen.pad(20f, 28f, 18f, 28f);
        screen.add(titleBar("QUESTS")).growX().padBottom(8f);
        screen.row();
        screen.add(buildTabs()).growX().padBottom(8f);
        screen.row();
        screen.add(buildQuestList()).grow();
        screen.row();
        screen.add(buildFooter()).growX().padTop(8f);
        root.add(screen).grow();
    }

    private Table buildTabs() {
        Table tabs = new Table();
        for (String name : List.of("ALL", "MAIN", "DAILY", "EPIC")) {
            TextButton button = name.equals(category)
                ? theme.primaryButton(name)
                : theme.secondaryButton(name);
            button.getLabel().setFontScale(0.8f);
            UiActions.onClick(button, () -> setCategory(name));
            tabs.add(button).width(150f).height(46f).padRight(7f);
        }
        tabs.add().expandX();
        return tabs;
    }

    private ScrollPane buildQuestList() {
        Table list = new Table();
        list.top();
        for (QuestDefinition quest : filteredQuests()) {
            list.add(buildQuestCard(quest)).growX().padBottom(8f);
            list.row();
        }
        ScrollPane scroll = new ScrollPane(list, theme.skin());
        scroll.setFadeScrollBars(false);
        scroll.setOverscroll(false, false);
        scroll.setScrollingDisabled(true, false);
        return scroll;
    }

    private Table buildQuestCard(QuestDefinition quest) {
        QuestProgress progress = user.getQuestLog().getProgress(quest);
        boolean completed = progress.isCompleted(quest.getTarget());
        boolean claimed = progress.isRewardClaimed();
        Table card = theme.settingsCardPanel(12f);

        Table text = new Table();
        text.left();
        Label title = theme.heading(quest.getTitle());
        title.setAlignment(Align.left);
        text.add(title).growX().left();
        Label tag = theme.settingsLabel(quest.getCategory() + " • " + quest.getPriority());
        tag.setAlignment(Align.right);
        tag.setFontScale(0.78f);
        text.add(tag).right();
        text.row();

        Label description = theme.settingsLabel(questDescription(quest));
        description.setWrap(true);
        description.setAlignment(Align.left);
        text.add(description).colspan(2).growX().height(44f).padTop(2f);
        text.row();

        int reward = progress.resolveRewardAmount(quest.getRewardAmount());
        String progressText = progress.getProgress() + " / " + quest.getTarget();
        Label progressLabel = theme.settingsLabel("Progress: " + progressText);
        Label rewardLabel = theme.settingsLabel("Reward: " + reward + " " + quest.getRewardType());
        rewardLabel.setAlignment(Align.right);
        text.add(progressLabel).left().padTop(4f);
        text.add(rewardLabel).right().padTop(4f);

        card.add(text).growX().padRight(12f);

        TextButton action;
        if (claimed) {
            action = theme.secondaryButton("CLAIMED");
            action.setDisabled(true);
        } else if (completed) {
            action = theme.primaryButton("CLAIM");
            UiActions.onClick(action, () -> claimQuest(quest));
        } else {
            action = theme.tertiaryButton("ACTIVE");
            action.setDisabled(true);
        }
        action.getLabel().setFontScale(0.78f);
        card.add(action).width(150f).height(48f);
        return card;
    }

    private String questDescription(QuestDefinition quest) {
        return switch (quest.getParameter()) {
            case "BANNED_FAMILY" -> quest.getDescription() + " Today: "
                + app.services().quests().getDailyBannedFamily();
            case "EMPTY_COLUMN" -> quest.getDescription() + " Column: "
                + app.services().quests().getDailyEmptyColumn();
            case "EMPTY_ROW" -> quest.getDescription() + " Row: "
                + app.services().quests().getDailyEmptyRow();
            case "EMPTY_CROSS" -> quest.getDescription() + " Index: "
                + app.services().quests().getDailyEmptyCrossIndex();
            default -> quest.getDescription();
        };
    }

    private List<QuestDefinition> filteredQuests() {
        ArrayList<QuestDefinition> result = new ArrayList<>();
        for (QuestDefinition quest : app.services().quests().getQuestFactory().getAllDefinitions()) {
            if ("ALL".equals(category) || quest.getCategory().name().equals(category)) {
                result.add(quest);
            }
        }
        result.sort(Comparator.comparingInt(this::priorityRank)
            .thenComparingInt(QuestDefinition::getId));
        return List.copyOf(result);
    }

    private int priorityRank(QuestDefinition quest) {
        QuestPriority priority = quest.getPriority();
        return switch (priority) {
            case CRITICAL -> 0;
            case HIGH -> 1;
            case MEDIUM -> 2;
            case LOW -> 3;
        };
    }

    private Table buildFooter() {
        Table footer = new Table();
        status.setWrap(false);
        status.setAlignment(Align.left);
        footer.add(status).width(650f).left();
        footer.add().expandX();
        TextButton miniGames = theme.primaryButton("Mini Games");
        TextButton back = theme.secondaryButton("Back");
        UiActions.onClick(miniGames, app::showMiniGamesFromQuests);
        UiActions.onClick(back, app::showAdventure);
        footer.add(miniGames).width(180f).height(50f).padRight(8f);
        footer.add(back).width(160f).height(50f);
        return footer;
    }

    private void claimQuest(QuestDefinition quest) {
        ActionResult result = app.services().quests().claimReward(quest.getId());
        if (result.isSuccessful()) {
            theme.showSuccess(status, result.getMessage());
        } else {
            theme.showError(status, result.getMessage());
        }
        rebuild();
    }

    private void setCategory(String next) {
        category = next;
        rebuild();
    }

    private void rebuild() {
        root.clearChildren();
        buildUi();
    }
}
