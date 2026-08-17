package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import controller.ActionResult;
import model.ArmorDefinition;
import model.PlantDefinition;
import model.PlantFamily;
import model.ZombieDefinition;
import pvz.PvzApplication;
import pvz.ui.PlantAnimationActor;
import pvz.ui.PlantPacketCard;
import pvz.ui.CollectionCardFactory;
import pvz.ui.UiTheme;
import pvz.ui.ZombieArtResolver;
import pvz.ui.ZombieAnimationActor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CollectionScreen extends AuthenticatedUiScreen {
    private static final float CARD_WIDTH = 132f;
    private static final float CARD_HEIGHT = 140f;
    private static final int GRID_COLUMNS = 5;
    private static final int PLANT_PURCHASE_COST = 2000;

    private enum Tab {
        PLANTS,
        ZOMBIES
    }

    private final Label status;
    private final CollectionCardFactory cardFactory;
    private final Table detailHost;
    private Tab tab = Tab.PLANTS;
    private String familyFilter = "ALL";
    private String plantStateFilter = "ALL";
    private String zombieStateFilter = "ALL";
    private String focusedPlant;
    private String focusedZombie;

    public CollectionScreen(PvzApplication app) {
        super(app);
        status = statusLabel();
        cardFactory = new CollectionCardFactory(app.assets(), theme, user);
        detailHost = new Table();
        chooseInitialFocus();
        buildUi();
    }

    private void chooseInitialFocus() {
        List<String> owned = app.services().collection().getOwnedPlants();
        if (!owned.isEmpty()) {
            focusedPlant = owned.get(0);
        }
        List<String> seen = app.services().collection().getSeenZombies();
        if (!seen.isEmpty()) {
            focusedZombie = seen.get(0);
        }
    }

    private void buildUi() {
        Table screen = new Table();
        screen.top();
        screen.pad(38f, 38f, 18f, 38f);

        screen.add(titleBar("COLLECTION"))
            .width(1200f)
            .height(56f)
            .padBottom(8f);
        screen.row();

        screen.add(buildTabsAndFilters())
            .width(1185f)
            .height(48f)
            .padBottom(8f);
        screen.row();

        screen.add(buildContent())
            .width(1185f)
            .height(500f);
        screen.row();

        status.setWrap(false);
        status.setAlignment(Align.left);
        screen.add(status)
            .width(1185f)
            .height(30f)
            .left()
            .padTop(4f);

        root.add(screen).grow();
    }

    private Table buildTabsAndFilters() {
        Table row = new Table();
        TextButton plants = tab == Tab.PLANTS
            ? theme.primaryButton("Plants")
            : theme.secondaryButton("Plants");
        TextButton zombies = tab == Tab.ZOMBIES
            ? theme.primaryButton("Zombies")
            : theme.secondaryButton("Zombies");
        UiActions.onClick(plants, () -> switchTab(Tab.PLANTS));
        UiActions.onClick(zombies, () -> switchTab(Tab.ZOMBIES));
        row.add(plants).width(155f).height(48f).padRight(7f);
        row.add(zombies).width(155f).height(48f).padRight(16f);

        if (tab == Tab.PLANTS) {
            row.add(theme.settingsLabel("Family")).padRight(5f);
            SelectBox<String> families = buildFamilyFilter();
            row.add(families).width(185f).height(46f).padRight(12f);
            row.add(theme.settingsLabel("Status")).padRight(5f);
            SelectBox<String> states = buildPlantStateFilter();
            row.add(states).width(170f).height(46f);
        } else {
            row.add(theme.settingsLabel("Discovery")).padRight(5f);
            SelectBox<String> states = buildZombieStateFilter();
            row.add(states).width(180f).height(46f);
        }
        row.add().expandX();
        TextButton back = theme.secondaryButton("Back");
        back.getLabel().setFontScale(0.72f);
        UiActions.onClick(back, app::showAdventure);
        row.add(back).width(140f).height(46f).padLeft(10f);
        return row;
    }

    private SelectBox<String> buildFamilyFilter() {
        SelectBox<String> box = new SelectBox<>(theme.skin());
        ArrayList<String> values = new ArrayList<>();
        values.add("ALL");
        for (PlantFamily family : PlantFamily.values()) {
            values.add(family.getDisplayName());
        }
        box.setItems(values.toArray(String[]::new));
        box.setSelected(familyFilter);
        box.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                familyFilter = box.getSelected();
                rebuild();
            }
        });
        return box;
    }

    private SelectBox<String> buildPlantStateFilter() {
        SelectBox<String> box = new SelectBox<>(theme.skin());
        box.setItems("ALL", "OWNED", "LOCKED", "UPGRADABLE");
        box.setSelected(plantStateFilter);
        box.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                plantStateFilter = box.getSelected();
                rebuild();
            }
        });
        return box;
    }

    private SelectBox<String> buildZombieStateFilter() {
        SelectBox<String> box = new SelectBox<>(theme.skin());
        box.setItems("ALL", "SEEN", "UNSEEN");
        box.setSelected(zombieStateFilter);
        box.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                zombieStateFilter = box.getSelected();
                rebuild();
            }
        });
        return box;
    }

    private Table buildContent() {
        Table content = new Table();
        refreshDetail();

        Table catalog = buildCatalogPanel();
        content.add(catalog)
            .width(790f)
            .height(500f)
            .minWidth(0f)
            .minHeight(0f)
            .padRight(10f);

        ScrollPane detailScroll = new ScrollPane(detailHost, theme.skin());
        detailScroll.setFadeScrollBars(false);
        detailScroll.setOverscroll(false, false);
        detailScroll.setScrollingDisabled(true, false);
        content.add(detailScroll)
            .width(375f)
            .height(500f)
            .minWidth(0f)
            .minHeight(0f);
        return content;
    }

    private void refreshDetail() {
        detailHost.clearChildren();
        detailHost.top();
        detailHost.add(buildDetailPanel())
            .growX()
            .top();
        detailHost.invalidateHierarchy();
    }

    private Table buildCatalogPanel() {
        Table panel = theme.settingsCardPanel(10f);
        panel.top();
        Label title = theme.settingsTitle(tab == Tab.PLANTS ? "PLANTS" : "ZOMBIES");
        panel.add(title).growX().padBottom(5f);
        panel.row();

        Table grid = tab == Tab.PLANTS ? buildPlantGrid() : buildZombieGrid();
        ScrollPane scroll = new ScrollPane(grid, theme.skin());
        scroll.setFadeScrollBars(false);
        scroll.setOverscroll(false, false);
        scroll.setScrollingDisabled(true, false);
        panel.add(scroll).grow();
        return panel;
    }

    private Table buildPlantGrid() {
        Table grid = new Table();
        grid.top().left();
        int column = 0;
        for (PlantDefinition plant : filteredPlants()) {
            PlantPacketCard card = cardFactory.plantCard(plant);
            UiActions.onClick(card, () -> focusPlant(plant));
            grid.add(card).width(CARD_WIDTH).height(CARD_HEIGHT).pad(4f);
            column++;
            if (column == GRID_COLUMNS) {
                grid.row();
                column = 0;
            }
        }
        return grid;
    }

    private Table buildZombieGrid() {
        Table grid = new Table();
        grid.top().left();
        int column = 0;
        for (ZombieDefinition zombie : filteredZombies()) {
            Button card = cardFactory.zombieCard(zombie, isSeen(zombie));
            UiActions.onClick(card, () -> focusZombie(zombie));
            grid.add(card).width(CARD_WIDTH).height(CARD_HEIGHT).pad(4f);
            column++;
            if (column == GRID_COLUMNS) {
                grid.row();
                column = 0;
            }
        }
        return grid;
    }

    private List<PlantDefinition> filteredPlants() {
        return app.services().gameData().getPlantFactory().getAllDefinitions().stream()
            .filter(this::matchesFamily)
            .filter(this::matchesPlantState)
            .sorted(Comparator.comparingInt(PlantDefinition::getId))
            .toList();
    }

    private boolean matchesFamily(PlantDefinition plant) {
        return "ALL".equals(familyFilter)
            || plant.getFamily().getDisplayName().equalsIgnoreCase(familyFilter);
    }

    private boolean matchesPlantState(PlantDefinition plant) {
        boolean owned = isOwned(plant);
        return switch (plantStateFilter) {
            case "OWNED" -> owned;
            case "LOCKED" -> !owned;
            case "UPGRADABLE" -> canUpgradeNow(plant);
            default -> true;
        };
    }

    private List<ZombieDefinition> filteredZombies() {
        return app.services().gameData().getZombieFactory().getAllDefinitions().stream()
            .filter(zombie -> switch (zombieStateFilter) {
                case "SEEN" -> isSeen(zombie);
                case "UNSEEN" -> !isSeen(zombie);
                default -> true;
            })
            .toList();
    }

    private Table buildDetailPanel() {
        return tab == Tab.PLANTS ? buildPlantDetail() : buildZombieDetail();
    }

    private Table buildPlantDetail() {
        Table panel = theme.settingsCardPanel(12f);
        panel.top();
        panel.add(theme.settingsTitle("PLANT INFO")).growX().padBottom(5f);
        panel.row();
        PlantDefinition plant = app.services().collection().findPlant(focusedPlant).orElse(null);
        if (plant == null) {
            panel.add(theme.settingsLabel("Select a plant.")).padTop(20f);
            return panel;
        }

        PlantAnimationActor animation = new PlantAnimationActor(app.assets(), plant);
        if (animation.hasAnimation()) {
            panel.add(animation).width(270f).height(135f).padBottom(2f);
            panel.row();
        }
        Label name = theme.heading(plant.getName());
        name.setWrap(false);
        panel.add(name).growX().height(36f).padBottom(4f);
        panel.row();
        addDetail(panel, "Family", plant.getFamily().getDisplayName());
        addDetail(panel, "Sun Cost", Integer.toString(plant.getCost()));
        addDetail(panel, "Health", Integer.toString(plant.getBaseHealth()));
        addDetail(panel, "Damage", plant.getDamage());
        addDetail(panel, "Tags", plant.getTags().isEmpty() ? "-" : String.join(", ", plant.getTags()));
        addDetail(panel, "Level", plantLevelText(plant));
        addDetail(panel, "Seed Packets", seedText(plant));

        Label ability = theme.settingsLabel(plant.getBaseAbility());
        ability.setWrap(true);
        ability.setAlignment(Align.left);
        panel.add(ability).growX().height(60f).padTop(6f);
        panel.row();
        panel.add(buildPlantAction(plant)).growX().height(50f).padTop(7f);
        return panel;
    }

    private Table buildPlantAction(PlantDefinition plant) {
        Table row = new Table();
        if (!isOwned(plant)) {
            TextButton buy = theme.primaryButton("Buy - " + PLANT_PURCHASE_COST + " Coins");
            UiActions.onClick(buy, () -> purchasePlant(plant));
            row.add(buy).growX().height(48f);
            return row;
        }
        TextButton upgrade = theme.primaryButton(isMaxLevel(plant) ? "MAX LEVEL" : upgradeText(plant));
        upgrade.setDisabled(isMaxLevel(plant));
        UiActions.onClick(upgrade, () -> upgradePlant(plant));
        row.add(upgrade).growX().height(48f);
        return row;
    }

    private Table buildZombieDetail() {
        Table panel = theme.settingsCardPanel(12f);
        panel.top();
        panel.add(theme.settingsTitle("ZOMBIE INFO")).growX().padBottom(5f);
        panel.row();
        ZombieDefinition zombie = app.services().collection().findZombie(focusedZombie).orElse(null);
        if (zombie == null || !isSeen(zombie)) {
            Label message = theme.settingsLabel("Discover a zombie in battle to reveal its information.");
            message.setWrap(true);
            message.setAlignment(Align.center);
            panel.add(message).width(330f).padTop(28f);
            return panel;
        }

        ZombieAnimationActor animation = new ZombieAnimationActor(app.assets(), zombie);
        if (animation.hasAnimation()) {
            panel.add(animation).width(280f).height(175f).padBottom(4f);
            panel.row();
        } else {
            Image art = ZombieArtResolver.image(theme, zombie);
            if (art != null) {
                panel.add(art).width(250f).height(160f).padBottom(4f);
                panel.row();
            }
        }
        Label name = theme.heading(zombie.getDisplayName());
        name.setWrap(false);
        panel.add(name).growX().height(38f).padBottom(4f);
        panel.row();
        addDetail(panel, "Health", Integer.toString(zombie.getHitpoints()));
        addDetail(panel, "Speed", String.format("%.2f", zombie.getSpeed()));
        addDetail(panel, "Eat Damage", Integer.toString(zombie.getEatDamagePerSecond()));
        addDetail(panel, "Ability", zombie.getAbility().name().replace('_', ' '));
        addDetail(panel, "Armor", armorText(zombie));
        addDetail(panel, "Worlds", Integer.toString(zombie.getSeasons().size()));
        return panel;
    }

    private void addDetail(Table panel, String key, String value) {
        Table row = new Table();
        Label left = theme.settingsLabel(key);
        Label right = theme.settingsLabel(value);
        right.setAlignment(Align.right);
        right.setEllipsis(true);
        row.add(left).left();
        row.add(right).expandX().right();
        panel.add(row).growX().height(28f);
        panel.row();
    }

    private String armorText(ZombieDefinition zombie) {
        List<ArmorDefinition> armor = app.services().collection().getArmorDefinitions(zombie);
        if (armor.isEmpty()) {
            return "None";
        }
        return armor.stream().map(ArmorDefinition::getArmorType).reduce((a, b) -> a + ", " + b).orElse("None");
    }

    private void switchTab(Tab next) {
        tab = next;
        rebuild();
    }

    private void focusPlant(PlantDefinition plant) {
        focusedPlant = plant.getName();
        refreshDetail();
    }

    private void focusZombie(ZombieDefinition zombie) {
        if (!isSeen(zombie)) {
            theme.showError(status, "This zombie has not been discovered yet.");
            return;
        }
        focusedZombie = zombie.getDisplayName();
        refreshDetail();
    }

    private void purchasePlant(PlantDefinition plant) {
        ActionResult result = app.services().collection().purchasePlant(plant.getName());
        showResult(result);
        if (result.isSuccessful()) {
            focusedPlant = plant.getName();
        }
        rebuild();
    }

    private void upgradePlant(PlantDefinition plant) {
        ActionResult result = app.services().collection().upgradePlant(plant.getName());
        showResult(result);
        rebuild();
    }

    private void showResult(ActionResult result) {
        if (result.isSuccessful()) {
            theme.showSuccess(status, result.getMessage());
        } else {
            theme.showError(status, result.getMessage());
        }
    }

    private boolean isOwned(PlantDefinition plant) {
        return user.getCollectionBook().getPlantLevel(plant.getName()) > 0;
    }

    private boolean isSeen(ZombieDefinition zombie) {
        return new HashSet<>(app.services().collection().getSeenZombies())
            .contains(zombie.getDisplayName());
    }

    private boolean isMaxLevel(PlantDefinition plant) {
        int level = user.getCollectionBook().getPlantLevel(plant.getName());
        int maximum = Math.max(1, plant.getLevelUpgrades().size() + 1);
        return level > 0 && level >= maximum;
    }

    private boolean canUpgradeNow(PlantDefinition plant) {
        int level = user.getCollectionBook().getPlantLevel(plant.getName());
        if (level <= 0 || isMaxLevel(plant)) {
            return false;
        }
        int seedCost = level * 10;
        int coinCost = level * 1000;
        return user.getInventory().getSeedPacketCount(plant.getName()) >= seedCost
            && user.getWallet().getCoins() >= coinCost;
    }

    private String plantLevelText(PlantDefinition plant) {
        int level = user.getCollectionBook().getPlantLevel(plant.getName());
        return level == 0 ? "Locked" : "Lv " + level;
    }

    private String seedText(PlantDefinition plant) {
        int level = user.getCollectionBook().getPlantLevel(plant.getName());
        int seeds = user.getInventory().getSeedPacketCount(plant.getName());
        if (level == 0) {
            return Integer.toString(seeds);
        }
        if (isMaxLevel(plant)) {
            return seeds + " (MAX)";
        }
        return seeds + " / " + (level * 10);
    }

    private String upgradeText(PlantDefinition plant) {
        int level = user.getCollectionBook().getPlantLevel(plant.getName());
        return "Upgrade - " + (level * 10) + " Seeds / " + (level * 1000) + " Coins";
    }

    private void rebuild() {
        root.clearChildren();
        buildUi();
    }
}
