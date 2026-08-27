package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Align;
import controller.ActionResult;
import model.PlantDefinition;
import model.ShopState;
import pvz.PvzApplication;
import pvz.ui.PlantArtResolver;
import pvz.ui.UiTheme;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ShopScreen extends AuthenticatedUiScreen {
    private static final String STORE_BACKGROUND = "IMAGE_UI_STORE_MINISTORE_BG";
    private static final String POT_ICON = "IMAGE_ZEN_GARDEN_LOCKED_POT_ICON";
    private static final String PLANT_FOOD_ICON =
        "IMAGE_UI_ALMANAC_ALMANAC_STAT_ICON_PLANTFOOD_LARGE";
    private static final String SEED_ICON = "IMAGE_UI_STOREMULTI_SEEDPACKETICON";
    private static final String GEM_STACK = "IMAGE_UI_GEMS_STACK_2";

    private final TextField quantityField;
    private final SelectBox<String> plantSelect;
    private String message = "Choose an item to purchase.";
    private boolean messageSuccess = true;

    public ShopScreen(PvzApplication app) {
        super(app);
        quantityField = theme.textField("1");
        quantityField.setText("1");
        plantSelect = new SelectBox<>(theme.skin());
        refreshOwnedPlants();
        DrawableSetter.apply(root, theme.drawable(STORE_BACKGROUND));
        buildUi();
    }

    private void buildUi() {
        root.clearChildren();
        app.services().shop().dailyOffer();

        Table screen = new Table();
        screen.top();
        screen.pad(30f, 40f, 24f, 40f);

        screen.add(titleBar("SHOP"))
            .width(1180f)
            .height(54f)
            .padBottom(8f);
        screen.row();

        screen.add(buildInventoryStrip())
            .width(1180f)
            .height(56f)
            .padBottom(8f);
        screen.row();

        screen.add(buildDailyOffer())
            .width(1180f)
            .height(126f)
            .padBottom(8f);
        screen.row();

        screen.add(buildPermanentGrid())
            .width(1180f)
            .height(365f);
        screen.row().padTop(8f);

        screen.add(buildFooter()).width(1180f).height(54f);
        root.add(screen).grow();
    }

    private Table buildInventoryStrip() {
        Table strip = theme.settingsBadgePanel(7f);
        strip.add(theme.settingsLabel("Quantity")).padRight(7f);
        strip.add(quantityField).width(92f).height(42f).padRight(18f);

        strip.add(theme.settingsLabel("Selected Plant")).padRight(7f);
        strip.add(plantSelect).width(230f).height(42f);
        strip.add().expandX();

        strip.add(theme.settingsLabel("Plant Food " + user.getInventory().getPlantFoods()))
            .padRight(18f);
        strip.add(theme.settingsLabel("Pots " + user.getInventory().getPots()))
            .padRight(18f);
        strip.add(theme.settingsLabel(
            "Greenhouse " + user.getGreenhouse().getUnlockedSlotCount() + " / 20"
        ));
        return strip;
    }

    private Table buildDailyOffer() {
        ShopState state = user.getShopState();
        Table card = theme.settingsCardPanel(10f);

        PlantDefinition definition = app.services()
            .gameData()
            .getPlantFactory()
            .findDefinition(state.getDailyPlant())
            .orElse(null);
        Image packet = definition == null ? null : PlantArtResolver.packetImage(theme, definition);
        if (packet != null) {
            card.add(packet).size(92f).padRight(14f);
        } else {
            addIcon(card, SEED_ICON, 72f, 14f);
        }

        Table text = new Table();
        text.add(theme.settingsTitle("DAILY OFFER")).left();
        text.row();
        Label description = theme.settingsLabel(
            "10 seed packets for " + state.getDailyPlant() + "  |  1600 Coins"
        );
        text.add(description).left().padTop(4f);
        text.row();
        text.add(theme.settingsLabel(
            state.isDailyPurchased() ? "Purchased today" : "Available once today"
        )).left().padTop(3f);
        card.add(text).expandX().left();

        TextButton buy = theme.tertiaryButton(
            state.isDailyPurchased() ? "Purchased" : "Buy Daily"
        );
        buy.setDisabled(state.isDailyPurchased());
        if (!state.isDailyPurchased()) {
            UiActions.onClick(buy, () -> purchase(6, 1, null));
        }
        card.add(buy).width(180f).height(54f).right();
        return card;
    }

    private Table buildPermanentGrid() {
        Table outer = theme.settingsCardPanel(10f);
        Table grid = new Table();
        List<ShopEntry> entries = List.of(
            new ShopEntry(1, "Pot / Greenhouse Slot", "Buy a Pot for 2000 Coins to unlock the next greenhouse slot.",
                "2000 Coins", POT_ICON),
            new ShopEntry(2, "Plant Food", "Add one Plant Food. Capacity is 3.",
                "3 Gems", PLANT_FOOD_ICON),
            new ShopEntry(3, "Random Packets", "5 packets for a random owned plant.",
                "1000 Coins", SEED_ICON),
            new ShopEntry(4, "Selected Packets", "10 packets for the selected owned plant.",
                "5 Gems", SEED_ICON),
            new ShopEntry(5, "Currency Exchange", "Exchange 5 gems for 500 coins.",
                "5 Gems", GEM_STACK)
        );

        for (int index = 0; index < entries.size(); index++) {
            grid.add(itemCard(entries.get(index)))
                .width(360f)
                .height(160f)
                .pad(6f);
            if (index == 2) {
                grid.row();
            }
        }
        outer.add(grid).center();
        return outer;
    }

    private Table itemCard(ShopEntry entry) {
        Table card = theme.settingsBadgePanel(10f);
        Table top = new Table();
        addIcon(top, entry.iconId(), 64f, 10f);

        Table labels = new Table();
        Label title = theme.heading(entry.title());
        title.setFontScale(0.86f);
        title.setAlignment(Align.left);
        labels.add(title).left();
        labels.row();
        Label description = theme.bodyLabel(entry.description());
        description.setFontScale(0.68f);
        description.setAlignment(Align.left);
        labels.add(description).width(245f).left().padTop(2f);
        top.add(labels).expandX().left();
        card.add(top).growX();
        card.row();

        Table buyRow = new Table();
        buyRow.add(theme.settingsLabel(entry.price())).expandX().left();
        TextButton buy = theme.primaryButton("Buy");
        if (entry.id() == 1 && user.getGreenhouse().getUnlockedSlotCount() >= 20) {
            buy.setDisabled(true);
        }
        UiActions.onClick(buy, () -> buyEntry(entry.id()));
        buyRow.add(buy).width(120f).height(44f);
        card.add(buyRow).growX().padTop(6f);
        return card;
    }

    private Table buildFooter() {
        Table footer = new Table();
        Label status = theme.statusLabel();
        status.setWrap(false);
        if (messageSuccess) {
            theme.showSuccess(status, message);
        } else {
            theme.showError(status, message);
        }

        TextButton greenhouse = theme.primaryButton("Greenhouse");
        TextButton back = theme.secondaryButton("Back to Adventure");
        UiActions.onClick(greenhouse, app::showGreenhouse);
        UiActions.onClick(back, app::showAdventure);

        footer.add(status).width(720f).left();
        footer.add().expandX();
        footer.add(greenhouse).width(170f).height(48f).padRight(8f);
        footer.add(back).width(220f).height(48f);
        return footer;
    }

    private void buyEntry(int itemId) {
        int count = quantity();
        if (count <= 0) {
            return;
        }
        String plant = itemId == 4 ? plantSelect.getSelected() : null;
        purchase(itemId, count, plant);
    }

    private void purchase(int itemId, int count, String plant) {
        ActionResult result = app.services().shop().buyItem(itemId, count, plant);
        message = result.getMessage();
        messageSuccess = result.isSuccessful();
        refreshOwnedPlants();
        buildUi();
    }

    private int quantity() {
        try {
            int value = Integer.parseInt(quantityField.getText().trim());
            if (value <= 0 || value > 20) {
                message = "Quantity must be between 1 and 20.";
                messageSuccess = false;
                buildUi();
                return -1;
            }
            return value;
        } catch (NumberFormatException exception) {
            message = "Quantity must be a number.";
            messageSuccess = false;
            buildUi();
            return -1;
        }
    }

    private void refreshOwnedPlants() {
        String selected = plantSelect == null ? null : plantSelect.getSelected();
        ArrayList<String> names = new ArrayList<>(user.getCollectionBook().getOwnedPlants());
        names.sort(Comparator.naturalOrder());
        if (names.isEmpty()) {
            names.add("Peashooter");
        }
        plantSelect.setItems(names.toArray(new String[0]));
        if (selected != null && names.contains(selected)) {
            plantSelect.setSelected(selected);
        }
    }

    private void addIcon(Table table, String id, float size, float rightPadding) {
        Image image = theme.image(id);
        if (image != null) {
            table.add(image).size(size).padRight(rightPadding);
        }
    }

    private record ShopEntry(
        int id,
        String title,
        String description,
        String price,
        String iconId
    ) {
    }

    private static final class DrawableSetter {
        private DrawableSetter() {
        }

        private static void apply(Table table, com.badlogic.gdx.scenes.scene2d.utils.Drawable drawable) {
            if (drawable != null) {
                table.setBackground(drawable);
            }
        }
    }
}
