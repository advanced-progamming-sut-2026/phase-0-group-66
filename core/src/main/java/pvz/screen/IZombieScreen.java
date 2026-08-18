package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import model.IZombieSession;
import model.ZombieDefinition;
import pvz.PvzApplication;
import pvz.ui.MiniGameUnitLayer;
import pvz.ui.ZombieArtResolver;

import java.util.List;

public final class IZombieScreen extends MiniGamePlayScreen {
    private static final int ROWS = 5;
    private static final int COLS = 9;
    private static final float BOARD_WIDTH = 855f;
    private static final float BOARD_HEIGHT = 470f;
    private static final String BRAIN_ART = "IMAGE_UI_GAMEOVER_FAIL_SCREEN_BRAIN_ONLY";

    private final IZombieSession iZombie;
    private final MiniGameUnitLayer units;
    private final Group brainLayer;
    private final Table cardTray;
    private final Label sunLabel;
    private final Label progress;
    private String selectedCard;

    public IZombieScreen(PvzApplication app) {
        super(app);
        iZombie = (IZombieSession) session;
        units = new MiniGameUnitLayer(app);
        brainLayer = new Group();
        cardTray = new Table();
        sunLabel = theme.heading("");
        progress = theme.settingsLabel("");
        List<IZombieSession.ZombieCardView> cards = iZombie.getCardViews();
        selectedCard = cards.isEmpty() ? null : cards.get(0).key();
        buildUi();
        refreshFromSession();
    }

    private void buildUi() {
        Table screen = new Table();
        screen.top().pad(32f, 34f, 18f, 34f);
        screen.add(titleBar("I, ZOMBIE - LEVEL " + session.getLevel()))
            .colspan(2).width(1205f).height(52f).padBottom(10f);
        screen.row();
        screen.add(buildSidePanel()).width(315f).height(548f).padRight(12f);
        screen.add(buildBoard()).width(BOARD_WIDTH).height(BOARD_HEIGHT).top();
        screen.row();
        message.setAlignment(Align.center);
        screen.add(message).colspan(2).width(1060f).height(28f).padTop(5f);
        root.add(screen).grow();
    }

    private Table buildSidePanel() {
        Table panel = theme.settingsCardPanel(12f);
        panel.top();
        Table currency = theme.settingsBadgePanel(6f);
        Image sun = theme.image("IMAGE_UI_HUD_INGAME_SUN");
        if (sun != null) {
            currency.add(sun).size(34f).padRight(6f);
        }
        currency.add(sunLabel);
        panel.add(currency).width(280f).height(52f).padBottom(7f);
        panel.row();
        panel.add(theme.heading("ZOMBIE CARDS")).padBottom(5f);
        panel.row();
        panel.add(cardTray).width(285f).height(305f).top();
        panel.row().padTop(4f);
        panel.add(theme.heading("DEPLOY TO ROW")).padBottom(3f);
        panel.row();

        Table rows = new Table();
        for (int row = 1; row <= 5; row++) {
            int selectedRow = row;
            TextButton button = theme.primaryButton("ROW " + row);
            button.getLabel().setFontScale(0.78f);
            UiActions.onClick(button, () -> deploy(selectedRow));
            rows.add(button).width(126f).height(42f).pad(2f);
            if (row % 2 == 0) {
                rows.row();
            }
        }
        panel.add(rows).width(280f).height(135f);
        panel.row().padTop(3f);
        progress.setAlignment(Align.center);
        panel.add(progress).width(280f).height(30f);
        panel.row().padTop(5f);
        TextButton back = theme.secondaryButton("Back to Mini Games");
        UiActions.onClick(back, app::returnToMiniGames);
        panel.add(back).width(255f).height(48f);
        return panel;
    }

    private Stack buildBoard() {
        Stack board = new Stack();
        Image background = theme.image("IMAGE_BACKGROUNDS_BACKGROUND_LOD_BIGBRAINZ_TEXTURE");
        if (background == null) {
            background = theme.image("IMAGE_BACKGROUNDS_EGYPT_TEXTURE");
        }
        if (background != null) {
            background.setScaling(Scaling.stretch);
            board.add(background);
        }
        board.add(units);
        board.add(brainLayer);
        return board;
    }

    private void deploy(int row) {
        if (selectedCard == null || session.isFinished()) {
            return;
        }
        execute("deploy " + selectedCard + " " + row);
    }

    @Override
    protected void refreshFromSession() {
        units.setPlants(iZombie.getPlantViews());
        units.setZombies(iZombie.getZombieViews());
        rebuildBrains(iZombie.getBrains());
        rebuildCards(iZombie.getCardViews());
        sunLabel.setText(Integer.toString(iZombie.getSun()));
        progress.setText(
            "Brains " + iZombie.getBrainsEaten() + " / 5   |   Score " + session.getScore()
        );
    }

    private void rebuildBrains(boolean[] brains) {
        brainLayer.clearChildren();
        float cellHeight = BOARD_HEIGHT / ROWS;
        for (int row = 0; row < Math.min(ROWS, brains.length); row++) {
            if (!brains[row]) {
                continue;
            }
            Image brain = theme.image(BRAIN_ART);
            if (brain == null) {
                continue;
            }
            brain.setScaling(Scaling.fit);
            float size = cellHeight * 0.62f;
            float y = (ROWS - 1 - row) * cellHeight + (cellHeight - size) * 0.5f;
            brain.setBounds(4f, y, size, size);
            brainLayer.addActor(brain);
        }
    }

    private void rebuildCards(List<IZombieSession.ZombieCardView> cards) {
        cardTray.clearChildren();
        for (IZombieSession.ZombieCardView card : cards) {
            Table item = theme.settingsBadgePanel(4f);
            Image art = cardImage(card.type());
            if (art != null) {
                art.setScaling(Scaling.fit);
                item.add(art).size(48f).padRight(4f);
            }
            String text = card.type() + "  " + card.cost() + " sun";
            TextButton select = card.key().equals(selectedCard)
                ? theme.tertiaryButton(text)
                : theme.primaryButton(text);
            select.getLabel().setFontScale(0.58f);
            UiActions.onClick(select, () -> selectCard(card.key()));
            item.add(select).width(214f).height(42f);
            cardTray.add(item).width(280f).height(53f).padBottom(3f);
            cardTray.row();
        }
    }

    private Image cardImage(String type) {
        ZombieDefinition definition = resolveZombie(type);
        return definition == null ? null : ZombieArtResolver.image(theme, definition);
    }

    private ZombieDefinition resolveZombie(String type) {
        String lookup = switch (type) {
            case "Basic" -> "Basic Zombie";
            case "Conehead" -> "Conehead Zombie";
            case "Buckethead" -> "Buckethead Zombie";
            case "All-Star" -> "All-Star Zombie";
            case "Newspaper" -> "Newspaper Zombie";
            case "Prospector" -> "Prospector Zombie";
            case "Parasol" -> "Parasol Zombie";
            case "Ra" -> "Ra Zombie";
            case "Explorer" -> "Explorer Zombie";
            case "Knight" -> "Knight Zombie";
            case "Blockhead" -> "Brickhead Zombie";
            case "Dodo Rider" -> "Dodo Rider Zombie";
            case "Wizard" -> "Wizard Zombie";
            default -> type;
        };
        return app.services().gameData().getZombieFactory().findDefinition(lookup).orElse(null);
    }

    private void selectCard(String key) {
        selectedCard = key;
        rebuildCards(iZombie.getCardViews());
        theme.showSuccess(message, key + " selected. Choose a row.");
    }
}
