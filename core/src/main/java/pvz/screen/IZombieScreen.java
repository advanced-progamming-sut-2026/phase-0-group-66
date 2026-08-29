package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import network.client.NetworkIZombieSession;
import network.game.MatchReaction;
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
    private final Label plantSunLabel;
    private final Label progress;
    private final Label reactionLabel;
    private String selectedCard;
    private String selectedPlant = "Sunflower";

    public IZombieScreen(PvzApplication app) {
        super(app);
        iZombie = (IZombieSession) session;
        units = new MiniGameUnitLayer(app);
        brainLayer = new Group();
        cardTray = new Table();
        sunLabel = theme.heading("");
        plantSunLabel = theme.heading("");
        progress = theme.settingsLabel("");
        reactionLabel = theme.settingsLabel("");
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
        if (onlineSession() != null) {
            screen.row().padTop(4f);
            screen.add(buildReactionPanel()).colspan(2).width(1205f).height(78f);
        }
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
        NetworkIZombieSession online = onlineSession();
        if (online != null) {
            panel.add(theme.heading("ROLE: " + online.getRole())).padBottom(5f);
            panel.row();
            if (online.getRole() == network.game.MatchRole.PLANTS) {
                panel.add(buildPlantControls()).width(285f).height(365f).top();
            } else {
                panel.add(buildZombieControls()).width(285f).height(365f).top();
            }
        } else {
            panel.add(theme.heading("ZOMBIE CARDS")).padBottom(5f);
            panel.row();
            panel.add(cardTray).width(285f).height(305f).top();
            panel.row().padTop(4f);
            panel.add(theme.heading("DEPLOY TO ROW")).padBottom(3f);
            panel.row();
            panel.add(buildZombieControls()).width(285f).height(135f).top();
        }
        panel.row().padTop(3f);
        panel.add(plantSunLabel).width(280f).height(22f);
        panel.row();
        progress.setAlignment(Align.center);
        panel.add(progress).width(280f).height(30f);
        panel.row().padTop(5f);
        TextButton back = theme.secondaryButton("Back to Mini Games");
        UiActions.onClick(back, app::returnToMiniGames);
        panel.add(back).width(255f).height(48f);
        return panel;
    }

    private Table buildZombieControls() {
        Table controls = new Table();
        for (int row = 1; row <= 5; row++) {
            int selectedRow = row;
            TextButton button = theme.primaryButton("ROW " + row);
            button.getLabel().setFontScale(0.78f);
            UiActions.onClick(button, () -> deploy(selectedRow));
            controls.add(button).width(126f).height(42f).pad(2f);
            if (row % 2 == 0) {
                controls.row();
            }
        }
        return controls;
    }

    private Table buildPlantControls() {
        Table controls = new Table();
        controls.add(theme.heading("PLANT CARDS")).colspan(3).padBottom(4f);
        controls.row();
        String[] plants = {"Sunflower", "Peashooter", "Wall-nut", "Snow Pea", "Repeater", "Cabbage-pult"};
        for (int index = 0; index < plants.length; index++) {
            String plant = plants[index];
            TextButton button = theme.primaryButton(plant);
            button.getLabel().setFontScale(0.55f);
            UiActions.onClick(button, () -> {
                selectedPlant = plant;
                theme.showSuccess(message, plant + " selected. Choose a tile.");
            });
            controls.add(button).width(92f).height(36f).pad(2f);
            if (index % 3 == 2) {
                controls.row();
            }
        }
        controls.row().padTop(5f);
        controls.add(theme.heading("PLACE ON TILE")).colspan(9).padBottom(2f);
        controls.row();
        for (int row = 1; row <= 5; row++) {
            int selectedRow = row;
            for (int column = 1; column <= 9; column++) {
                int selectedColumn = column;
                TextButton tile = theme.secondaryButton(row + ":" + column);
                tile.getLabel().setFontScale(0.38f);
                UiActions.onClick(tile, () -> execute(
                    "plant " + selectedPlant + " " + selectedRow + " " + selectedColumn
                ));
                controls.add(tile).width(30f).height(28f).pad(1f);
            }
            controls.row();
        }
        return controls;
    }

    private Table buildReactionPanel() {
        Table reactions = theme.settingsCardPanel(8f);
        reactions.add(theme.fieldLabel("REACTIONS")).width(120f).height(28f).padRight(8f);
        String[] messages = {"Nice move!", "Good luck!", "Well played!"};
        for (String value : messages) {
            TextButton button = theme.secondaryButton(value);
            button.getLabel().setFontScale(0.52f);
            UiActions.onClick(button, () -> sendReaction("message", value));
            reactions.add(button).width(130f).height(32f).padRight(4f);
        }
        String[] emojis = {"😀", "🔥", "😮"};
        for (String value : emojis) {
            TextButton button = theme.primaryButton(value);
            UiActions.onClick(button, () -> sendReaction("emoji", value));
            reactions.add(button).width(48f).height(32f).padRight(4f);
        }
        reactions.add(reactionLabel).width(260f).height(28f);
        return reactions;
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
        plantSunLabel.setText(iZombie.isMultiplayer()
            ? "Plant sun " + iZombie.getPlantSun() : "");
        progress.setText(
            "Brains " + iZombie.getBrainsEaten() + " / 5   |   Score " + session.getScore()
        );
        NetworkIZombieSession online = onlineSession();
        if (online != null && !online.getReactions().isEmpty()) {
            MatchReaction latest = online.getReactions().get(online.getReactions().size() - 1);
            reactionLabel.setText(latest.sender() + ": " + latest.value());
        }
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

    private void sendReaction(String category, String value) {
        NetworkIZombieSession online = onlineSession();
        if (online == null || session.isFinished()) {
            return;
        }
        try {
            online.sendReaction(category, value);
            refreshFromSession();
        } catch (IllegalStateException exception) {
            theme.showError(message, exception.getMessage());
        }
    }
}
