package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import controller.ActionResult;
import controller.SettingsController;
import pvz.PvzApplication;
import pvz.app.AudioSettings;
import pvz.app.DisplaySettings;
import pvz.ui.UiTheme;

public final class SettingsScreen extends AuthenticatedUiScreen {
    private static final float PANEL_WIDTH = 960f;
    private static final float PANEL_HEIGHT = 660f;
    private static final float CARD_WIDTH = 406f;
    private static final float CARD_HEIGHT = 318f;

    private final SettingsController controller;
    private final DisplaySettings displaySettings;
    private final AudioSettings audioSettings;

    private final Label difficultyValue;
    private final Label speedValue;
    private final Label displayValue;
    private final Label musicValue;
    private final Label sfxValue;
    private final Label status;

    private final Slider musicSlider;
    private final Slider sfxSlider;
    private final CheckBox gridBox;
    private final CheckBox debugBox;
    private final CheckBox fullscreenBox;
    private final CheckBox vsyncBox;

    public SettingsScreen(PvzApplication app) {
        super(app);
        controller = app.services().settings();
        displaySettings = app.displaySettings();
        audioSettings = app.audioSettings();

        difficultyValue = settingsValue("");
        speedValue = settingsValue("");
        displayValue = settingsValue("");
        musicValue = settingsValue("");
        sfxValue = settingsValue("");
        status = statusLabel();
        status.setWrap(false);

        musicSlider = theme.audioSlider(audioSettings.getMusicVolume());
        sfxSlider = theme.audioSlider(audioSettings.getSfxVolume());
        gridBox = theme.checkBox(" Show battle grid", user.isGridVisible());
        debugBox = theme.checkBox(" Enable debug controls", user.isDebugMode());
        fullscreenBox = theme.checkBox(" Fullscreen", displaySettings.isFullscreen());
        vsyncBox = theme.checkBox(" VSync", displaySettings.isVsync());

        bindActions();
        buildUi();
        refreshValues();
    }

    private void buildUi() {
        Table panel = theme.dialogPanel();
        panel.top();

        panel.add(buildHeader()).width(860f).height(54f).padBottom(8f);
        panel.row();

        panel.add(buildAudioCard()).width(860f).height(142f).padBottom(10f);
        panel.row();

        Table cards = new Table();
        cards.add(buildGameplayCard()).width(CARD_WIDTH).height(CARD_HEIGHT).padRight(16f);
        cards.add(buildDisplayCard()).width(CARD_WIDTH).height(CARD_HEIGHT);
        panel.add(cards).width(828f).height(CARD_HEIGHT);
        panel.row();

        panel.add(status).width(820f).height(28f).padTop(5f);
        panel.row();

        Table actions = new Table();
        TextButton cheats = theme.primaryButton("Cheat Codes");
        TextButton reset = theme.tertiaryButton("Reset Defaults");
        TextButton back = theme.secondaryButton("Back");
        UiActions.onClick(cheats, app::showCheats);
        UiActions.onClick(reset, this::resetDefaults);
        UiActions.onClick(back, app::showMainMenu);
        actions.add(cheats).width(190f).height(48f).padRight(10f);
        actions.add(reset).width(220f).height(48f).padRight(10f);
        actions.add(back).width(180f).height(48f);
        panel.add(actions).padTop(3f);

        root.add(panel).width(PANEL_WIDTH).height(PANEL_HEIGHT).center();
    }

    private Table buildHeader() {
        Table header = new Table();
        header.add(theme.settingsTitle("Settings")).expandX().center();
        header.add(currencyBadge(UiTheme.COIN_ICON, user.getWallet().getCoins()))
            .width(112f).height(42f).padLeft(10f).padRight(6f);
        header.add(currencyBadge(UiTheme.GEM_ICON, user.getWallet().getGems()))
            .width(112f).height(42f);
        return header;
    }

    private Table currencyBadge(String iconId, int value) {
        Table badge = theme.settingsBadgePanel(4f);
        Image icon = theme.image(iconId);
        if (icon != null) {
            badge.add(icon).size(26f).padRight(4f);
        }
        badge.add(theme.settingsLabel(Integer.toString(value))).center();
        return badge;
    }

    private Table buildAudioCard() {
        Table card = theme.settingsCardPanel(12f);
        card.left();
        card.add(theme.settingsLabel("AUDIO")).left().padBottom(4f).colspan(3);
        card.row();
        addAudioRow(card, "Music", musicSlider, musicValue);
        addAudioRow(card, "Sound FX", sfxSlider, sfxValue);
        return card;
    }

    private void addAudioRow(Table card, String title, Slider slider, Label value) {
        card.add(theme.settingsLabel(title)).width(150f).left().padLeft(8f);
        card.add(slider).width(525f).height(44f).left().padLeft(8f).padRight(10f);
        card.add(value).width(70f).right().padRight(8f);
        card.row();
    }

    private Table buildGameplayCard() {
        Table card = theme.settingsCardPanel(14f);
        card.top().left();
        card.add(theme.settingsLabel("GAMEPLAY")).left().padBottom(7f).colspan(2);
        card.row();

        addNumberSetting(
            card,
            "Difficulty",
            "1 Easy   2 Normal   3 Hard   4 Expert   5 Nightmare",
            1,
            5,
            difficultyValue,
            false,
            this::changeDifficulty
        );

        addNumberSetting(
            card,
            "Game speed",
            "Battle simulation speed",
            1,
            3,
            speedValue,
            true,
            this::changeGameSpeed
        );

        card.add(gridBox).left().height(34f).colspan(2);
        card.row();
        card.add(debugBox).left().height(34f).colspan(2);
        return card;
    }

    private Table buildDisplayCard() {
        Table card = theme.settingsCardPanel(14f);
        card.top().left();
        card.add(theme.settingsLabel("DISPLAY")).left().padBottom(6f).colspan(2);
        card.row();

        displayValue.setAlignment(Align.left);
        card.add(displayValue).width(372f).height(30f).left().colspan(2).padBottom(5f);
        card.row();

        Table toggles = new Table();
        toggles.left();
        toggles.add(fullscreenBox).width(178f).left();
        toggles.add(vsyncBox).width(145f).left();
        card.add(toggles).left().colspan(2).padBottom(6f);
        card.row();

        card.add(theme.settingsLabel("Window size")).left().padBottom(4f).colspan(2);
        card.row();
        card.add(resolutionButtons()).left().colspan(2).padBottom(7f);
        card.row();

        card.add(theme.settingsLabel("Quick presets")).left().padBottom(4f).colspan(2);
        card.row();
        card.add(presetButtons()).left().colspan(2);
        return card;
    }

    private Table resolutionButtons() {
        Table buttons = new Table();
        buttons.left();
        addResolutionButton(buttons, "1280x720", 1280, 720);
        addResolutionButton(buttons, "1600x900", 1600, 900);
        buttons.row().padTop(4f);
        addResolutionButton(buttons, "1366x768", 1366, 768);
        addResolutionButton(buttons, "1920x1080", 1920, 1080);
        return buttons;
    }

    private Table presetButtons() {
        Table buttons = new Table();
        TextButton balanced = theme.primaryButton("Balanced");
        TextButton cinematic = theme.tertiaryButton("Cinematic");
        UiActions.onClick(balanced, this::applyBalancedPreset);
        UiActions.onClick(cinematic, this::applyCinematicPreset);
        buttons.add(balanced).width(162f).height(42f).padRight(7f);
        buttons.add(cinematic).width(170f).height(42f);
        return buttons;
    }

    private void addNumberSetting(
        Table table,
        String title,
        String description,
        int from,
        int to,
        Label currentValue,
        boolean speedLabels,
        IntAction action
    ) {
        Table header = new Table();
        header.add(theme.settingsLabel(title)).left();
        header.add().expandX();
        header.add(theme.settingsLabel("Current:")).padRight(5f);
        header.add(currentValue).width(52f).right();
        table.add(header).width(370f).left().colspan(2);
        table.row();

        Label descriptionLabel = theme.settingsLabel(description);
        table.add(descriptionLabel).width(370f).height(28f).left().colspan(2).padBottom(3f);
        table.row();

        Table buttons = new Table();
        buttons.left();
        for (int value = from; value <= to; value++) {
            int selected = value;
            String text = speedLabels ? value + "x" : Integer.toString(value);
            TextButton button = theme.primaryButton(text);
            UiActions.onClick(button, () -> action.run(selected));
            buttons.add(button).width(speedLabels ? 94f : 59f).height(40f).padRight(5f);
        }
        table.add(buttons).left().colspan(2).padBottom(7f);
        table.row();
    }

    private void addResolutionButton(Table table, String text, int width, int height) {
        TextButton button = theme.secondaryButton(text);
        UiActions.onClick(button, () -> changeResolution(width, height));
        table.add(button).width(164f).height(40f).padRight(6f);
    }

    private Label settingsValue(String text) {
        Label label = theme.settingsLabel(text);
        label.setAlignment(Align.right);
        return label;
    }

    private void bindActions() {
        musicSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                audioSettings.setMusicVolume(musicSlider.getValue());
                refreshAudioValues();
            }
        });
        sfxSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                audioSettings.setSfxVolume(sfxSlider.getValue());
                refreshAudioValues();
            }
        });
        UiActions.onClick(gridBox, () -> changeGrid(gridBox.isChecked()));
        UiActions.onClick(debugBox, () -> changeDebug(debugBox.isChecked()));
        UiActions.onClick(fullscreenBox, () -> changeFullscreen(fullscreenBox.isChecked()));
        UiActions.onClick(vsyncBox, () -> changeVsync(vsyncBox.isChecked()));
    }

    private void changeDifficulty(int value) {
        handleResult(controller.changeDifficulty(value));
        refreshValues();
    }

    private void changeGameSpeed(int value) {
        handleResult(controller.changeGameSpeed(value));
        refreshValues();
    }

    private void changeGrid(boolean visible) {
        handleResult(controller.changeGridVisible(visible));
    }

    private void changeDebug(boolean enabled) {
        handleResult(controller.changeDebugMode(enabled));
    }

    private void changeFullscreen(boolean enabled) {
        displaySettings.setFullscreen(enabled);
        refreshValues();
        showSuccess(enabled ? "Fullscreen enabled." : "Windowed mode enabled.");
    }

    private void changeVsync(boolean enabled) {
        displaySettings.setVsync(enabled);
        refreshValues();
        showSuccess(enabled ? "VSync enabled." : "VSync disabled.");
    }

    private void changeResolution(int width, int height) {
        displaySettings.setWindowSize(width, height);
        fullscreenBox.setChecked(false);
        refreshValues();
        showSuccess("Resolution changed to " + width + " x " + height + ".");
    }

    private void applyBalancedPreset() {
        controller.changeDifficulty(3);
        controller.changeGameSpeed(1);
        controller.changeGridVisible(false);
        controller.changeDebugMode(false);
        displaySettings.setVsync(true);
        audioSettings.setMusicVolume(0.80f);
        audioSettings.setSfxVolume(0.90f);
        syncControls();
        refreshValues();
        showSuccess("Balanced preset applied.");
    }

    private void applyCinematicPreset() {
        controller.changeGameSpeed(1);
        controller.changeGridVisible(false);
        controller.changeDebugMode(false);
        displaySettings.setVsync(true);
        audioSettings.setMusicVolume(1f);
        audioSettings.setSfxVolume(0.80f);
        syncControls();
        refreshValues();
        showSuccess("Cinematic preset applied.");
    }

    private void resetDefaults() {
        controller.changeDifficulty(3);
        controller.changeGameSpeed(1);
        controller.changeGridVisible(false);
        controller.changeDebugMode(false);
        displaySettings.resetDefaults();
        audioSettings.resetDefaults();
        syncControls();
        refreshValues();
        showSuccess("All settings reset to defaults.");
    }

    private void syncControls() {
        gridBox.setChecked(user.isGridVisible());
        debugBox.setChecked(user.isDebugMode());
        fullscreenBox.setChecked(displaySettings.isFullscreen());
        vsyncBox.setChecked(displaySettings.isVsync());
        musicSlider.setValue(audioSettings.getMusicVolume());
        sfxSlider.setValue(audioSettings.getSfxVolume());
    }

    private void refreshValues() {
        difficultyValue.setText(Integer.toString(user.getDifficultyLevel()));
        speedValue.setText(user.getGameSpeed() + "x");
        displayValue.setText(displaySettings.currentModeText());
        fullscreenBox.setChecked(displaySettings.isFullscreen());
        vsyncBox.setChecked(displaySettings.isVsync());
        refreshAudioValues();
    }

    private void refreshAudioValues() {
        musicValue.setText(Math.round(audioSettings.getMusicVolume() * 100f) + "%");
        sfxValue.setText(Math.round(audioSettings.getSfxVolume() * 100f) + "%");
    }

    private void handleResult(ActionResult result) {
        if (result.isSuccessful()) {
            showSuccess(result.getMessage());
        } else {
            theme.showError(status, result.getMessage());
        }
    }

    private void showSuccess(String message) {
        theme.showSuccess(status, message);
    }

    @FunctionalInterface
    private interface IntAction {
        void run(int value);
    }
}
