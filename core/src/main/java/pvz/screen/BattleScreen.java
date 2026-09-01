package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
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
import pvz.app.AudioSettings;
import pvz.app.PvzAudio;
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
    private final Label missionLabel;
    private final Label notificationLabel;
    private final Table seedBank;
    private final Table missionBanner;
    private final Stack pauseLayer;
    private final BattleBoardActor boardActor;
    private final TextButton startWavesButton;
    private final AudioSettings audioSettings;

    private float modelAccumulator;
    private String selectedPlant;
    private ToolMode toolMode = ToolMode.PLANT;
    private boolean paused;
    private boolean resultShown;
    private float introElapsed;
    private IntroPhase introPhase = IntroPhase.PAN_TO_ZOMBIES;
    private PendingPlantFood pendingPlantFood;
    private float plantFoodImpactHold;
    private float missionElapsed;
    private float notificationElapsed;

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
        waveLabel = theme.heading("");
        status = theme.statusLabel();
        introLabel = theme.title("");
        introLabel.setAlignment(Align.center);
        missionLabel = theme.bodyLabel("Mission: " + level.getSpecialRuleSummary());
        missionLabel.setAlignment(Align.center);
        missionLabel.setWrap(true);
        notificationLabel = theme.heading("");
        notificationLabel.setColor(com.badlogic.gdx.graphics.Color.WHITE);
        notificationLabel.setEllipsis(true);
        seedBank = new Table();
        missionBanner = new Table();
        pauseLayer = new Stack();
        boardActor = new BattleBoardActor(app.assets(), controller, level,
            this::handleCellClick, this::handleCellHover);
        boardActor.setShowGrid(user.isGridVisible());
        startWavesButton = theme.tertiaryButton("START WAVES");
        audioSettings = app.audioSettings();
        buildUi();
        refreshSeedBank();
        refreshHud();
        consumeControllerEvents();
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
        hud.add(buildTopHud()).growX().height(178f);
        hud.row();
        hud.add().expand();
        hud.row();
        hud.add(buildBottomHud()).growX().height(60f).pad(0f, 16f, 6f, 16f);
        battle.add(hud);
        battle.add(buildIntroLayer());
        battle.add(buildMissionBanner());
        battle.add(buildNotificationLayer());
        return battle;
    }

    private Table buildMissionBanner() {
        missionBanner.setFillParent(true);
        missionBanner.top().padTop(122f);
        missionBanner.setTouchable(Touchable.disabled);
        Table panel = badgePanel(10f);
        panel.add(missionLabel).width(860f).height(58f);
        missionBanner.add(panel).width(890f).height(78f).center();
        return missionBanner;
    }

    private Table buildNotificationLayer() {
        Table layer = new Table();
        layer.setFillParent(true);
        layer.top().padTop(204f);
        layer.setTouchable(Touchable.disabled);
        Table panel = badgePanel(7f);
        panel.add(notificationLabel).width(610f).height(42f);
        layer.add(panel).width(640f).height(56f).center();
        layer.setVisible(false);
        return layer;
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
        if (user.isDebugMode()) {
            sunBadge.add(debugButton("+250", () -> addDebugSuns(250))).width(62f).height(32f)
                .padLeft(4f);
        }
        bar.add(sunBadge).height(58f).padRight(8f);

        seedBank.left();
        bar.add(seedBank).expandX().left();

        Table controls = new Table();
        Table tools = new Table();
        tools.add(toolButton("IMAGE_UI_HUD_INGAME_PLANTFOOD_BUTTON", this::selectPlantFood))
            .size(54f).padRight(6f);
        tools.add(toolButton("IMAGE_UI_HUD_INGAME_SHOVEL_BUTTON", this::selectShovel))
            .size(54f).padRight(6f);
        tools.add(toolButton("IMAGE_UI_HUD_INGAME_PAUSE_BUTTON", this::togglePause))
            .size(54f);
        controls.add(tools).right();
        if (user.isDebugMode()) {
            controls.row();
            controls.add(buildDebugControls()).right().padTop(5f);
        }
        bar.add(controls).right();
        return bar;
    }

    private Table buildDebugControls() {
        Table controls = new Table();
        controls.add(theme.settingsLabel("DEBUG")).padRight(4f);
        controls.add(debugButton("C +1000", () -> addDebugCurrency("coin", 1000)))
            .width(72f).height(32f).padRight(3f);
        controls.add(debugButton("G +50", () -> addDebugCurrency("gem", 50)))
            .width(64f).height(32f).padRight(3f);
        return controls;
    }

    private TextButton debugButton(String text, Runnable action) {
        TextButton button = theme.secondaryButton(text);
        button.getLabel().setFontScale(0.52f);
        UiActions.onClick(button, action);
        return button;
    }

    private void addDebugCurrency(String type, int amount) {
        showResult(app.services().game().addWalletCurrency(amount, type));
    }

    private void addDebugSuns(int amount) {
        showResult(app.services().game().addSuns(amount));
        refreshHud();
    }

    private void addDebugPlantFood() {
        showResult(app.services().game().addPlantFoodCheat());
        refreshHud();
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
        if (user.isDebugMode()) {
            bar.add(debugButton("+1", this::addDebugPlantFood)).width(48f).height(32f)
                .padLeft(3f);
        }

        startWavesButton.setVisible(false);
        UiActions.onClick(startWavesButton, this::startZombieWaves);
        bar.add(startWavesButton).width(170f).height(46f).padLeft(10f);

        bar.add(status).expandX().fillX().pad(0f, 12f, 0f, 12f);

        Table waveProgress = badgePanel(4f);
        Image meter = theme.image("IMAGE_UI_HUD_INGAME_PROGRESS_METER");
        if (meter != null) {
            meter.setScaling(Scaling.fit);
            waveProgress.add(meter).width(118f).height(34f).padRight(5f);
        }
        waveProgress.add(waveLabel).width(128f).right();
        bar.add(waveProgress).width(260f).height(42f).right();
        return bar;
    }

    private Stack buildPauseLayer() {
        pauseLayer.clearChildren();
        Table backdrop = new Table();
        backdrop.setFillParent(true);
        backdrop.setTouchable(Touchable.enabled);

        Table menu = theme.dialogPanel();
        menu.pad(24f, 32f, 22f, 32f);
        menu.add(theme.title("GAME PAUSED")).width(620f).height(58f).colspan(2).padBottom(14f);
        menu.row();
        Slider music = theme.audioSlider(audioSettings.getMusicVolume());
        Slider sfx = theme.audioSlider(audioSettings.getSfxVolume());
        music.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                audioSettings.setMusicVolume(music.getValue());
            }
        });
        sfx.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                audioSettings.setSfxVolume(sfx.getValue());
            }
        });
        addPauseAudioRow(menu, "Music", music);
        addPauseAudioRow(menu, "Sound FX", sfx);
        menu.row().padTop(16f);

        TextButton exitLevel = theme.tertiaryButton("EXIT LEVEL");
        TextButton restart = theme.tertiaryButton("RESTART");
        TextButton resume = theme.secondaryButton("RESUME");
        UiActions.onClick(exitLevel, () -> exitLevel());
        UiActions.onClick(restart, this::returnToPlantSelection);
        UiActions.onClick(resume, this::togglePause);
        Table actions = new Table();
        actions.add(exitLevel).width(190f).height(58f).padRight(10f);
        actions.add(restart).width(180f).height(58f).padRight(10f);
        actions.add(resume).width(180f).height(58f);
        menu.add(actions).width(580f).height(58f).colspan(2);

        Table popup = new Table();
        popup.setFillParent(true);
        popup.add(menu).width(700f).height(360f).center();
        pauseLayer.add(backdrop);
        pauseLayer.add(popup);
        pauseLayer.setVisible(false);
        return pauseLayer;
    }

    private void addPauseAudioRow(Table menu, String title, Slider slider) {
        menu.add(theme.settingsLabel(title)).width(130f).left().padRight(12f);
        menu.add(slider).width(430f).height(42f).left();
        menu.row();
    }

    private void exitLevel() {
        paused = false;
        app.showChapterLevels(chapter);
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
        float cardWidth = selectedPlants.size() > 6 ? 78f : 92f;
        float cardHeight = selectedPlants.size() > 6 ? 68f : 82f;
        int index = 0;
        for (String plantName : selectedPlants) {
            PlantDefinition definition = app.services().gameData().getPlantFactory()
                .findDefinition(plantName)
                .orElse(null);
            if (definition == null) {
                continue;
            }
            seedBank.add(seedCard(definition)).width(cardWidth).height(cardHeight).padRight(4f);
            index++;
            if (index % 4 == 0 && index < selectedPlants.size()) {
                seedBank.row();
            }
        }
    }

    private Stack seedCard(PlantDefinition definition) {
        Stack stack = new Stack();
        boolean active = toolMode == ToolMode.PLANT && definition.getName().equals(selectedPlant);
        String frameId = active ? "IMAGE_UI_PACKETS_SELECTED" : "IMAGE_UI_PACKETS_READY";
        Image frame = theme.image(frameId);
        if (frame != null) {
            frame.setScaling(Scaling.stretch);
            frame.setTouchable(Touchable.disabled);
            stack.add(frame);
        }

        String packetId = packetArtId(definition);
        Image packet = packetId == null ? null : theme.image(packetId);
        if (packet != null) {
            packet.setScaling(Scaling.fit);
            packet.setTouchable(Touchable.disabled);
            Table packetLayer = new Table();
            packetLayer.pad(7f, 8f, 14f, 8f);
            packetLayer.setTouchable(Touchable.disabled);
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
        cost.setTouchable(Touchable.disabled);
        cooldownLabel.setTouchable(Touchable.disabled);
        text.add(cost).expandX().left().padLeft(5f);
        text.add(cooldownLabel).right().padRight(4f);
        text.setTouchable(Touchable.disabled);
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
                boardActor.setPreviewPlant(app.services().gameData().getPlantFactory()
                    .createPlant(definition.getName()));
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
        String plantedPlant = selectedPlant;
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
                boardActor.clearPreviewPlant();
            }
        } else {
            theme.showError(status, "Choose a seed packet, shovel, plant food, or a sun first.");
            return;
        }
        showResult(result);
        if (result.isSuccessful() && isExplosivePlant(plantedPlant)) {
            app.audio().playSfx(PvzAudio.EXPLOSION_SOUND);
        }
        refreshSeedBank();
        refreshHud();
    }

    private void handleCellHover(int col, int row) {
        if (paused || isIntroRunning() || pendingPlantFood != null
            || game.getGameState() != GameState.RUNNING) {
            return;
        }
        if (collectSunIfPresent(col, row)) {
            refreshHud();
        }
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
        boardActor.clearPreviewPlant();
        theme.showSuccess(status, "Shovel selected. Click a planted tile.");
        refreshSeedBank();
    }

    private void selectPlantFood() {
        if (isIntroRunning() || pendingPlantFood != null) {
            return;
        }
        toolMode = ToolMode.PLANT_FOOD;
        selectedPlant = null;
        boardActor.clearPreviewPlant();
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
        boardActor.setAnimationPaused(paused);
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
        if (!paused) {
            missionElapsed += Math.max(0f, delta);
            if (missionElapsed >= 4.0f) {
                missionBanner.setVisible(false);
            }
            if (notificationElapsed > 0f) {
                notificationElapsed = Math.max(0f,
                    notificationElapsed - Math.max(0f, delta));
                if (notificationElapsed == 0f) {
                    notificationLabel.getParent().getParent().setVisible(false);
                }
            }
        }
        advanceIntro(delta);
        advancePendingPlantFood(delta);
        advanceModel(delta);
        consumeControllerEvents();
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
        int previousWave = game.getCurrentWave() == null
            ? 0 : game.getCurrentWave().getWaveNumber();
        int previousMowerKills = game.getLawnMowerKills();
        ActionResult result = controller.advanceTime(BASE_TICKS_PER_STEP * speed);
        if (!result.isSuccessful()) {
            theme.showError(status, result.getMessage());
        } else {
            if (game.getCurrentWave() != null
                && game.getCurrentWave().getWaveNumber() > previousWave) {
                app.audio().playSfx(PvzAudio.ZOMBIES_COMING_SOUND);
                app.audio().playSfx(PvzAudio.ZOMBIES_SOUND);
            }
            if (game.getLawnMowerKills() > previousMowerKills) {
                app.audio().playSfx(PvzAudio.LAWN_MOWER_SOUND);
            }
        }
        refreshSeedBank();
    }

    private void refreshHud() {
        sunLabel.setText(Integer.toString(game.getSunAmount()));
        plantFoodLabel.setText("x" + game.getPlantFoodCount());
        missionLabel.setText("Mission: " + level.getSpecialRuleSummary());
        Wave wave = game.getCurrentWave();
        int current = wave == null ? 0 : wave.getWaveNumber();
        if (game.areZombieWavesStarted() && wave == null
            && game.getElapsedTicks() < Game.INITIAL_PREPARATION_TICKS) {
            int remaining = (int) Math.ceil(
                (Game.INITIAL_PREPARATION_TICKS - game.getElapsedTicks())
                    / (double) Game.TICKS_PER_SECOND);
            waveLabel.setText("PREPARE " + remaining + "s");
        } else {
            waveLabel.setText("Wave " + current + " / " + level.getWaves().size());
        }
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
        if (game.areZombieWavesStarted() && game.getCurrentWave() == null) {
            theme.showSuccess(status, "Preparation phase: the first wave starts in 15 seconds.");
        } else if (game.areZombieWavesStarted()) {
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
        boardActor.setAnimationPaused(true);
        app.audio().playSfx(game.getGameState() == GameState.WON
            ? PvzAudio.WIN_SOUND : PvzAudio.LOSS_SOUND);
        showEndOverlay(game.getGameState() == GameState.WON);
    }

    private boolean isExplosivePlant(String plantName) {
        if (plantName == null) {
            return false;
        }
        String normalized = plantName.toLowerCase().replace("-", "").replace(" ", "");
        return normalized.contains("cherrybomb") || normalized.contains("jalapeno")
            || normalized.contains("squash") || normalized.contains("potatomine")
            || normalized.contains("explodeonut") || normalized.contains("grapeshot");
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
        panel.add(theme.settingsLabel("Exit cost: 0 coins")).padBottom(8f);
        panel.row();
        TextButton continueButton = theme.primaryButton(won ? "Continue" : "RESTART");
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

    private void consumeControllerEvents() {
        for (String event : controller.drainUiEvents()) {
            String notification = notificationFor(event);
            if (notification != null) {
                notificationLabel.setText(notification);
                notificationLabel.getParent().getParent().setVisible(true);
                notificationElapsed = 3.2f;
            }
        }
    }

    private String notificationFor(String event) {
        if (event == null) {
            return null;
        }
        String lower = event.toLowerCase();
        if (lower.contains("game started")) {
            return "GAME STARTED";
        }
        if (lower.contains("tornado")) {
            return "TORNADO ENTRY: ZOMBIE DROPPED AHEAD";
        }
        if (lower.contains("wave ") || lower.contains("final wave") || lower.contains("zombie waves started")) {
            return event.toUpperCase();
        }
        if (lower.contains("necromancy") || lower.contains("tomb")) {
            return "NECROMANCY! THE TOMBS ARE RISING";
        }
        if (lower.contains("dropped a plant food") || lower.contains("released a plant food")
            || lower.contains("plant foods now")) {
            return "PLANT FOOD COLLECTED";
        }
        if (lower.contains("coin")) {
            return "COINS COLLECTED";
        }
        if (lower.contains("diamond") || lower.contains("gem")) {
            return "DIAMONDS COLLECTED";
        }
        if (lower.contains(" dropped a pot") || lower.contains(" pots now")) {
            return "POT COLLECTED";
        }
        if (lower.contains("cold wind") || level.getSeason() == model.SeasonType.BIG_WAVE_BEACH
            && lower.contains("wave")) {
            return "BIG WAVE BEACH: THE TIDE IS RISING";
        }
        return null;
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
