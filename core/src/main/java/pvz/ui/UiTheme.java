package pvz.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import pvz.assets.UiAtlasHelper;
import pvz.skin.BorderedTable;

public final class UiTheme {
    public static final String MAIN_MENU_BACKGROUND = "IMAGE_MAINMENU_BACKGROUND";
    public static final String PVZ2_LOGO = "IMAGE_UI_MAINMENU_PVZ2_LOGO_HORIZONTAL";
    public static final String MAIN_MENU_TILE = "IMAGE_UI_MAINMENU_MM_TILE";
    public static final String MAIN_MENU_INSET = "IMAGE_UI_MAINMENU_INSET_BKGD";
    public static final String PLAYER_ICON = "IMAGE_UI_MAINMENU_MM_PLAYERICON";
    public static final String NEWS_ICON = "IMAGE_UI_MAINMENU_MM_NEWSICON";
    public static final String SETTINGS_ICON = "IMAGE_UI_MAINMENU_MM_SETTINGS";
    public static final String ALMANAC_ICON = "IMAGE_UI_HUD_ALMANACBUTTON_BUTTONS_HUD_ALMANAC_NORMAL";
    public static final String QUEST_ICON = "IMAGE_UI_HUD_QUESTBUTTON_QUEST_ICON_UP";
    public static final String ADVENTURE_ICON = "IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_MENU_NORMAL";
    public static final String GREENHOUSE_ICON = "IMAGE_UI_HUD_INGAME_SPROUT_ICON_NOPLUS";
    public static final String LEADERBOARD_ICON = "IMAGE_UI_GAMECENTER_ANDROID_LEADERBOARD";
    public static final String COIN_ICON = "IMAGE_UI_HUD_INGAME_COIN";
    public static final String GEM_ICON = "IMAGE_UI_HUD_INGAME_GEM";
    public static final String DIFFICULTY_BG = "IMAGE_UI_QUESTS_DIFFICULTY_BG";
    public static final String RED_DOT = "IMAGE_UI_HUD_INGAME_STORE_RED_DOT";

    private static final Color ERROR_COLOR = new Color(1f, 0.34f, 0.23f, 1f);
    private static final Color SUCCESS_COLOR = new Color(0.48f, 1f, 0.35f, 1f);
    private static final Color NORMAL_STATUS_COLOR = Color.WHITE;

    private final Skin skin;
    private final UiAtlasHelper atlas;

    public UiTheme(Skin skin, UiAtlasHelper atlas) {
        this.skin = skin;
        this.atlas = atlas;
    }

    public Image screenBackground() {
        Image background = atlas.image(MAIN_MENU_BACKGROUND, Scaling.fill);
        if (background != null) {
            background.setAlign(Align.center);
            return background;
        }
        Image fallback = new Image(skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        fallback.setScaling(Scaling.fill);
        return fallback;
    }

    public Image image(String imageId) {
        return atlas.image(imageId, Scaling.fit);
    }

    public Drawable drawable(String imageId) {
        return atlas.drawable(imageId);
    }

    public Image pvzLogo() {
        return image(PVZ2_LOGO);
    }

    public BorderedTable dialogPanel() {
        BorderedTable panel = new BorderedTable();
        panel.pad(30f, 40f, 32f, 40f);
        return panel;
    }

    public Table insetPanel(float padding) {
        Table table = new Table();
        Drawable background = drawable(MAIN_MENU_INSET);
        if (background == null) {
            background = skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10");
        }
        table.setBackground(background);
        table.pad(padding);
        return table;
    }

    public Label title(String text) {
        Label label = new Label(text, skin, "big_outline");
        label.setAlignment(Align.center);
        return label;
    }

    public Label heading(String text) {
        Label label = new Label(text, skin, "medium");
        label.setAlignment(Align.center);
        return label;
    }

    public Label fieldLabel(String text) {
        Label label = new Label(text, skin, "secondary");
        label.setAlignment(Align.left);
        return label;
    }

    public Label bodyLabel(String text) {
        Label label = new Label(text, skin, "secondary");
        label.setAlignment(Align.center);
        label.setWrap(true);
        return label;
    }

    public TextField textField(String hint) {
        TextField field = new TextField("", skin);
        field.setMessageText(hint);
        return field;
    }

    public TextField passwordField(String hint) {
        TextField field = textField(hint);
        field.setPasswordMode(true);
        field.setPasswordCharacter('*');
        return field;
    }

    public SelectBox<String> genderSelect() {
        SelectBox<String> selectBox = new SelectBox<>(skin);
        selectBox.setItems("MALE", "FEMALE");
        return selectBox;
    }

    public CheckBox stayLoggedInCheckBox() {
        return new CheckBox(" Stay logged in", skin);
    }

    public TextButton primaryButton(String text) {
        return new TextButton(text, skin, "green");
    }

    public TextButton secondaryButton(String text) {
        return new TextButton(text, skin, "brown");
    }

    public TextButton tertiaryButton(String text) {
        return new TextButton(text, skin, "purple");
    }

    public Label statusLabel() {
        Label label = bodyLabel("");
        label.setColor(NORMAL_STATUS_COLOR);
        return label;
    }

    public void showError(Label label, String message) {
        label.setColor(ERROR_COLOR);
        label.setText(message == null ? "" : message);
    }

    public void showSuccess(Label label, String message) {
        label.setColor(SUCCESS_COLOR);
        label.setText(message == null ? "" : message);
    }

    public Skin skin() {
        return skin;
    }
}
