package pvz.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import model.ArmorDefinition;
import model.PlantDefinition;
import model.User;
import model.ZombieDefinition;
import pvz.PvzApplication;
import pvz.screen.UiActions;

import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.Consumer;

public final class CollectionDetailPanel {
    private static final int PLANT_PURCHASE_COST = 2000;
    private static final float PANEL_CONTENT_WIDTH = 332f;
    private static final float KEY_WIDTH = 108f;
    private static final float VALUE_WIDTH = 216f;
    private static final float STAT_ICON_WIDTH = 32f;
    private static final float STAT_KEY_WIDTH = 80f;
    private static final float STAT_VALUE_WIDTH = 212f;
    private static final String SUN_COST_ICON =
        "IMAGE_UI_ALMANAC_ALMANAC_STAT_ICON_SUNCOST";
    private static final String TOUGHNESS_ICON =
        "IMAGE_UI_ALMANAC_PLANTS_TOUGHNESS_ICON";
    private static final String DAMAGE_ICON =
        "IMAGE_UI_ALMANAC_PLANTS_DAMAGE_ICON";
    private static final String RECHARGE_ICON =
        "IMAGE_UI_ALMANAC_PLANTS_RECHARGE_ICON";
    private static final String FAMILY_ICON =
        "IMAGE_UI_ALMANAC_ALMANAC_STAT_ICON_FAMILY";

    private final PvzApplication app;
    private final UiTheme theme;
    private final User user;

    public CollectionDetailPanel(PvzApplication app, UiTheme theme, User user) {
        this.app = app;
        this.theme = theme;
        this.user = user;
    }

    public Table buildPlant(PlantDefinition plant,
                            Consumer<PlantDefinition> purchaseAction,
                            Consumer<PlantDefinition> upgradeAction) {
        Table panel = basePanel("PLANT INFO");
        if (plant == null) {
            panel.add(centeredMessage("Select a plant."))
                .width(PANEL_CONTENT_WIDTH)
                .padTop(20f);
            return panel;
        }

        addPlantVisual(panel, plant);
        addName(panel, plant.getName());
        addDetail(panel, "ID", Integer.toString(plant.getId()));
        addDetail(panel, "Data Key", plant.getKey());
        addDetail(panel, "Required", plant.isRequired() ? "Yes" : "No");
        addFamilyDetail(panel, plant);
        addDetail(panel, "Tags", plant.getTags().isEmpty()
            ? "None" : String.join(", ", plant.getTags()));
        addStatDetail(panel, SUN_COST_ICON, "Sun Cost", Integer.toString(plant.getCost()));
        addStatDetail(panel, TOUGHNESS_ICON, "Base Health", Integer.toString(plant.getBaseHealth()));
        addStatDetail(panel, DAMAGE_ICON, "Damage", plant.getDamage());
        addDetail(panel, "Action Interval", formatSeconds(plant.getActionIntervalSeconds()));
        addStatDetail(panel, RECHARGE_ICON, "Recharge", formatSeconds(plant.getRechargeSeconds()));
        addDetail(panel, "Projectiles", Integer.toString(plant.getProjectileCount()));
        addDetail(panel, "Current Level", plantLevelText(plant));
        addDetail(panel, "Seed Packets", seedText(plant));

        addSection(panel, "BASE ABILITY");
        addDetail(panel, "Type", prettyEnum(plant.getAbility().name()));
        addDetail(panel, "Power", trimNumber(plant.getAbilityPower()));
        addDetail(panel, "Parameters", numberMapText(plant.getAbilityParameters()));
        addDescription(panel, plant.getBaseAbility());
        addSection(panel, "PLANT FOOD");
        addDetail(panel, "Type", prettyEnum(plant.getPlantFoodType().name()));
        addDetail(panel, "Power", trimNumber(plant.getPlantFoodPower()));
        addDetail(panel, "Parameters", numberMapText(plant.getPlantFoodParameters()));
        addDescription(panel, plant.getPlantFoodEffect());
        addSection(panel, "LEVEL UPGRADES");
        addDescription(panel, upgradeDetails(plant));

        panel.add(buildPlantAction(plant, purchaseAction, upgradeAction))
            .width(PANEL_CONTENT_WIDTH)
            .minWidth(0f)
            .height(50f)
            .padTop(8f);
        return panel;
    }

    public Table buildZombie(ZombieDefinition zombie, boolean seen) {
        Table panel = basePanel("ZOMBIE INFO");
        if (zombie == null || !seen) {
            panel.add(centeredMessage(
                "Discover a zombie in battle to reveal its information."))
                .width(PANEL_CONTENT_WIDTH).padTop(28f);
            return panel;
        }

        addZombieVisual(panel, zombie);
        addName(panel, zombie.getDisplayName());
        addDetail(panel, "Data Alias", zombie.getAlias());
        addDetail(panel, "Health", Integer.toString(zombie.getHitpoints()));
        addDetail(panel, "Eat DPS", Integer.toString(zombie.getEatDamagePerSecond()));
        addDetail(panel, "Speed", formatSpeed(zombie.getSpeed()));
        addDetail(panel, "Wave Cost", Integer.toString(zombie.getWavePointCost()));
        addDetail(panel, "Selection Weight", Integer.toString(zombie.getWeight()));
        addDetail(panel, "Plant Food Drop", zombie.canSpawnPlantFood() ? "Yes" : "No");
        addDetail(panel, "Ability", prettyEnum(zombie.getAbility().name()));
        addDetail(panel, "Worlds", worldText(zombie));
        addDetail(panel, "Armors", armorText(zombie));

        addSection(panel, "SPECIAL PROPERTIES");
        addSpecialProperties(panel, zombie.getSpecialProperties());
        return panel;
    }

    private Table basePanel(String title) {
        Table panel = theme.settingsCardPanel(10f);
        panel.defaults().minWidth(0f);
        panel.top();
        panel.add(theme.settingsTitle(title))
            .width(PANEL_CONTENT_WIDTH)
            .minWidth(0f)
            .padBottom(5f);
        panel.row();
        return panel;
    }

    private void addPlantVisual(Table panel, PlantDefinition plant) {
        PlantAnimationActor animation = new PlantAnimationActor(app.assets(), plant);
        if (animation.hasAnimation()) {
            panel.add(animation).width(250f).height(135f).padBottom(2f);
            panel.row();
            return;
        }
        Image art = PlantArtResolver.packetImage(theme, plant);
        if (art != null) {
            panel.add(art).width(145f).height(115f).padBottom(4f);
            panel.row();
        }
    }

    private void addZombieVisual(Table panel, ZombieDefinition zombie) {
        ZombieAnimationActor animation = new ZombieAnimationActor(app.assets(), zombie);
        if (animation.hasAnimation()) {
            panel.add(animation).width(250f).height(165f).padBottom(4f);
            panel.row();
            return;
        }
        Image art = ZombieArtResolver.image(theme, zombie);
        if (art != null) {
            panel.add(art).width(235f).height(150f).padBottom(4f);
            panel.row();
        }
    }

    private void addName(Table panel, String text) {
        Label name = theme.heading(text);
        name.setWrap(true);
        name.setAlignment(Align.center);
        panel.add(name)
            .width(PANEL_CONTENT_WIDTH)
            .minWidth(0f)
            .minHeight(38f)
            .padBottom(4f);
        panel.row();
    }

    private void addDetail(Table panel, String key, String value) {
        Table row = new Table();
        Label left = theme.settingsLabel(key);
        Label right = theme.settingsLabel(nonEmpty(value));
        left.setAlignment(Align.left | Align.top);
        right.setWrap(true);
        right.setAlignment(Align.right | Align.top);
        left.setWrap(false);
        row.defaults().minWidth(0f);
        row.add(left).width(KEY_WIDTH).minWidth(0f).left().top();
        row.add(right).width(VALUE_WIDTH).minWidth(0f).right().top();
        panel.add(row)
            .width(PANEL_CONTENT_WIDTH)
            .minWidth(0f)
            .minHeight(28f)
            .padBottom(2f);
        panel.row();
    }

    private void addStatDetail(Table panel, String iconId, String key, String value) {
        Table row = new Table();
        Image icon = theme.image(iconId);
        if (icon != null) {
            icon.setScaling(Scaling.fit);
            row.add(icon).size(STAT_ICON_WIDTH).padRight(6f);
        } else {
            row.add().width(STAT_ICON_WIDTH + 6f);
        }
        Label left = theme.settingsLabel(key);
        Label right = theme.settingsLabel(nonEmpty(value));
        left.setAlignment(Align.left | Align.top);
        right.setWrap(true);
        right.setAlignment(Align.right | Align.top);
        left.setWrap(false);
        row.defaults().minWidth(0f);
        row.add(left).width(STAT_KEY_WIDTH).minWidth(0f).left().top();
        row.add(right).width(STAT_VALUE_WIDTH).minWidth(0f).right().top();
        panel.add(row)
            .width(PANEL_CONTENT_WIDTH)
            .minWidth(0f)
            .minHeight(32f)
            .padBottom(2f);
        panel.row();
    }

    private void addFamilyDetail(Table panel, PlantDefinition plant) {
        Table row = new Table();
        Image icon = theme.image(FAMILY_ICON);
        if (icon != null) {
            icon.setScaling(Scaling.fit);
            row.add(icon).size(STAT_ICON_WIDTH).padRight(6f);
        } else {
            row.add().width(STAT_ICON_WIDTH + 6f);
        }
        Label key = theme.settingsLabel("Family");
        key.setAlignment(Align.left | Align.top);
        key.setWrap(false);
        row.add(key).width(STAT_KEY_WIDTH).minWidth(0f).left().top();

        Table value = new Table();
        value.defaults().minWidth(0f);
        Label family = theme.settingsLabel(plant.getFamily().getDisplayName());
        family.setAlignment(Align.right | Align.top);
        family.setWrap(false);
        value.add(family).width(STAT_VALUE_WIDTH - 32f).right().top();
        Image familyIcon = PlantArtResolver.familyIcon(theme, plant);
        if (familyIcon != null) {
            familyIcon.setScaling(Scaling.fit);
            value.add(familyIcon).size(28f).padLeft(4f).right().top();
        }
        row.add(value).width(STAT_VALUE_WIDTH).minWidth(0f).right().top();
        panel.add(row)
            .width(PANEL_CONTENT_WIDTH)
            .minWidth(0f)
            .minHeight(32f)
            .padBottom(2f);
        panel.row();
    }

    private void addSection(Table panel, String title) {
        Label heading = theme.settingsLabel(title);
        heading.setAlignment(Align.left);
        panel.add(heading)
            .width(PANEL_CONTENT_WIDTH)
            .minWidth(0f)
            .left()
            .padTop(9f)
            .padBottom(2f);
        panel.row();
    }

    private void addDescription(Table panel, String text) {
        Label description = theme.settingsLabel(nonEmpty(text));
        description.setWrap(true);
        description.setAlignment(Align.left | Align.top);
        panel.add(description)
            .width(PANEL_CONTENT_WIDTH)
            .minWidth(0f)
            .left()
            .padBottom(4f);
        panel.row();
    }

    private void addSpecialProperties(Table panel, Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            addDescription(panel, "None");
            return;
        }
        boolean added = false;
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank() || key.trim().startsWith("#")) {
                continue;
            }
            addDetail(panel, prettyPropertyName(key), propertyText(entry.getValue()));
            added = true;
        }
        if (!added) {
            addDescription(panel, "None");
        }
    }

    private Table buildPlantAction(PlantDefinition plant,
                                   Consumer<PlantDefinition> purchaseAction,
                                   Consumer<PlantDefinition> upgradeAction) {
        Table row = new Table();
        if (!isOwned(plant)) {
            TextButton buy = theme.primaryButton("Buy - " + PLANT_PURCHASE_COST + " Coins");
            UiActions.onClick(buy, () -> purchaseAction.accept(plant));
            row.add(buy).growX().height(48f);
            return row;
        }
        boolean max = isMaxLevel(plant);
        TextButton upgrade = theme.primaryButton(max ? "MAX LEVEL" : upgradeButtonText(plant));
        upgrade.setDisabled(max);
        upgrade.getLabel().setWrap(false);
        upgrade.getLabel().setFontScale(0.58f);
        UiActions.onClick(upgrade, () -> upgradeAction.accept(plant));
        row.add(upgrade).width(292f).height(44f).center();
        return row;
    }


    private String numberMapText(Map<String, Double> values) {
        if (values == null || values.isEmpty()) {
            return "None";
        }
        StringBuilder text = new StringBuilder();
        for (Map.Entry<String, Double> entry : values.entrySet()) {
            if (text.length() > 0) {
                text.append(", ");
            }
            text.append(prettyPropertyName(entry.getKey()))
                .append(" = ")
                .append(trimNumber(entry.getValue()));
        }
        return text.toString();
    }

    private String armorText(ZombieDefinition zombie) {
        List<ArmorDefinition> armors = app.services().collection().getArmorDefinitions(zombie);
        if (armors.isEmpty()) {
            return "None";
        }
        StringBuilder text = new StringBuilder();
        for (ArmorDefinition armor : armors) {
            if (text.length() > 0) {
                text.append("; ");
            }
            text.append(armor.getArmorType())
                .append(" (")
                .append(armor.getBaseHealth())
                .append(" HP");
            if (armor.hasFlag("metallic")) {
                text.append(", metallic");
            }
            text.append(')');
        }
        return text.toString();
    }

    private String worldText(ZombieDefinition zombie) {
        if (zombie.getSeasons().isEmpty()) {
            return "None";
        }
        return zombie.getSeasons().stream()
            .map(season -> prettyEnum(season.name()))
            .reduce((left, right) -> left + ", " + right)
            .orElse("None");
    }

    private String upgradeDetails(PlantDefinition plant) {
        List<String> upgrades = plant.getLevelUpgrades();
        if (upgrades.isEmpty()) {
            return "None";
        }
        StringBuilder text = new StringBuilder();
        for (String upgrade : upgrades) {
            if (text.length() > 0) {
                text.append('\n');
            }
            text.append(upgrade);
        }
        return text.toString();
    }

    private String formatSeconds(OptionalDouble seconds) {
        if (seconds.isEmpty() || seconds.getAsDouble() <= 0.0) {
            return "N/A";
        }
        return trimNumber(seconds.getAsDouble()) + " s";
    }

    private String formatSpeed(double speed) {
        return speed <= 0.0 ? "Stationary" : trimNumber(speed);
    }

    private String propertyText(Object value) {
        if (value == null) {
            return "None";
        }
        if (value instanceof Double number) {
            return trimNumber(number);
        }
        if (value instanceof Float number) {
            return trimNumber(number.doubleValue());
        }
        return value.toString();
    }

    private String prettyPropertyName(String key) {
        return key.replace("project", "")
            .replaceAll("([a-z0-9])([A-Z])", "$1 $2")
            .replace('_', ' ')
            .trim();
    }

    private String prettyEnum(String value) {
        String[] words = value.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    private String trimNumber(double value) {
        if (Math.rint(value) == value) {
            return Long.toString(Math.round(value));
        }
        String text = String.format(java.util.Locale.ROOT, "%.3f", value);
        while (text.endsWith("0")) {
            text = text.substring(0, text.length() - 1);
        }
        return text.endsWith(".") ? text.substring(0, text.length() - 1) : text;
    }

    private Label centeredMessage(String text) {
        Label message = theme.settingsLabel(text);
        message.setWrap(true);
        message.setAlignment(Align.center);
        return message;
    }

    private boolean isOwned(PlantDefinition plant) {
        return user.getCollectionBook().getPlantLevel(plant.getName()) > 0;
    }

    private boolean isMaxLevel(PlantDefinition plant) {
        int level = user.getCollectionBook().getPlantLevel(plant.getName());
        int maximum = Math.max(1, plant.getLevelUpgrades().size() + 1);
        return level > 0 && level >= maximum;
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

    private String upgradeButtonText(PlantDefinition plant) {
        int level = user.getCollectionBook().getPlantLevel(plant.getName());
        return "Upgrade - " + (level * 10) + " Seeds / " + (level * 1000) + " Coins";
    }

    private String nonEmpty(String value) {
        return value == null || value.isBlank() ? "None" : value;
    }
}
