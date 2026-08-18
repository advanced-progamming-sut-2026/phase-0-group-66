package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import model.Chapter;
import model.GameProgress;
import pvz.PvzApplication;
import pvz.ui.UiTheme;

import java.util.List;

public final class AdventureScreen extends AuthenticatedUiScreen {
    private static final float CARD_WIDTH = 438f;
    private static final float CARD_HEIGHT = 184f;
    private static final float CARD_GAP = 12f;
    private static final float INFO_HEIGHT = 62f;
    private static final String LOCK_ICON = "IMAGE_UI_GENERIC_BUTTON_HUD_MINIGAMES_LOCKED";

    private static final String[] CHAPTER_ART = {
        "IMAGE_BACKGROUNDS_EGYPT_TEXTURE",
        "IMAGE_BACKGROUNDS_ICEAGE_TEXTURE",
        "IMAGE_BACKGROUNDS_BEACH_TEXTURE",
        "IMAGE_BACKGROUNDS_DARK_TEXTURE"
    };

    private static final String[] CHAPTER_TROPHY = {
        "IMAGE_WORLDMAP_TROPHY_EGYPT",
        "IMAGE_WORLDMAP_TROPHY_ICEAGE",
        "IMAGE_WORLDMAP_TROPHY_BEACH",
        "IMAGE_WORLDMAP_TROPHY_DARK"
    };

    public AdventureScreen(PvzApplication app) {
        super(app);
        buildUi();
    }

    private void buildUi() {
        Table screen = new Table();
        screen.top();
        screen.pad(46f, 54f, 20f, 54f);

        screen.add(titleBar("ADVENTURE"))
            .width(1120f)
            .height(54f)
            .padBottom(10f);
        screen.row();

        screen.add(buildChapterGrid())
            .expandX()
            .center()
            .padBottom(12f);
        screen.row();

        screen.add(buildFooter())
            .width(1120f)
            .height(54f);

        root.add(screen).grow();
    }

    private Table buildChapterGrid() {
        Table grid = new Table();
        List<Chapter> chapters = app.services().adventure().getChapters();

        for (int index = 0; index < chapters.size(); index++) {
            Chapter chapter = chapters.get(index);
            grid.add(chapterCard(chapter, index))
                .width(CARD_WIDTH)
                .height(CARD_HEIGHT)
                .pad(CARD_GAP / 2f);

            if (index % 2 == 1) {
                grid.row();
            }
        }
        return grid;
    }

    private Button chapterCard(Chapter chapter, int index) {
        boolean unlocked = user.getProgress().isChapterUnlocked(chapter.getName());
        Button button = new Button(cardStyle());
        button.add(buildChapterContent(chapter, index, unlocked)).grow();
        button.setDisabled(!unlocked);

        if (unlocked) {
            UiActions.onClick(button, () -> app.showChapterLevels(chapter));
        }
        return button;
    }

    private Stack buildChapterContent(Chapter chapter, int index, boolean unlocked) {
        Stack stack = new Stack();

        Table artLayer = new Table();
        Drawable art = theme.drawable(CHAPTER_ART[index]);
        if (art != null) {
            artLayer.setBackground(art);
        }
        stack.add(artLayer);

        Table badgeLayer = new Table();
        badgeLayer.top().right();
        Image badge = theme.image(unlocked ? CHAPTER_TROPHY[index] : LOCK_ICON);
        if (badge != null) {
            badgeLayer.add(badge)
                .size(unlocked ? 48f : 42f)
                .pad(9f);
        }
        stack.add(badgeLayer);

        Table bottomLayer = new Table();
        bottomLayer.bottom();
        bottomLayer.add(chapterInfo(chapter, unlocked))
            .growX()
            .height(INFO_HEIGHT);
        stack.add(bottomLayer);

        return stack;
    }

    private Table chapterInfo(Chapter chapter, boolean unlocked) {
        Table panel = new Table();
        panel.setBackground(theme.skin().getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        panel.pad(6f, 12f, 6f, 12f);

        Label name = theme.heading(chapter.getName());
        name.setAlignment(Align.left);
        panel.add(name).expandX().left();

        Label status = theme.fieldLabel(unlocked ? "UNLOCKED" : "LOCKED");
        status.setAlignment(Align.right);
        panel.add(status).right();
        panel.row();

        Label progress = theme.bodyLabel(progressText(chapter));
        progress.setWrap(false);
        progress.setAlignment(Align.left);
        panel.add(progress)
            .colspan(2)
            .growX()
            .left()
            .padTop(1f);

        return panel;
    }

    private String progressText(Chapter chapter) {
        int completed = completedLevels(chapter, user.getProgress());
        return completed + " / " + chapter.getLevels().size() + " levels completed";
    }

    private int completedLevels(Chapter chapter, GameProgress progress) {
        if (progress.getLastChapterNumber() > chapter.getChapterNumber()) {
            return chapter.getLevels().size();
        }
        if (progress.getLastChapterNumber() == chapter.getChapterNumber()) {
            return Math.min(progress.getLastLevelNumber(), chapter.getLevels().size());
        }
        return 0;
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

    private Table buildFooter() {
        Table footer = new Table();
        TextButton collection = theme.secondaryButton("Collection");
        TextButton greenhouse = theme.primaryButton("Greenhouse");
        TextButton quests = theme.tertiaryButton("Quests");
        TextButton miniGames = theme.primaryButton("Mini Games");
        TextButton back = theme.secondaryButton("Back");

        UiActions.onClick(collection, app::showCollection);
        UiActions.onClick(greenhouse, app::showGreenhouse);
        UiActions.onClick(quests, app::showQuests);
        UiActions.onClick(
            miniGames,
            () -> app.showPlaceholder("Mini Games", "Back to Adventure", app::showAdventure)
        );
        UiActions.onClick(back, app::showMainMenu);

        footer.add(collection).width(155f).height(48f).padRight(7f);
        footer.add(greenhouse).width(165f).height(48f).padRight(7f);
        footer.add(quests).width(140f).height(48f).padRight(7f);
        footer.add(miniGames).width(155f).height(48f);
        footer.add().expandX();
        footer.add(back).width(145f).height(48f);
        return footer;
    }
}
