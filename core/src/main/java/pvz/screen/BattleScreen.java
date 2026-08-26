package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import controller.ActionResult;
import controller.GameController;
import model.Board;
import model.Chapter;
import model.Game;
import model.GameState;
import model.Level;
import model.Plant;
import model.PlantDefinition;
import model.PlantFoodType;
import model.Sun;
import model.Wave;
import model.Zombie;
import pvz.PvzApplication;
import pvz.ui.BattleBoardActor;
import pvz.ui.UiTheme;

import java.util.ArrayList;
import java.util.List;

public final class BattleScreen extends AuthenticatedUiScreen {
    private static final float MODEL_STEP_SECONDS = 0.50f;
    private static final int BASE_TICKS_PER_STEP = 5;
    private static final float PAN_TO_ZOMBIES_SECONDS = 1.25f;
    private static final float ZOMBIE_PREVIEW_SECONDS = 1.75f;
    private static final float PAN_HOME_SECONDS = 1.25f;
    private static final float READY_DELAY_SECONDS = 2.50f;
    private static final float PLANT_FOOD_ANIMATION_SECONDS = 1.10f;

    private final Chapter chapter;
    private final Level level;
    private final GameController controller;
    private final Game game;
    private final Label sunLabel;
    private final Label plantFoodLabel;
    private final Label waveLabel;
    private final Label status;
    private final Label introLabel;
    private final Table seedBank;
    private final Stack pauseLayer;
    private final BattleBoardActor boardActor;
    private final TextButton startWavesButton;

    private float modelAccumulator;
    private String selectedPlant;
    private ToolMode toolMode = ToolMode.PLANT;
    private boolean paused;
    private boolean resultShown;
    private float introElapsed;
    private IntroPhase introPhase = IntroPhase.PAN_TO_ZOMBIES;
    private PendingPlantFood pendingPlantFood;
    private float plantFoodImpactHold;

    public BattleScreen(PvzApplication app, Chapter chapter, Level level) {
        super(app);
        this.chapter = chapter;
        this.level = level;
        this.controller = app.services().game();
        this.game = controller.getGame();
        if (game == null || game.getGameState() != GameState.RUNNING) {
            throw new IllegalStateException("BattleScreen requires a running game.");
        }

        sunLabel = theme.heading("");
        plantFoodLabel = theme.settingsLabel("");
        waveLabel = theme.settingsLabel("");
        status = theme.statusLabel();
        introLabel = theme.title("");
        introLabel.setAlignment(Align.center);
        seedBank = new Table();
        pauseLayer = new Stack();
        boardActor = new BattleBoardActor(app.assets(), controller, level, this::handleCellClick);
        boardActor.setShowGrid(user.isGridVisible());
        startWavesButton = theme.tertiaryButton("START WAVES");
        buildUi();
        refreshSeedBank();
        refreshHud();
    }

    private void buildUi() {
        root.clearChildren();
        Stack screen = new Stack();
        screen.add(buildBattleLayer());
        screen.add(buildPauseLayer());
        root.add(screen).grow();
    }

    private Actor buildBattleLayer() {
        Stack battle = new Stack();
        battle.add(boardActor);

        Table hud = new Table();
        hud.top();
        hud.add(buildTopHud()).growX().height(118f);
        hud.row();
        hud.add().expand();
        hud.row();
        hud.add(buildBottomHud()).growX().height(60f).pad(0f, 16f, 6f, 16f);
        battle.add(hud);
        battle.add(buildIntroLayer());
        return battle;
    }

    private Table buildIntroLayer() {
        Table layer = new Table();
        layer.center();
        layer.setTouchable(Touchable.disabled);
        introLabel.setTouchable(Touchable.disabled);
        layer.add(introLabel).width(560f).height(90f);
        return layer;
    }

    private Table buildTopHud() {
        Table bar = new Table();
        bar.pad(8f, 14f, 4f, 14f);

        Table sunBadge = badgePanel(5f);
        Image sun = theme.image("IMAGE_UI_HUD_INGAME_SUN");
        if (sun != null) {
            sunBadge.add(sun).size(42f).padRight(4f);
        }
        sunBadge.add(sunLabel).minWidth(72f).left();
        bar.add(sunBadge).height(58f).padRight(8f);

        seedBank.left();
        bar.add(seedBank).expandX().left();

        Table tools = new Table();
        tools.add(toolButton("IMAGE_UI_HUD_INGAME_PLANTFOOD_BUTTON", this::selectPlantFood))
            .size(54f).padRight(6f);
        tools.add(toolButton("IMAGE_UI_HUD_INGAME_SHOVEL_BUTTON", this::selectShovel))
            .size(54f).padRight(6f);
        tools.add(toolButton("IMAGE_UI_HUD_INGAME_PAUSE_BUTTON", this::togglePause))
            .size(54f);
        bar.add(tools).right();
        return bar;
    }

    private Table buildBottomHud() {
        Table bar = new Table();
        plantFoodLabel.setAlignment(Align.left);
        waveLabel.setAlignment(Align.right);
        status.setAlignment(Align.center);
        status.setEllipsis(true);

        Image food = theme.image("IMAGE_UI_HUD_INGAME_PLANTFOOD_BANK_FILLED_SLOT");
        if (food != null) {
            bar.add(food).size(34f).padRight(3f);
        }
        bar.add(plantFoodLabel).width(100f).left();

        startWavesButton.setVisible(false);
        UiActions.onClick(startWavesButton, this::startZombieWaves);
        bar.add(startWavesButton).width(170f).height(46f).padLeft(10f);

        bar.add(status).expandX().fillX().pad(0f, 12f, 0f, 12f);

        Image meter = theme.image("IMAGE_UI_HUD_INGAME_PROGRESS_METER");
        if (meter != null) {
            meter.setScaling(Scaling.fit);
            bar.add(meter).width(118f).height(34f).padRight(5f);
        }
        bar.add(waveLabel).width(128f).right();
        return bar;
    }

    private Stack buildPauseLayer() {
        pauseLayer.clearChildren();
        Table dim = new Table();
        Image background = theme.image("IMAGE_UI_DIALOG_ASSET_TINT_ROUNDED_BOX_9SLICE");
        if (background != null) {
            background.getColor().a = 0.95f;
            dim.add(background).width(460f).height(300f);
        }

        Table menu = theme.dialogPanel();
        menu.add(theme.title("PAUSED")).padBottom(18f);
        menu.row();
        TextButton resume = theme.primaryButton("Resume");
        TextButton restartSelection = theme.secondaryButton("Choose Plants");
        TextButton mainMenu = theme.tertiaryButton("Main Menu");
        UiActions.onClick(resume, this::togglePause);
        UiActions.onClick(restartSelection, this::returnToPlantSelection);
        UiActions.onClick(mainMenu, app::showMainMenu);
        menu.add(resume).width(250f).height(56f).padBottom(8f);
        menu.row();
        menu.add(restartSelection).width(250f).height(52f).padBottom(8f);
        menu.row();
        menu.add(mainMenu).width(250f).height(52f);

        pauseLayer.add(new Table());
        pauseLayer.add(menu);
        pauseLayer.setVisible(false);
        return pauseLayer;
    }

    private Table badgePanel(float padding) {
        Table table = new Table();
        table.setBackground(
            theme.skin().getDrawable("image_ui_dialog_asset_inner_bkgd_10")
        );
        table.pad(padding);
        return table;
    }

    private Table toolButton(String imageId, Runnable action) {
        Table button = badgePanel(4f);
        Image image = theme.image(imageId);
        if (image != null) {
            button.add(image).grow();
        } else {
            button.add(theme.settingsLabel("?"));
        }
        button.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        button.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                action.run();
            }
        });
        return button;
    }

    private void refreshSeedBank() {
        seedBank.clearChildren();
        List<String> selectedPlants = controller.getSelectedPlants();
        for (String plantName : selectedPlants) {
            PlantDefinition definition = app.services().gameData().getPlantFactory()
                .findDefinition(plantName)
                .orElse(null);
            if (definition == null) {
                continue;
            }
            seedBank.add(seedCard(definition)).width(104f).height(110f).padRight(6f);
        }
    }

    private Stack seedCard(PlantDefinition definition) {
        Stack stack = new Stack();
        boolean active = toolMode == ToolMode.PLANT && definition.getName().equals(selectedPlant);
        String frameId = active ? "IMAGE_UI_PACKETS_SELECTED" : "IMAGE_UI_PACKETS_READY";
        Image frame = theme.image(frameId);
        if (frame != null) {
            frame.setScaling(Scaling.stretch);
            stack.add(frame);
        }

        String packetId = packetArtId(definition);
        Image packet = packetId == null ? null : theme.image(packetId);
        if (packet != null) {
            packet.setScaling(Scaling.fit);
            Table packetLayer = new Table();
            packetLayer.pad(7f, 8f, 14f, 8f);
            packetLayer.add(packet).grow();
            stack.add(packetLayer);
        }

        int cooldown = game.getCooldownTicks(definition.getName());
        Table text = new Table();
        text.bottom();
        Label cost = theme.settingsLabel(Integer.toString(definition.getCost()));
        cost.setFontScale(0.72f);
        String cooldownText = cooldown <= 0 ? "READY" : String.format("%.1fs", cooldown / 10f);
        Label cooldownLabel = theme.settingsLabel(cooldownText);
        cooldownLabel.setFontScale(0.64f);
        text.add(cost).expandX().left().padLeft(5f);
        text.add(cooldownLabel).right().padRight(4f);
        stack.add(text);

        stack.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        stack.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                if (isIntroRunning()) {
                    return;
                }
                selectedPlant = definition.getName();
                toolMode = ToolMode.PLANT;
                theme.showSuccess(status, "Selected " + definition.getName() + ".");
                refreshSeedBank();
            }
        });
        return stack;
    }

    private void handleCellClick(int col, int row) {
        if (paused || isIntroRunning() || pendingPlantFood != null
            || game.getGameState() != GameState.RUNNING) {
            return;
        }
        if (collectSunIfPresent(col, row)) {
            refreshHud();
            return;
        }
        ActionResult result;
        if (toolMode == ToolMode.SHOVEL) {
            result = controller.pluckPlant(col, row);
            toolMode = ToolMode.PLANT;
        } else if (toolMode == ToolMode.PLANT_FOOD) {
            if (beginPlantFood(col, row)) {
                return;
            }
            result = controller.feedPlant(col, row);
            toolMode = ToolMode.PLANT;
        } else if (selectedPlant != null) {
            result = controller.plantPlant(selectedPlant, col, row);
            if (result.isSuccessful()) {
                selectedPlant = null;
            }
        } else {
            theme.showError(status, "Choose a seed packet, shovel, plant food, or a sun first.");
            return;
        }
        showResult(result);
        refreshSeedBank();
        refreshHud();
    }

    private boolean beginPlantFood(int col, int row) {
        Board board = game.getBoard();
        if (board == null || !board.isInside(row - 1, col - 1)) {
            return false;
        }
        Plant plant = board.getTile(row - 1, col - 1).getPlant();
        if (plant == null || plant.isDestroyed()) {
            return false;
        }
        if (game.getPlantFoodCount() <= 0
            || plant.getDefinition().getPlantFoodType() == PlantFoodType.NONE) {
            return false;
        }
        pendingPlantFood = new PendingPlantFood(
            col,
            row,
            plant,
            new ArrayList<>(board.getZombies())
        );
        toolMode = ToolMode.PLANT;
        selectedPlant = null;
        boardActor.triggerPlantFood(plant, PLANT_FOOD_ANIMATION_SECONDS + 0.25f);
        theme.showSuccess(status, "Plant Food! " + plant.getName() + " is powering up...");
        refreshSeedBank();
        return true;
    }

    private void advancePendingPlantFood(float delta) {
        if (pendingPlantFood == null || paused || resultShown) {
            return;
        }
        pendingPlantFood.elapsed += Math.max(0f, delta);
        if (pendingPlantFood.elapsed < PLANT_FOOD_ANIMATION_SECONDS) {
            return;
        }

        PendingPlantFood request = pendingPlantFood;
        pendingPlantFood = null;
        ActionResult result = controller.feedPlant(request.col, request.row);
        showResult(result);
        if (result.isSuccessful()) {
            ArrayList<Zombie> casualties = new ArrayList<>();
            for (Zombie zombie : request.zombiesBefore) {
                if (zombie != null && zombie.isDead()) {
                    casualties.add(zombie);
                }
            }
            boardActor.showPlantFoodCasualties(casualties, 0.55f);
            plantFoodImpactHold = 0.55f;
        }
        refreshSeedBank();
        refreshHud();
    }

    private boolean collectSunIfPresent(int col, int row) {
        Board board = game.getBoard();
        if (board == null) {
            return false;
        }
        for (Sun sun : board.getSunsAt(row - 1, col - 1)) {
            if (!sun.isCollected()) {
                ActionResult result = controller.collectSun(col, row);
                showResult(result);
                return result.isSuccessful();
            }
        }
        return false;
    }

    private void selectShovel() {
        if (isIntroRunning() || pendingPlantFood != null) {
            return;
        }
        toolMode = ToolMode.SHOVEL;
        selectedPlant = null;
        theme.showSuccess(status, "Shovel selected. Click a planted tile.");
        refreshSeedBank();
    }

    private void selectPlantFood() {
        if (isIntroRunning() || pendingPlantFood != null) {
            return;
        }
        toolMode = ToolMode.PLANT_FOOD;
        selectedPlant = null;
        theme.showSuccess(status, "Plant food selected. Click a plant.");
        refreshSeedBank();
    }

    private void startZombieWaves() {
        if (isIntroRunning() || pendingPlantFood != null) {
            return;
        }
        ActionResult result = controller.startZombieWaves();
        showResult(result);
        startWavesButton.setVisible(!game.areZombieWavesStarted());
        refreshSeedBank();
        refreshHud();
    }

    private void togglePause() {
        paused = !paused;
        pauseLayer.setVisible(paused);
    }

    @Override
    protected void handleEscape() {
        togglePause();
    }

    private void returnToPlantSelection() {
        paused = false;
        ActionResult result = controller.startLevel(chapter.getName(), level.getLevelNumber());
        if (result.isSuccessful()) {
            app.showPlantSelection(chapter, level);
        } else {
            theme.showError(status, result.getMessage());
            pauseLayer.setVisible(false);
        }
    }

    private void showResult(ActionResult result) {
        if (result == null) {
            return;
        }
        if (result.isSuccessful()) {
            theme.showSuccess(status, result.getMessage());
        } else {
            theme.showError(status, result.getMessage());
        }
    }

    @Override
    public void render(float delta) {
        advanceIntro(delta);
        advancePendingPlantFood(delta);
        advanceModel(delta);
        refreshHud();
        checkResult();
        super.render(delta);
    }

    private void advanceModel(float delta) {
        if (plantFoodImpactHold > 0f) {
            plantFoodImpactHold = Math.max(0f, plantFoodImpactHold - Math.max(0f, delta));
            return;
        }
        if (paused || resultShown || isIntroRunning() || pendingPlantFood != null
            || game.getGameState() != GameState.RUNNING) {
            return;
        }
        if (!game.areZombieWavesStarted()) {
            return;
        }
        modelAccumulator += Math.max(0f, delta);
        if (modelAccumulator < MODEL_STEP_SECONDS) {
            return;
        }
        modelAccumulator -= MODEL_STEP_SECONDS;
        int speed = Math.max(1, Math.min(3, user.getGameSpeed()));
        ActionResult result = controller.advanceTime(BASE_TICKS_PER_STEP * speed);
        if (!result.isSuccessful()) {
            theme.showError(status, result.getMessage());
        }
        refreshSeedBank();
    }

    private void refreshHud() {
        sunLabel.setText(Integer.toString(game.getSunAmount()));
        plantFoodLabel.setText("x" + game.getPlantFoodCount());
        Wave wave = game.getCurrentWave();
        int current = wave == null ? 0 : wave.getWaveNumber();
        waveLabel.setText("Wave " + current + " / " + level.getWaves().size());
        startWavesButton.setVisible(!isIntroRunning() && !game.areZombieWavesStarted());
    }

    private void advanceIntro(float delta) {
        if (!isIntroRunning() || paused || resultShown) {
            return;
        }
        introElapsed += Math.max(0f, delta);
        switch (introPhase) {
            case PAN_TO_ZOMBIES -> updatePanToZombies();
            case SHOW_ZOMBIES -> updateZombiePreview();
            case PAN_HOME -> updatePanHome();
            case READY_DELAY -> updateReadyDelay();
            case FINISHED -> finishIntro();
        }
    }

    private void updatePanToZombies() {
        float progress = clamp01(introElapsed / PAN_TO_ZOMBIES_SECONDS);
        boardActor.setCameraPan(smooth(progress));
        introLabel.setText("");
        if (progress >= 1f) {
            changeIntroPhase(IntroPhase.SHOW_ZOMBIES);
        }
    }

    private void updateZombiePreview() {
        boardActor.setCameraPan(1f);
        introLabel.setText("ZOMBIES APPROACHING...");
        if (introElapsed >= ZOMBIE_PREVIEW_SECONDS) {
            changeIntroPhase(IntroPhase.PAN_HOME);
        }
    }

    private void updatePanHome() {
        float progress = clamp01(introElapsed / PAN_HOME_SECONDS);
        boardActor.setCameraPan(1f - smooth(progress));
        introLabel.setText("");
        if (progress >= 1f) {
            changeIntroPhase(IntroPhase.READY_DELAY);
        }
    }

    private void updateReadyDelay() {
        boardActor.setCameraPan(0f);
        if (introElapsed < 0.85f) {
            introLabel.setText("READY...");
        } else if (introElapsed < 1.65f) {
            introLabel.setText("SET...");
        } else {
            introLabel.setText("PLANT!");
        }
        if (introElapsed >= READY_DELAY_SECONDS) {
            changeIntroPhase(IntroPhase.FINISHED);
            finishIntro();
        }
    }

    private void finishIntro() {
        introPhase = IntroPhase.FINISHED;
        introLabel.setText("");
        boardActor.setCameraPan(0f);
        startWavesButton.setVisible(!game.areZombieWavesStarted());
        if (game.areZombieWavesStarted()) {
            theme.showSuccess(status, "The first wave is coming!");
        } else {
            theme.showSuccess(status, "Set up your defense, then start the waves.");
        }
    }

    private void changeIntroPhase(IntroPhase phase) {
        introPhase = phase;
        introElapsed = 0f;
    }

    private boolean isIntroRunning() {
        return introPhase != IntroPhase.FINISHED;
    }

    private float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private float smooth(float value) {
        return value * value * (3f - 2f * value);
    }

    private void checkResult() {
        if (resultShown || game.getGameState() == GameState.RUNNING) {
            return;
        }
        resultShown = true;
        paused = true;
        showEndOverlay(game.getGameState() == GameState.WON);
    }

    private void showEndOverlay(boolean won) {
        pauseLayer.clearChildren();
        Table panel = theme.dialogPanel();
        panel.add(theme.title(won ? "LEVEL COMPLETE!" : "ZOMBIES ATE YOUR BRAINS!"))
            .padBottom(12f);
        panel.row();
        panel.add(theme.settingsLabel("Zombie kills: " + game.getZombieKillCount())).padBottom(4f);
        panel.row();
        panel.add(theme.settingsLabel("Sun collected: " + game.getTotalSunCollected())).padBottom(4f);
        panel.row();
        panel.add(theme.settingsLabel("Time: " + String.format("%.1fs", game.getElapsedTicks() / 10f)))
            .padBottom(14f);
        panel.row();
        TextButton continueButton = theme.primaryButton(won ? "Continue" : "Try Again");
        TextButton menuButton = theme.secondaryButton("Main Menu");
        UiActions.onClick(continueButton, () -> {
            if (won) {
                app.showChapterLevels(chapter);
            } else {
                app.showLevelBriefing(chapter, level);
            }
        });
        UiActions.onClick(menuButton, app::showMainMenu);
        panel.add(continueButton).width(260f).height(58f).padBottom(8f);
        panel.row();
        panel.add(menuButton).width(260f).height(52f);
        pauseLayer.add(panel);
        pauseLayer.setVisible(true);
    }

    private String packetArtId(PlantDefinition plant) {
        String normalized = plant.getKey().toUpperCase().replaceAll("[^A-Z0-9]", "");
        normalized = switch (normalized) {
            case "GOOPEASHOOTER" -> "POISONPEASHOOTER";
            case "MEGAGATLINGPEA" -> "MEGAGATLING";
            case "CHERRYBOMB" -> "CHERRY_BOMB";
            case "ICEBERGLETTUCE" -> "ICEBURG";
            case "PIERCEMINT" -> "SPEARMINT";
            default -> normalized;
        };
        String id = "IMAGE_UI_PACKETS_" + normalized;
        return theme.drawable(id) == null ? null : id;
    }

    @Override
    public void dispose() {
        boardActor.dispose();
        super.dispose();
    }

    private static final class PendingPlantFood {
        private final int col;
        private final int row;
        private final Plant plant;
        private final List<Zombie> zombiesBefore;
        private float elapsed;

        private PendingPlantFood(int col, int row, Plant plant, List<Zombie> zombiesBefore) {
            this.col = col;
            this.row = row;
            this.plant = plant;
            this.zombiesBefore = zombiesBefore;
        }
    }

    private enum IntroPhase {
        PAN_TO_ZOMBIES,
        SHOW_ZOMBIES,
        PAN_HOME,
        READY_DELAY,
        FINISHED
    }

    private enum ToolMode {
        PLANT,
        SHOVEL,
        PLANT_FOOD
    }
}
