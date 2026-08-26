package pvz.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import model.Chapter;
import model.GameProgress;
import model.Level;
import pvz.PvzApplication;
import pvz.ui.UiTheme;

public final class ChapterLevelsScreen extends AuthenticatedUiScreen {
    private static final float CARD_WIDTH = 270f;
    private static final float CARD_HEIGHT = 352f;
    private static final String[] CHAPTER_BACKGROUNDS = {
        "IMAGE_BACKGROUNDS_EGYPT_TEXTURE",
        "IMAGE_BACKGROUNDS_ICEAGE_TEXTURE",
        "IMAGE_BACKGROUNDS_BEACH_TEXTURE",
        "IMAGE_BACKGROUNDS_DARK_TEXTURE"
    };
    private static final String NODE_COMPLETE = "IMAGE_UI_QUESTS_QUEST_LEVEL_NODE_COMPLETE";
    private static final String NODE_AVAILABLE = "IMAGE_UI_QUESTS_QUEST_LEVEL_NODE_NEXT";
    private static final String NODE_LOCKED = "IMAGE_UI_QUESTS_QUEST_LEVEL_NODE_UPCOMING";

    private final Chapter chapter;

    public ChapterLevelsScreen(PvzApplication app, Chapter chapter) {
        super(app);
        this.chapter = chapter;
        buildUi();
    }

    private void buildUi() {
        Image chapterBackground = theme.image(chapterBackgroundId());
        if (chapterBackground != null) {
            chapterBackground.setScaling(Scaling.fill);
            chapterBackground.setAlign(Align.center);
            chapterBackground.setFillParent(true);
            root.addActor(chapterBackground);
        }

        Table screen = new Table();
        screen.top();
        screen.pad(24f, 42f, 18f, 42f);
        screen.add(titleBar(chapter.getName())).width(1180f).height(54f);
        screen.row();
        screen.add(buildSummary()).width(1160f).height(64f).padTop(8f);
        screen.row();
        screen.add(buildLevels()).width(1160f).height(CARD_HEIGHT + 14f).center().padTop(12f);
        screen.row();
        screen.add(buildFooter()).width(1180f).height(50f).padTop(6f);
        root.add(screen).grow();
    }

    private Table buildSummary() {
        Table summary = new Table();
        summary.setBackground(
            theme.skin().getDrawable("image_ui_dialog_asset_inner_bkgd_10")
        );
        summary.pad(8f);
        int completed = completedLevels();
        summary.add(theme.heading("Chapter " + chapter.getChapterNumber())).padRight(18f);
        Label season = theme.bodyLabel(pretty(chapter.getSeason().name()));
        season.setWrap(false);
        summary.add(season).expandX().left();
        summary.add(theme.fieldLabel(completed + " / " + chapter.getLevels().size() + " completed"));
        return summary;
    }

    private Table buildLevels() {
        Table row = new Table();
        row.center();
        for (Level level : chapter.getLevels()) {
            row.add(levelCard(level)).width(CARD_WIDTH).height(CARD_HEIGHT).pad(7f);
        }
        return row;
    }

    private Button levelCard(Level level) {
        LevelState state = stateOf(level);
        Button button = new Button(cardStyle());
        button.add(levelContent(level, state)).grow();
        button.setColor(0.76f, 0.68f, 0.49f, 1f);
        button.setDisabled(state == LevelState.LOCKED);
        if (state != LevelState.LOCKED) {
            UiActions.onClick(button, () -> app.showLevelBriefing(chapter, level));
        }
        return button;
    }

    private Table levelContent(Level level, LevelState state) {
        Table content = new Table();
        content.pad(14f, 12f, 12f, 12f);

        Image node = theme.image(nodeFor(state));
        if (node != null) {
            content.add(node).size(86f).padBottom(5f);
            content.row();
        }

        content.add(theme.heading("LEVEL " + level.getLevelNumber())).padBottom(5f);
        content.row();

        Label type = theme.bodyLabel(pretty(level.getSpecialType().name()));
        type.setWrap(false);
        content.add(type).padBottom(12f);
        content.row();

        Table details = new Table();
        addDetail(details, "Waves", Integer.toString(level.getWaves().size()));
        addDetail(details, "Starting Sun", Integer.toString(level.getStartingSunAmount()));
        addDetail(details, "Plant Slots", Integer.toString(level.getAllowedPlantCount()));
        content.add(details).growX();
        content.row();

        Label status = theme.heading(statusText(state));
        status.setAlignment(Align.center);
        if (state == LevelState.LOCKED) {
            status.setColor(Color.GRAY);
        }
        content.add(status).expandY().bottom().padTop(12f);
        return content;
    }

    private void addDetail(Table table, String name, String value) {
        Label key = theme.fieldLabel(name);
        key.setAlignment(Align.left);
        table.add(key).expandX().left().padBottom(4f);
        Label data = theme.fieldLabel(value);
        data.setAlignment(Align.right);
        table.add(data).right().padBottom(4f);
        table.row();
    }

    private LevelState stateOf(Level level) {
        if (isCompleted(level)) {
            return LevelState.COMPLETED;
        }
        if (user.getProgress().isLevelUnlocked(level.getLevelId())) {
            return LevelState.AVAILABLE;
        }
        return LevelState.LOCKED;
    }

    private boolean isCompleted(Level level) {
        GameProgress progress = user.getProgress();
        return progress.getLastChapterNumber() > chapter.getChapterNumber()
            || (progress.getLastChapterNumber() == chapter.getChapterNumber()
            && progress.getLastLevelNumber() >= level.getLevelNumber());
    }

    private int completedLevels() {
        GameProgress progress = user.getProgress();
        if (progress.getLastChapterNumber() > chapter.getChapterNumber()) {
            return chapter.getLevels().size();
        }
        if (progress.getLastChapterNumber() == chapter.getChapterNumber()) {
            return Math.min(progress.getLastLevelNumber(), chapter.getLevels().size());
        }
        return 0;
    }

    private String nodeFor(LevelState state) {
        return switch (state) {
            case COMPLETED -> NODE_COMPLETE;
            case AVAILABLE -> NODE_AVAILABLE;
            case LOCKED -> NODE_LOCKED;
        };
    }

    private String statusText(LevelState state) {
        return switch (state) {
            case COMPLETED -> "COMPLETED";
            case AVAILABLE -> "PLAY";
            case LOCKED -> "LOCKED";
        };
    }

    private Button.ButtonStyle cardStyle() {
        Button.ButtonStyle style = new Button.ButtonStyle();
        Drawable background = theme.drawable(UiTheme.MAIN_MENU_TILE);
        if (background == null) {
            background = theme.skin().getDrawable("image_ui_dialog_asset_inner_bkgd_10");
        }
        style.up = background;
        style.down = background;
        style.disabled = background;
        return style;
    }

    private String chapterBackgroundId() {
        int index = Math.max(0, Math.min(CHAPTER_BACKGROUNDS.length - 1,
            chapter.getChapterNumber() - 1));
        return CHAPTER_BACKGROUNDS[index];
    }

    private Table buildFooter() {
        Table footer = new Table();
        TextButton quests = theme.tertiaryButton("Quests");
        TextButton miniGames = theme.primaryButton("Mini Games");
        TextButton back = theme.secondaryButton("Back to Chapters");
        UiActions.onClick(quests, app::showQuests);
        UiActions.onClick(miniGames, app::showMiniGames);
        UiActions.onClick(back, app::showAdventure);
        footer.add(quests).width(150f).height(50f).padRight(8f);
        footer.add(miniGames).width(170f).height(50f);
        footer.add().expandX();
        footer.add(back).width(230f).height(50f);
        return footer;
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

    private enum LevelState {
        COMPLETED,
        AVAILABLE,
        LOCKED
    }
}
