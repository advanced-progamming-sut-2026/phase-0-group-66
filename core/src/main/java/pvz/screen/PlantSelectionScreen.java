package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import controller.ActionResult;
import model.Chapter;
import model.Game;
import model.Level;
import model.PlantAbility;
import model.PlantDefinition;
import model.PlantFoodType;
import pvz.PvzApplication;
import pvz.ui.PlantAnimationActor;
import pvz.ui.PlantPacketCard;
import pvz.ui.PlantPacketCard.State;
import pvz.ui.UiTheme;

import java.util.List;
import java.util.Set;

public final class PlantSelectionScreen extends AuthenticatedUiScreen {
    private static final float CATALOG_CARD_WIDTH = 136f;
    private static final float CATALOG_CARD_HEIGHT = 142f;
    private static final float SLOT_WIDTH = 108f;
    private static final float SLOT_HEIGHT = 82f;

    private final Chapter chapter;
    private final Level level;
    private final Label status;

    private String focusedPlant;
    private ScrollPane catalogScroll;
    private float savedScrollY;
    private Set<String> availablePlantNames = Set.of();

    public PlantSelectionScreen(PvzApplication app, Chapter chapter, Level level) {
        super(app);
        this.chapter = chapter;
        this.level = level;
        status = statusLabel();
        List<String> selected = app.services().game().getSelectedPlants();
        if (!selected.isEmpty()) {
            focusedPlant = selected.get(0);
        } else {
            List<String> available = app.services().game().getAvailablePlants();
            if (!available.isEmpty()) {
                focusedPlant = available.get(0);
            }
        }
        buildUi();
    }

    private void buildUi() {
        availablePlantNames = Set.copyOf(app.services().game().getAvailablePlants());
        Table screen = new Table();
        screen.top();
        screen.pad(18f, 20f, 18f, 20f);
        screen.add(buildHeader()).growX();
        screen.row();
        screen.add(buildContent()).grow().padTop(8f);
        screen.row();
        screen.add(buildFooter()).growX().padTop(7f);
        root.add(screen).grow();
        restoreCatalogScroll();
    }

    private Table buildHeader() {
        Table header = new Table();
        header.add(theme.title("CHOOSE YOUR PLANTS")).expandX().left();

        Table levelBadge = badgePanel(7f);
        levelBadge.add(theme.settingsLabel(chapter.getName() + "  •  Level " + level.getLevelNumber()));
        header.add(levelBadge).height(42f).padRight(8f);

        Table gemBadge = badgePanel(5f);
        Image gem = theme.image(UiTheme.GEM_ICON);
        if (gem != null) {
            gemBadge.add(gem).size(28f).padRight(4f);
        }
        gemBadge.add(theme.heading(Integer.toString(user.getWallet().getGems())));
        header.add(gemBadge).width(110f).height(42f);
        return header;
    }

    private Table buildContent() {
        Table content = new Table();
        content.add(buildSelectedPanel()).width(246f).growY().padRight(8f);
        content.add(buildCatalogPanel()).width(746f).growY().padRight(8f);
        content.add(buildDetailsPanel()).width(238f).growY();
        return content;
    }

    private Table buildSelectedPanel() {
        Table panel = cardPanel(10f);
        panel.top();

        int selectedCount = app.services().game().getSelectedPlants().size();
        panel.add(theme.settingsTitle("YOUR TEAM")).colspan(2).padBottom(2f);
        panel.row();
        Label slots = theme.settingsLabel(selectedCount + " / " + level.getAllowedPlantCount() + " selected");
        slots.setAlignment(Align.center);
        panel.add(slots).colspan(2).height(26f).padBottom(5f);
        panel.row();

        List<String> selected = app.services().game().getSelectedPlants();
        int maximum = level.getAllowedPlantCount();
        for (int index = 0; index < maximum; index++) {
            if (index < selected.size()) {
                PlantDefinition plant = findPlant(selected.get(index));
                if (plant != null) {
                    PlantPacketCard card = createCard(plant, true);
                    UiActions.onClick(card, () -> focusOrToggle(plant));
                    panel.add(card).width(SLOT_WIDTH).height(SLOT_HEIGHT).pad(3f);
                } else {
                    panel.add(emptySlot()).width(SLOT_WIDTH).height(SLOT_HEIGHT).pad(3f);
                }
            } else {
                panel.add(emptySlot()).width(SLOT_WIDTH).height(SLOT_HEIGHT).pad(3f);
            }
            if (index % 2 == 1) {
                panel.row();
            }
        }
        if (maximum % 2 == 1) {
            panel.row();
        }

        panel.add().expandY().colspan(2);
        panel.row();
        status.setAlignment(Align.center);
        status.setWrap(true);
        panel.add(status).colspan(2).width(218f).height(54f).padTop(5f);
        return panel;
    }

    private Table emptySlot() {
        Table slot = new Table();
        Image empty = theme.image("IMAGE_UI_PACKETS_EMPTY_PACKET");
        if (empty != null) {
            slot.add(empty).grow();
        } else {
            slot.setBackground(theme.skin().getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        }
        return slot;
    }

    private Table buildCatalogPanel() {
        Table panel = cardPanel(8f);
        panel.top();
        Table title = new Table();
        title.add(theme.settingsTitle("PLANT COLLECTION")).expandX().left();
        Label hint = theme.settingsLabel("Click a plant to add/remove it");
        hint.setFontScale(0.76f);
        title.add(hint).right();
        panel.add(title).growX().pad(0f, 5f, 5f, 5f);
        panel.row();

        Table grid = new Table();
        grid.top().left();
        List<PlantDefinition> plants = app.services().gameData().getPlantFactory().getAllDefinitions();
        int column = 0;
        for (PlantDefinition plant : plants) {
            PlantPacketCard card = createCard(plant, false);
            UiActions.onClick(card, () -> focusOrToggle(plant));
            grid.add(card)
                .width(CATALOG_CARD_WIDTH)
                .height(CATALOG_CARD_HEIGHT)
                .pad(4f);
            column++;
            if (column == 5) {
                grid.row();
                column = 0;
            }
        }

        catalogScroll = new ScrollPane(grid, theme.skin());
        catalogScroll.setFadeScrollBars(false);
        catalogScroll.setScrollingDisabled(true, false);
        catalogScroll.setOverscroll(false, false);
        panel.add(catalogScroll).grow();
        return panel;
    }

    private Table buildDetailsPanel() {
        Table panel = cardPanel(11f);
        panel.top();
        panel.add(theme.settingsTitle("PLANT INFO")).growX().padBottom(5f);
        panel.row();

        PlantDefinition plant = focusedDefinition();
        if (plant == null) {
            Label message = theme.settingsLabel("Select a plant to see its details.");
            message.setWrap(true);
            panel.add(message).width(205f).padTop(20f);
            return panel;
        }

        PlantAnimationActor animation = new PlantAnimationActor(app.assets(), plant);
        if (animation.hasAnimation()) {
            panel.add(animation).width(205f).height(142f);
        } else {
            String packetArt = packetArtId(plant);
            Image image = packetArt == null ? null : theme.image(packetArt);
            if (image != null) {
                panel.add(image).width(126f).height(112f).padTop(8f);
            }
        }
        panel.row();

        Label name = theme.heading(plant.getName());
        name.setWrap(false);
        name.setEllipsis(true);
        panel.add(name).width(210f).height(34f).padBottom(4f);
        panel.row();

        addDetail(panel, "Sun", Integer.toString(plant.getCost()));
        addDetail(panel, "Family", plant.getFamily().getDisplayName());
        addDetail(panel, "Damage", plant.getDamage());
        addDetail(panel, "Health", Integer.toString(plant.getBaseHealth()));

        int currentLevel = user.getCollectionBook().getPlantLevel(plant.getName());
        addDetail(panel, "Level", currentLevel == 0 ? "Locked" : Integer.toString(currentLevel));
        addDetail(panel, "Seeds", seedSummary(plant));
        addDetail(panel, "Boost", boostSummary(plant));

        Label ability = theme.settingsLabel(plant.getBaseAbility());
        ability.setWrap(true);
        ability.setAlignment(Align.left);
        panel.add(ability).width(210f).height(54f).padTop(5f);
        panel.row();

        int upgradeCoinCost = Math.max(0, currentLevel * 1000);
        String upgradeText = isMaxLevel(plant) ? "MAX LEVEL" : "Upgrade " + upgradeCoinCost + " Coins";
        TextButton upgrade = theme.primaryButton(upgradeText);
        TextButton boost = theme.tertiaryButton("Boost 2 Gems");
        boolean owned = isOwned(plant);
        boolean canPlantFoodBoost = plant.getPlantFoodType() != PlantFoodType.NONE
            || plant.getAbility() == PlantAbility.IMITATER;
        upgrade.setDisabled(!owned || isMaxLevel(plant));
        boost.setDisabled(!owned || !isSelected(plant) || isLevelBoosted(plant)
            || !canPlantFoodBoost);
        UiActions.onClick(upgrade, () -> upgradePlant(plant));
        UiActions.onClick(boost, () -> boostPlant(plant));
        panel.add(upgrade).width(205f).height(45f).padTop(5f);
        panel.row();
        panel.add(boost).width(205f).height(45f).padTop(5f);
        return panel;
    }

    private void addDetail(Table panel, String key, String value) {
        Table row = new Table();
        Label keyLabel = theme.settingsLabel(key);
        keyLabel.setFontScale(0.78f);
        Label valueLabel = theme.settingsLabel(value);
        valueLabel.setFontScale(0.78f);
        valueLabel.setAlignment(Align.right);
        valueLabel.setEllipsis(true);
        row.add(keyLabel).left();
        row.add(valueLabel).expandX().right();
        panel.add(row).growX().height(25f);
        panel.row();
    }

    private Table buildFooter() {
        Table footer = new Table();
        TextButton back = theme.secondaryButton("Back");
        TextButton start = theme.tertiaryButton("LET'S ROCK!");
        start.getLabel().setFontScale(1.08f);
        UiActions.onClick(back, () -> app.showLevelBriefing(chapter, level));
        UiActions.onClick(start, this::startGame);
        footer.add(back).width(170f).height(50f);
        footer.add().expandX();
        footer.add(theme.settingsLabel("Select up to " + level.getAllowedPlantCount() + " plants."))
            .center();
        footer.add().expandX();
        footer.add(start).width(245f).height(58f);
        return footer;
    }

    private PlantPacketCard createCard(PlantDefinition plant, boolean compact) {
        int plantLevel = user.getCollectionBook().getPlantLevel(plant.getName());
        int seedPackets = user.getInventory().getSeedPacketCount(plant.getName());
        boolean owned = plantLevel > 0;
        boolean available = availablePlantNames.contains(plant.getName());
        boolean selected = isSelected(plant);
        boolean boosted = isBoosted(plant);
        int seedNeeded = Math.max(0, plantLevel * 10);
        State state = new State(
            owned,
            available,
            selected,
            boosted,
            plantLevel,
            seedPackets,
            seedNeeded,
            isMaxLevel(plant)
        );
        return new PlantPacketCard(theme, plant, state, compact);
    }

    private void focusOrToggle(PlantDefinition plant) {
        focusedPlant = plant.getName();
        if (!isOwned(plant)) {
            theme.showError(status, plant.getName() + " is locked.");
            rebuild();
            return;
        }
        if (!availablePlantNames.contains(plant.getName()) && !isSelected(plant)) {
            theme.showError(status, plant.getName() + " is unavailable in this level.");
            rebuild();
            return;
        }

        ActionResult result = isSelected(plant)
            ? app.services().game().removePlantSelection(plant.getName())
            : app.services().game().selectPlant(plant.getName());
        if (result.isSuccessful()) {
            theme.showSuccess(status, result.getMessage());
        } else {
            theme.showError(status, result.getMessage());
        }
        rebuild();
    }

    private void upgradePlant(PlantDefinition plant) {
        focusedPlant = plant.getName();
        ActionResult result = app.services().collection().upgradePlant(plant.getName());
        if (result.isSuccessful()) {
            theme.showSuccess(status, result.getMessage());
        } else {
            theme.showError(status, result.getMessage());
        }
        rebuild();
    }

    private void boostPlant(PlantDefinition plant) {
        focusedPlant = plant.getName();
        ActionResult result = app.services().game().boostPlant(plant.getName());
        if (result.isSuccessful()) {
            theme.showSuccess(status, result.getMessage());
        } else {
            theme.showError(status, result.getMessage());
        }
        rebuild();
    }

    private void startGame() {
        ActionResult result = app.services().game().startGame();
        if (!result.isSuccessful()) {
            theme.showError(status, result.getMessage());
            rebuild();
            return;
        }
        app.showBattle(chapter, level);
    }

    private String seedSummary(PlantDefinition plant) {
        if (!isOwned(plant)) {
            return "Locked";
        }
        int current = user.getCollectionBook().getPlantLevel(plant.getName());
        if (current >= maximumLevel(plant)) {
            return "MAX";
        }
        int available = user.getInventory().getSeedPacketCount(plant.getName());
        return available + " / " + current * 10;
    }

    private String boostSummary(PlantDefinition plant) {
        if (isLevelBoosted(plant)) {
            return "Level boost";
        }
        if (hasStoredBoost(plant)) {
            return "Greenhouse ready";
        }
        return "None";
    }

    private boolean isBoosted(PlantDefinition plant) {
        return isLevelBoosted(plant) || hasStoredBoost(plant);
    }

    private boolean isLevelBoosted(PlantDefinition plant) {
        Game game = app.services().game().getGame();
        return game != null && game.isLevelBoosted(plant.getName());
    }

    private boolean hasStoredBoost(PlantDefinition plant) {
        return user.getInventory().getStoredBoosts().getOrDefault(plant.getName(), 0) > 0;
    }

    private boolean isSelected(PlantDefinition plant) {
        return app.services().game().getSelectedPlants().contains(plant.getName());
    }

    private boolean isOwned(PlantDefinition plant) {
        return user.getCollectionBook().getOwnedPlants().contains(plant.getName());
    }

    private boolean isMaxLevel(PlantDefinition plant) {
        int current = user.getCollectionBook().getPlantLevel(plant.getName());
        return current > 0 && current >= maximumLevel(plant);
    }

    private int maximumLevel(PlantDefinition plant) {
        return Math.max(1, plant.getLevelUpgrades().size() + 1);
    }

    private PlantDefinition focusedDefinition() {
        return focusedPlant == null ? null : findPlant(focusedPlant);
    }

    private PlantDefinition findPlant(String name) {
        return app.services().gameData().getPlantFactory().findDefinition(name).orElse(null);
    }

    private String packetArtId(PlantDefinition plant) {
        String normalized = plant.getKey().toUpperCase().replaceAll("[^A-Z0-9]", "");
        if ("GOOPEASHOOTER".equals(normalized)) {
            normalized = "POISONPEASHOOTER";
        } else if ("MEGAGATLINGPEA".equals(normalized)) {
            normalized = "MEGAGATLING";
        } else if ("CHERRYBOMB".equals(normalized)) {
            normalized = "CHERRY_BOMB";
        } else if ("ICEBERGLETTUCE".equals(normalized)) {
            normalized = "ICEBURG";
        } else if ("PIERCEMINT".equals(normalized)) {
            normalized = "SPEARMINT";
        }
        String candidate = "IMAGE_UI_PACKETS_" + normalized;
        if (theme.drawable(candidate) != null) {
            return candidate;
        }
        return null;
    }

    private void rebuild() {
        if (catalogScroll != null) {
            savedScrollY = catalogScroll.getScrollY();
        }
        root.clearChildren();
        buildUi();
    }

    private void restoreCatalogScroll() {
        if (catalogScroll == null || savedScrollY <= 0f) {
            return;
        }
        catalogScroll.layout();
        catalogScroll.setScrollY(savedScrollY);
        catalogScroll.updateVisualScroll();
    }
    private Table cardPanel(float padding) {
        Table table = new Table();
        table.setBackground(
            theme.skin().getDrawable("image_ui_dialog_asset_inner_bkgd_10")
        );
        table.pad(padding);
        return table;
    }

    private Table badgePanel(float padding) {
        Table table = new Table();
        table.setBackground(
            theme.skin().getDrawable("image_ui_dialog_asset_inner_bkgd_10")
        );
        table.pad(padding);
        return table;
    }

}
