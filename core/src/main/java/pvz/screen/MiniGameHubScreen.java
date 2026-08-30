package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import model.MiniGameType;
import pvz.PvzApplication;

public final class MiniGameHubScreen extends AuthenticatedUiScreen {
    private static final String VASE_ICON =
        "IMAGE_EGGBREAKER_VASE_EGG_BROWN_VASE_EGG_BROWN_151X198";
    private static final String BOWLING_ICON =
        "IMAGE_PLANT_WALLNUT_WALLNUT_169X187";
    private static final String BRAIN_ICON =
        "IMAGE_UI_GAMEOVER_FAIL_SCREEN_BRAIN_ONLY";

    private final Label status;

    public MiniGameHubScreen(PvzApplication app) {
        super(app);
        status = theme.statusLabel();
        buildUi();
    }

    private void buildUi() {
        Table screen = new Table();
        screen.top().pad(24f, 54f, 16f, 54f);

        screen.add(titleBar("MINI GAMES"))
            .width(1170f)
            .height(56f)
            .padBottom(12f);
        screen.row();

        Table cards = new Table();
        cards.defaults().pad(10f);
        cards.add(gameCard(
            MiniGameType.VASEBREAKER,
            "Vasebreaker",
            "Break vases, collect temporary plant packets, and survive.",
            VASE_ICON
        )).width(350f).height(310f);
        cards.add(gameCard(
            MiniGameType.WALLNUT_BOWLING,
            "Wall-nut Bowling",
            "Use the conveyor and bowl three distinct wall-nut types.",
            BOWLING_ICON
        )).width(350f).height(310f);
        cards.add(gameCard(
            MiniGameType.I_ZOMBIE,
            "I, Zombie",
            "Spend sun on zombies and eat all five brains.",
            BRAIN_ICON
        )).width(350f).height(310f);
        cards.row();
        cards.add(gameCard(
            MiniGameType.BEGHOULD,
            "Beghouled",
            "Swap adjacent plants, create matches, and defend the lawn.",
            "IMAGE_UI_PENNY_PURSUITS_LEVEL_ICONS_TYPE_ICON_MINIGAME"
        )).width(350f).height(290f);
        cards.add(gameCard(
            MiniGameType.ZOMBOTANY,
            "Zombotany",
            "Fight zombies that carry plant powers.",
            "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_MUMMY"
        )).width(350f).height(290f);

        ScrollPane scroller = new ScrollPane(cards, theme.skin());
        scroller.setFadeScrollBars(false);
        scroller.setScrollingDisabled(true, false);
        screen.add(scroller).width(1160f).height(455f).center();
        screen.row().padTop(8f);

        TextButton back = theme.secondaryButton(app.miniGameBackText());
        UiActions.onClick(back, app::leaveMiniGames);
        screen.add(back).width(300f).height(48f);
        screen.row().padTop(4f);

        status.setAlignment(Align.center);
        screen.add(status).width(950f).height(30f);

        root.add(screen).grow();
    }

    private Table gameCard(
        MiniGameType type,
        String title,
        String description,
        String iconId
    ) {
        Table card = theme.settingsCardPanel(14f);
        card.top();

        Image icon = theme.image(iconId);
        if (icon != null) {
            card.add(icon).size(92f).padTop(8f).padBottom(8f);
            card.row();
        }

        Label titleLabel = theme.heading(title);
        titleLabel.setAlignment(Align.center);
        titleLabel.setWrap(true);
        titleLabel.setFontScale(0.84f);
        card.add(titleLabel).growX().height(50f);
        card.row().padTop(8f);

        Label body = theme.bodyLabel(description);
        body.setAlignment(Align.center);
        body.setWrap(true);
        body.setFontScale(0.78f);
        card.add(body).width(300f).height(72f);
        card.row().padTop(12f);

        card.add(levelButtons(type)).growX().height(58f);
        return card;
    }

    private Table levelButtons(MiniGameType type) {
        Table row = new Table();
        for (int level = 1; level <= 3; level++) {
            boolean completed = user.getProgress()
                .isMiniGameLevelCompleted(type, level);
            boolean unlocked = user.getProgress()
                .isMiniGameLevelUnlocked(type, level);
            String text = completed
                ? "L" + level + " ✓"
                : unlocked ? "Level " + level : "LOCKED";
            TextButton button = completed
                ? theme.tertiaryButton(text)
                : theme.primaryButton(text);
            button.setDisabled(!unlocked);
            if (unlocked) {
                int selectedLevel = level;
                UiActions.onClick(button, () -> start(type, selectedLevel));
            }
            row.add(button).width(96f).height(48f).pad(3f);
        }
        return row;
    }

    private void start(MiniGameType type, int level) {
        if (!app.startMiniGame(type, level)) {
            theme.showError(
                status,
                "Could not start " + displayName(type) + " level " + level + "."
            );
        }
    }

    private String displayName(MiniGameType type) {
        return switch (type) {
            case VASEBREAKER -> "VASEBREAKER";
            case WALLNUT_BOWLING -> "WALL-NUT BOWLING";
            case I_ZOMBIE -> "I, ZOMBIE";
            case BEGHOULD -> "BEGHOULed";
            case ZOMBOTANY -> "ZOMBOTANY";
        };
    }
}
