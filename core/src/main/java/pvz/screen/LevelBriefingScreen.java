package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import controller.ActionResult;
import model.Chapter;
import model.Level;
import pvz.PvzApplication;

public final class LevelBriefingScreen extends AuthenticatedUiScreen {
    private final Chapter chapter;
    private final Level level;
    private final Label status;

    public LevelBriefingScreen(PvzApplication app, Chapter chapter, Level level) {
        super(app);
        this.chapter = chapter;
        this.level = level;
        status = statusLabel();
        buildUi();
    }

    private void buildUi() {
        Table screen = new Table();
        screen.top();
        screen.padTop(24f);
        screen.add(titleBar(chapter.getName() + " - Level " + level.getLevelNumber()))
            .growX()
            .pad(0f, 42f, 0f, 42f);
        screen.row();
        screen.add(buildBriefing()).expand().center().padTop(14f);
        screen.row();
        screen.add(buildFooter()).growX().pad(0f, 42f, 24f, 42f);
        root.add(screen).grow();
    }

    private Table buildBriefing() {
        Table panel = theme.dialogPanel();
        panel.defaults().padBottom(8f);
        panel.add(theme.heading("MISSION BRIEFING")).colspan(2).padBottom(12f);
        panel.row();
        addInfo(panel, "Level Type", pretty(level.getSpecialType().name()));
        addInfo(panel, "Waves", Integer.toString(level.getWaves().size()));
        addInfo(panel, "Starting Sun", Integer.toString(level.getStartingSunAmount()));
        addInfo(panel, "Plant Slots", Integer.toString(level.getAllowedPlantCount()));
        addInfo(panel, "Wave Cost", waveCostText());

        panel.add(theme.fieldLabel("Objective")).top().left().padTop(8f);
        Label objective = theme.bodyLabel(level.getSpecialRuleSummary());
        objective.setAlignment(Align.left);
        objective.setWrap(true);
        panel.add(objective).width(620f).left().padTop(8f);
        panel.row();

        status.setWrap(false);
        panel.add(status).colspan(2).height(34f).padTop(4f);
        return panel;
    }

    private void addInfo(Table panel, String name, String value) {
        panel.add(theme.fieldLabel(name)).width(180f).left();
        Label label = theme.bodyLabel(value);
        label.setWrap(false);
        label.setAlignment(Align.left);
        panel.add(label).width(620f).left();
        panel.row();
    }

    private String waveCostText() {
        if (level.getWaves().isEmpty()) {
            return "No waves";
        }
        int first = level.getWaves().get(0).getDifficultyCost();
        int last = level.getWaves().get(level.getWaves().size() - 1).getDifficultyCost();
        return first + " - " + last;
    }

    private Table buildFooter() {
        Table footer = new Table();
        TextButton back = theme.secondaryButton("Back");
        TextButton prepare = theme.primaryButton(prepareButtonText());
        UiActions.onClick(back, () -> app.showChapterLevels(chapter));
        UiActions.onClick(prepare, this::prepareLevel);
        footer.add(back).width(180f).height(52f);
        footer.add().expandX();
        footer.add(prepare).width(260f).height(52f);
        return footer;
    }

    private String prepareButtonText() {
        if (level.getRuleStrategy().allowsManualPlantSelection()) {
            return "Choose Plants";
        }
        return "Prepare Level";
    }

    private void prepareLevel() {
        ActionResult result = app.services().game().startLevel(chapter.getName(), level.getLevelNumber());
        if (!result.isSuccessful()) {
            theme.showError(status, result.getMessage());
            return;
        }
        if (level.getRuleStrategy().allowsManualPlantSelection()) {
            app.showPlaceholder("Plant Selection");
        } else {
            app.showPlaceholder("Battle Screen - " + pretty(level.getSpecialType().name()));
        }
    }

    private String pretty(String value) {
        String[] words = value.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }
}
