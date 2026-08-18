package pvz.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import model.MiniGameType;
import pvz.PvzApplication;

public final class MiniGameHubScreen extends AuthenticatedUiScreen {
    private static final String VASE_ICON = "IMAGE_UI_FEATURE_UNLOCK_FEATURE_KEY_ART_VASEBREAKER";
    private static final String BOWLING_ICON = "IMAGE_PLANT_WALLNUT_WALLNUT_100X106";
    private static final String BRAIN_ICON = "IMAGE_UI_GAMEOVER_FAIL_SCREEN_BRAIN_ONLY";
    private static final String BONUS_ICON = "IMAGE_UI_GENERIC_BUTTON_HUD_MINIGAMES_NORMAL";

    private final Label status;

    public MiniGameHubScreen(PvzApplication app) {
        super(app);
        status = theme.statusLabel();
        buildUi();
    }

    private void buildUi() {
        Table screen = new Table();
        screen.top().pad(38f, 54f, 22f, 54f);
        screen.add(titleBar("MINI GAMES")).width(1170f).height(54f).padBottom(14f);
        screen.row();

        Table cards = new Table();
        cards.defaults().pad(8f);
        cards.add(gameCard(
            MiniGameType.VASEBREAKER,
            "Vasebreaker",
            "Break vases, collect temporary plant packets, and survive.",
            VASE_ICON,
            true
        )).width(350f).height(245f);
        cards.add(gameCard(
            MiniGameType.WALLNUT_BOWLING,
            "Wall-nut Bowling",
            "Use the conveyor and bowl three distinct wall-nut types.",
            BOWLING_ICON,
            true
        )).width(350f).height(245f);
        cards.add(gameCard(
            MiniGameType.I_ZOMBIE,
            "I, Zombie",
            "Spend sun on zombies and eat all five brains.",
            BRAIN_ICON,
            true
        )).width(350f).height(245f);
        cards.row();
        cards.add(gameCard(
            MiniGameType.BEGHOULD,
            "Beghouled",
            "Bonus mini-game - graphical version comes after the required set.",
            BONUS_ICON,
            false
        )).width(350f).height(205f);
        cards.add(gameCard(
            MiniGameType.ZOMBOTANY,
            "Zombotany",
            "Bonus mini-game - graphical version comes after the required set.",
            BONUS_ICON,
            false
        )).width(350f).height(205f);

        Table right = theme.settingsCardPanel(14f);
        right.top();
        right.add(theme.heading("REQUIRED MINI GAMES")).padBottom(12f);
        right.row();
        Label help = theme.bodyLabel(
            "Each required mini-game has three levels. Complete a level to unlock "
                + "the next one. Wins reward coins and update quests automatically."
        );
        help.setAlignment(Align.left);
        right.add(help).width(300f).left();
        right.row().padTop(18f);
        TextButton back = theme.secondaryButton(app.miniGameBackText());
        UiActions.onClick(back, app::leaveMiniGames);
        right.add(back).width(260f).height(54f);

        cards.add(right).width(350f).height(205f);
        screen.add(cards).expand().center();
        screen.row();
        status.setAlignment(Align.center);
        screen.add(status).width(1000f).height(30f).padTop(3f);
        root.add(screen).grow();
    }

    private Table gameCard(
        MiniGameType type,
        String title,
        String description,
        String iconId,
        boolean playable
    ) {
        Table card = theme.settingsCardPanel(12f);
        card.top();
        Table heading = new Table();
        Image icon = theme.image(iconId);
        if (icon != null) {
            heading.add(icon).size(72f).padRight(10f);
        }
        Label titleLabel = theme.heading(title);
        titleLabel.setAlignment(Align.left);
        heading.add(titleLabel).expandX().left();
        card.add(heading).growX().height(78f);
        card.row();

        Label body = theme.bodyLabel(description);
        body.setAlignment(Align.left);
        body.setFontScale(0.78f);
        card.add(body).width(315f).height(playable ? 72f : 62f).left();
        card.row().padTop(7f);

        if (playable) {
            card.add(levelButtons(type)).growX().height(58f);
        } else {
            Label bonus = theme.settingsLabel("BONUS - NOT REQUIRED");
            bonus.setColor(new Color(0.48f, 0.22f, 0.67f, 1f));
            bonus.setAlignment(Align.center);
            card.add(bonus).growX().height(42f);
        }
        return card;
    }

    private Table levelButtons(MiniGameType type) {
        Table row = new Table();
        for (int level = 1; level <= 3; level++) {
            boolean completed = user.getProgress().isMiniGameLevelCompleted(type, level);
            boolean unlocked = user.getProgress().isMiniGameLevelUnlocked(type, level);
            String text = completed ? "L" + level + " ✓" : unlocked ? "Level " + level : "LOCKED";
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
            theme.showError(status, "Could not start " + type + " level " + level + ".");
        }
    }
}
