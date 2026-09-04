package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import controller.ActionResult;
import model.Greenhouse;
import model.GreenhouseSlot;
import model.PlantDefinition;
import pvz.PvzApplication;
import pvz.ui.PlantAnimationActor;
import pvz.ui.PlantArtResolver;
import pvz.ui.UiTheme;

import java.util.Optional;

public final class GreenhouseScreen extends AuthenticatedUiScreen {
    private static final String ZEN_BACKGROUND = "IMAGE_BACKGROUNDS_ZEN_GARDEN";
    private static final String LOCK_ICON = "IMAGE_ZEN_GARDEN_LOCKED_POT_ICON";
    private static final String POT_ICON =
        "IMAGE_ZEN_GARDEN_GROWING_PLANT_SLOT_GROWING_PLANT_SLOT_176X106";
    private static final String SPROUT_ICON = "IMAGE_UI_HUD_INGAME_SPROUT_ICON_NOPLUS";
    private static final String PLANT_FOOD_ICON =
        "IMAGE_UI_ALMANAC_ALMANAC_STAT_ICON_PLANTFOOD_LARGE";
    private static final String MARIGOLD_PACKET = "IMAGE_UI_PACKETS_MARIGOLD";

    private static final float SLOT_WIDTH = 190f;
    private static final float SLOT_HEIGHT = 136f;
    private static final float GRID_WIDTH = 830f;
    private static final float DETAIL_WIDTH = 300f;

    private int selectedX = 1;
    private int selectedY = 1;
    private String message = "Select a greenhouse slot.";
    private boolean messageSuccess = true;
    private Boolean selectedReadyState;
    private String renderedSlotState;
    private Label liveStateLabel;
    private Label liveCostLabel;
    private boolean actionInFlight;

    public GreenhouseScreen(PvzApplication app) {
        super(app);
        Drawable zen = theme.drawable(ZEN_BACKGROUND);
        if (zen != null) {
            root.setBackground(zen);
        }
        buildUi();
    }

    @Override
    protected void handleEscape() {
        app.showAdventure();
    }

    private void buildUi() {
        root.clearChildren();
        GreenhouseSlot selected = user.getGreenhouse().getSlot(selectedX, selectedY);
        selectedReadyState = selected.isEmpty()
            ? null : selected.isReady(System.currentTimeMillis());
        liveStateLabel = null;
        liveCostLabel = null;

        Table screen = new Table();
        screen.top();
        screen.pad(32f, 42f, 24f, 42f);

        screen.add(titleBar("GREENHOUSE"))
            .width(1180f)
            .height(54f)
            .padBottom(8f);
        screen.row();

        screen.add(buildInventoryBar())
            .width(1180f)
            .height(52f)
            .padBottom(8f);
        screen.row();

        Table body = new Table();
        body.add(buildGrid())
            .width(GRID_WIDTH)
            .height(465f)
            .left();
        body.add(buildDetail())
            .width(DETAIL_WIDTH)
            .height(465f)
            .padLeft(14f)
            .right();
        screen.add(body).width(1180f).height(465f);
        screen.row().padTop(8f);

        screen.add(buildFooter()).width(1180f).height(54f);
        addScrollable(screen);
        renderedSlotState = slotSnapshot(System.currentTimeMillis());
    }

    private Table buildInventoryBar() {
        Table bar = theme.settingsBadgePanel(7f);
        bar.add(icon(UiTheme.GREENHOUSE_ICON, 30f));
        bar.add(theme.settingsLabel(
            "Unlocked " + user.getGreenhouse().getUnlockedSlotCount()
                + " / " + Greenhouse.MAX_SLOTS
        )).padLeft(6f).padRight(24f);

        bar.add(icon(SPROUT_ICON, 30f));
        bar.add(theme.settingsLabel("Pots " + user.getInventory().getPots()))
            .padLeft(6f)
            .padRight(24f);

        bar.add(icon(PLANT_FOOD_ICON, 30f));
        bar.add(theme.settingsLabel("Plant Food " + user.getInventory().getPlantFoods()))
            .padLeft(6f);
        bar.add().expandX();

        Label hint = theme.settingsLabel("2h Marigold  |  8h Plant");
        bar.add(hint).right();
        return bar;
    }

    private Table buildGrid() {
        Table panel = theme.settingsCardPanel(12f);
        Table grid = new Table();
        long now = System.currentTimeMillis();

        for (int y = 1; y <= Greenhouse.ROWS; y++) {
            for (int x = 1; x <= Greenhouse.COLUMNS; x++) {
                GreenhouseSlot slot = user.getGreenhouse().getSlot(x, y);
                grid.add(slotButton(slot, now))
                    .width(SLOT_WIDTH)
                    .height(SLOT_HEIGHT)
                    .pad(4f);
            }
            grid.row();
        }
        panel.add(grid).center();
        return panel;
    }

    private Button slotButton(GreenhouseSlot slot, long now) {
        Button.ButtonStyle style = new Button.ButtonStyle();
        Drawable background = theme.drawable(UiTheme.MAIN_MENU_TILE);
        if (background == null) {
            background = theme.skin().getDrawable("image_ui_dialog_asset_inner_bkgd_10");
        }
        style.up = background;
        style.down = background;

        Button button = new Button(style);
        button.add(slotContent(slot, now)).grow();
        UiActions.onClick(button, () -> selectSlot(slot));
        return button;
    }

    private Stack slotContent(GreenhouseSlot slot, long now) {
        Stack stack = new Stack();
        Stack artLayer = new Stack();
        Table textLayer = new Table();
        textLayer.bottom();

        Image pot = theme.imageOrFallback(POT_ICON);
        pot.setScaling(Scaling.fit);
        artLayer.add(pot);

        if (!slot.isUnlocked()) {
            Image lock = theme.imageOrFallback(LOCK_ICON);
            lock.setScaling(Scaling.fit);
            artLayer.add(lock);
            textLayer.add(theme.settingsLabel("LOCKED")).center().padBottom(5f);
        } else if (slot.isEmpty()) {
            Label empty = greenhouseLabel("EMPTY");
            textLayer.add(empty).width(146f).center().padBottom(5f);
        } else {
            addPlantIdleArt(artLayer, slot.getPlantName());
            Label name = theme.settingsLabel(slot.getPlantName());
            name.setAlignment(Align.center);
            name.setWrap(true);
            name.setFontScale(0.68f);
            textLayer.add(name).width(146f).center();
            textLayer.row();
            Label state = theme.settingsLabel(slotState(slot, now));
            state.setAlignment(Align.center);
            state.setWrap(true);
            state.setFontScale(0.64f);
            textLayer.add(state).width(146f).center().padBottom(3f);
        }

        if (slot.getX() == selectedX && slot.getY() == selectedY) {
            Table selection = new Table();
            selection.setBackground(theme.drawable(UiTheme.DIFFICULTY_BG));
            stack.add(selection);
        }
        stack.add(artLayer);
        stack.add(textLayer);
        return stack;
    }

    private Table buildDetail() {
        GreenhouseSlot slot = user.getGreenhouse().getSlot(selectedX, selectedY);
        Table panel = theme.settingsCardPanel(14f);
        panel.top();

        panel.add(theme.settingsTitle("SLOT " + selectedX + ", " + selectedY))
            .center()
            .padBottom(8f);
        panel.row();

        if (!slot.isUnlocked()) {
            detailLocked(panel);
        } else if (slot.isEmpty()) {
            detailEmpty(panel);
        } else {
            detailPlant(panel, slot);
        }
        return panel;
    }

    private void detailLocked(Table panel) {
        addImage(panel, LOCK_ICON, 88f);
        panel.row().padTop(10f);
        panel.add(theme.heading("Locked Pot")).center();
        panel.row().padTop(8f);
        panel.add(wrapped("Buy a Pot upgrade in the Shop to unlock the next slot."))
            .width(260f);
        panel.row().padTop(16f);
        TextButton shop = theme.tertiaryButton("Open Shop");
        UiActions.onClick(shop, app::showShop);
        panel.add(shop).width(220f).height(52f);
    }

    private void detailEmpty(Table panel) {
        addImage(panel, POT_ICON, 108f);
        panel.row().padTop(8f);
        panel.add(theme.heading("Empty Pot")).center();
        panel.row().padTop(8f);
        panel.add(wrapped(
            "Planting consumes one inventory pot. The result is Marigold or an owned plant."
        )).width(260f);
        panel.row().padTop(14f);

        Label pots = theme.settingsLabel("Available pots: " + user.getInventory().getPots());
        panel.add(pots).center();
        panel.row().padTop(10f);

        TextButton plant = theme.primaryButton("Plant Pot");
        plant.setDisabled(user.getInventory().getPots() <= 0);
        UiActions.onClick(plant, this::plantSelected);
        panel.add(plant).width(220f).height(54f);
    }

    private void detailPlant(Table panel, GreenhouseSlot slot) {
        long now = System.currentTimeMillis();
        addPlantPreview(panel, slot.getPlantName());
        panel.row().padTop(4f);
        Label plantName = theme.heading(slot.getPlantName());
        plantName.setWrap(true);
        plantName.setFontScale(0.82f);
        panel.add(plantName).width(280f).height(34f).center();
        panel.row().padTop(7f);

        String kind = slot.isMarigold() ? "Marigold reward" : "Stored battle boost";
        panel.add(theme.settingsLabel(kind)).center();
        panel.row().padTop(6f);

        if (slot.isReady(now)) {
            liveStateLabel = greenhouseLabel("READY TO HARVEST");
            panel.add(liveStateLabel).center();
            panel.row().padTop(12f);
            TextButton collect = theme.primaryButton(
                slot.isMarigold() ? "Collect 500 Coins" : "Collect Boost"
            );
            UiActions.onClick(collect, this::collectSelected);
            panel.add(collect).width(245f).height(54f);
            return;
        }

        long remaining = slot.remainingMillis(now);
        liveStateLabel = greenhouseLabel("Remaining: " + formatDuration(remaining));
        panel.add(liveStateLabel).center();
        panel.row().padTop(6f);
        int gems = growthCost(remaining);
        liveCostLabel = greenhouseLabel("Finish now: " + gems + " gem(s)");
        panel.add(liveCostLabel).center();
        panel.row().padTop(12f);
        TextButton grow = theme.tertiaryButton("Speed Up");
        grow.setDisabled(user.getWallet().getGems() < gems);
        UiActions.onClick(grow, this::growSelected);
        panel.add(grow).width(220f).height(54f);
    }

    private Table buildFooter() {
        Table footer = new Table();
        Label status = theme.statusLabel();
        status.setWrap(true);
        status.setAlignment(Align.left);
        status.setFontScale(0.78f);
        if (messageSuccess) {
            theme.showSuccess(status, message);
        } else {
            theme.showError(status, message);
        }

        TextButton shop = theme.tertiaryButton("Shop");
        TextButton back = theme.secondaryButton("Back to Adventure");
        UiActions.onClick(shop, app::showShop);
        UiActions.onClick(back, app::showAdventure);

        footer.add(status).width(660f).left();
        footer.add().expandX();
        footer.add(shop).width(150f).height(48f).padRight(8f);
        footer.add(back).width(220f).height(48f);
        return footer;
    }

    private void selectSlot(GreenhouseSlot slot) {
        selectedX = slot.getX();
        selectedY = slot.getY();
        message = "Selected slot (" + selectedX + ", " + selectedY + ").";
        messageSuccess = true;
        buildUi();
    }

    private void plantSelected() {
        if (actionInFlight) {
            return;
        }
        actionInFlight = true;
        try {
            apply(app.services().greenhouse().plantPot(selectedX, selectedY));
        } finally {
            actionInFlight = false;
        }
    }

    private void collectSelected() {
        if (actionInFlight) {
            return;
        }
        actionInFlight = true;
        try {
            apply(app.services().greenhouse().collect(selectedX, selectedY));
        } finally {
            actionInFlight = false;
        }
    }

    private void growSelected() {
        if (actionInFlight) {
            return;
        }
        actionInFlight = true;
        try {
            apply(app.services().greenhouse().grow(selectedX, selectedY));
        } finally {
            actionInFlight = false;
        }
    }

    private void apply(ActionResult result) {
        message = result.getMessage();
        messageSuccess = result.isSuccessful();
        buildUi();
    }

    private void addPlantPreview(Table panel, String plantName) {
        Optional<PlantDefinition> definition = app.services()
            .gameData()
            .getPlantFactory()
            .findDefinition(plantName);
        if (definition.isEmpty()) {
            addImage(panel, MARIGOLD_PACKET, 110f);
            return;
        }

        Stack preview = new Stack();
        Image packet = PlantArtResolver.packetImage(theme, definition.get());
        if (packet != null) {
            Table fallback = new Table();
            fallback.add(packet).size(112f);
            preview.add(fallback);
        }
        PlantAnimationActor animation = new PlantAnimationActor(app.assets(), definition.get());
        if (animation.hasAnimation()) {
            animation.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
            preview.add(animation);
        }
        panel.add(preview).size(160f, 130f).center();
    }

    private void addPlantIdleArt(Stack stack, String plantName) {
        Optional<PlantDefinition> definition = app.services()
            .gameData()
            .getPlantFactory()
            .findDefinition(plantName);
        if (definition.isPresent()) {
            PlantAnimationActor animation = new PlantAnimationActor(app.assets(), definition.get());
            if (animation.hasAnimation()) {
                animation.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
                stack.add(animation);
                return;
            }
            Image packet = PlantArtResolver.packetImage(theme, definition.get());
            if (packet != null) {
                packet.setScaling(Scaling.fit);
                stack.add(packet);
                return;
            }
        }
        stack.add(theme.imageOrFallback(MARIGOLD_PACKET));
    }

    private void addImage(Table table, String id, float size) {
        table.add(theme.imageOrFallback(id)).size(size);
    }

    private Image icon(String id, float size) {
        Image image = theme.imageOrFallback(id);
        image.setSize(size, size);
        return image;
    }

    private Label wrapped(String text) {
        Label label = theme.bodyLabel(text);
        label.setFontScale(0.78f);
        label.setAlignment(Align.center);
        return label;
    }

    private Label greenhouseLabel(String text) {
        Label label = theme.settingsLabel(text);
        label.setAlignment(Align.center);
        label.setWrap(true);
        label.setFontScale(0.78f);
        return label;
    }

    private String slotState(GreenhouseSlot slot, long now) {
        if (slot.isReady(now)) {
            return "READY";
        }
        return formatDuration(slot.remainingMillis(now));
    }

    private int growthCost(long remainingMillis) {
        return (int) Math.ceil(remainingMillis / 3_600_000.0);
    }

    private String formatDuration(long millis) {
        long totalMinutes = (millis + 59_999L) / 60_000L;
        long hours = totalMinutes / 60L;
        long minutes = totalMinutes % 60L;
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return Math.max(1L, minutes) + "m";
    }

    @Override
    public void render(float delta) {
        String currentSlotState = slotSnapshot(System.currentTimeMillis());
        if (!currentSlotState.equals(renderedSlotState)) {
            buildUi();
            return;
        }
        refreshSelectedPlantDetails();
        super.render(delta);
    }

    private String slotSnapshot(long now) {
        StringBuilder snapshot = new StringBuilder();
        for (GreenhouseSlot slot : user.getGreenhouse().getSlots()) {
            snapshot.append(slot.getX()).append(':').append(slot.getY()).append('=')
                .append(slot.status(now)).append(';');
        }
        return snapshot.toString();
    }

    private void refreshSelectedPlantDetails() {
        GreenhouseSlot slot = user.getGreenhouse().getSlot(selectedX, selectedY);
        if (slot.isEmpty()) {
            if (selectedReadyState != null) {
                buildUi();
            }
            return;
        }

        long now = System.currentTimeMillis();
        boolean ready = slot.isReady(now);
        if (selectedReadyState == null || ready != selectedReadyState) {
            buildUi();
            return;
        }
        if (liveStateLabel != null) {
            liveStateLabel.setText(ready
                ? "READY TO HARVEST" : "Remaining: " + formatDuration(slot.remainingMillis(now)));
        }
        if (liveCostLabel != null && !ready) {
            liveCostLabel.setText("Finish now: " + growthCost(slot.remainingMillis(now)) + " gem(s)");
        }
    }
}
