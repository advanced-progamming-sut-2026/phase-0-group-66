package pvz.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Scaling;
import pvz.assets.UiAtlasHelper;
import pvz.skin.BorderedTable;

public final class UiTheme implements Disposable {
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

    private static final String UI_FONT = "skin/FBUSV8C5EI.TTF";
    private static final String BUTTON_FONT = "skin/HOUSE OF TERROR.TTF";
    private static final int FONT_OVERSAMPLE = 2;

    private static final Color ERROR_COLOR = new Color(0.82f, 0.09f, 0.05f, 1f);
    private static final Color SUCCESS_COLOR = new Color(0.12f, 0.55f, 0.08f, 1f);
    private static final Color DARK_BROWN = new Color(0.28f, 0.24f, 0f, 1f);
    private static final Color MESSAGE_GRAY = new Color(0.46f, 0.46f, 0.46f, 1f);

    private final Skin skin;
    private final UiAtlasHelper atlas;

    private final BitmapFont titleFont;
    private final BitmapFont headingFont;
    private final BitmapFont bodyFont;
    private final BitmapFont fieldFont;
    private final BitmapFont buttonFont;

    private final Label.LabelStyle titleStyle;
    private final Label.LabelStyle headingStyle;
    private final Label.LabelStyle fieldLabelStyle;
    private final Label.LabelStyle bodyStyle;
    private final TextField.TextFieldStyle textFieldStyle;
    private final CheckBox.CheckBoxStyle checkBoxStyle;
    private final SelectBox.SelectBoxStyle selectBoxStyle;

    public UiTheme(Skin skin, UiAtlasHelper atlas) {
        this.skin = skin;
        this.atlas = atlas;
        enableLinearFiltering();

        titleFont = generateFont(UI_FONT, 48, 3.5f);
        headingFont = generateFont(UI_FONT, 28, 0f);
        bodyFont = generateFont(UI_FONT, 22, 0f);
        fieldFont = generateFont(UI_FONT, 24, 0f);
        buttonFont = generateFont(BUTTON_FONT, 28, 1.2f);

        titleStyle = new Label.LabelStyle(titleFont, Color.WHITE);
        headingStyle = new Label.LabelStyle(headingFont, DARK_BROWN);
        fieldLabelStyle = new Label.LabelStyle(bodyFont, DARK_BROWN);
        bodyStyle = new Label.LabelStyle(bodyFont, DARK_BROWN);

        textFieldStyle = new TextField.TextFieldStyle(skin.get(TextField.TextFieldStyle.class));
        textFieldStyle.font = fieldFont;
        textFieldStyle.messageFont = fieldFont;
        textFieldStyle.fontColor = Color.BLACK;
        textFieldStyle.messageFontColor = MESSAGE_GRAY;

        checkBoxStyle = new CheckBox.CheckBoxStyle(skin.get(CheckBox.CheckBoxStyle.class));
        checkBoxStyle.font = bodyFont;
        checkBoxStyle.fontColor = DARK_BROWN;

        selectBoxStyle = new SelectBox.SelectBoxStyle(skin.get(SelectBox.SelectBoxStyle.class));
        selectBoxStyle.font = fieldFont;
        selectBoxStyle.fontColor = Color.BLACK;
        List.ListStyle listStyle = new List.ListStyle(selectBoxStyle.listStyle);
        listStyle.font = fieldFont;
        selectBoxStyle.listStyle = listStyle;
    }

    private void enableLinearFiltering() {
        if (skin.getAtlas() == null) {
            return;
        }
        for (Texture texture : skin.getAtlas().getTextures()) {
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
    }

    private BitmapFont generateFont(String path, int displaySize, float displayBorderWidth) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.classpath(path));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.size = displaySize * FONT_OVERSAMPLE;
        parameter.borderWidth = displayBorderWidth * FONT_OVERSAMPLE;
        parameter.borderColor = Color.BLACK;
        parameter.color = Color.WHITE;
        parameter.hinting = FreeTypeFontGenerator.Hinting.AutoMedium;
        parameter.kerning = true;
        parameter.minFilter = Texture.TextureFilter.Linear;
        parameter.magFilter = Texture.TextureFilter.Linear;
        BitmapFont font = generator.generateFont(parameter);
        generator.dispose();
        font.getData().setScale(1f / FONT_OVERSAMPLE);
        return font;
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
        panel.pad(26f, 38f, 28f, 38f);
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
        Label label = new Label(text, titleStyle);
        label.setAlignment(Align.center);
        return label;
    }

    public Label heading(String text) {
        Label label = new Label(text, headingStyle);
        label.setAlignment(Align.center);
        return label;
    }

    public Label fieldLabel(String text) {
        Label label = new Label(text, fieldLabelStyle);
        label.setAlignment(Align.left);
        return label;
    }

    public Label bodyLabel(String text) {
        Label label = new Label(text, bodyStyle);
        label.setAlignment(Align.center);
        label.setWrap(true);
        return label;
    }

    public TextField textField(String hint) {
        TextField field = new TextField("", textFieldStyle);
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
        SelectBox<String> selectBox = new SelectBox<>(selectBoxStyle);
        selectBox.setItems("MALE", "FEMALE");
        return selectBox;
    }

    public CheckBox stayLoggedInCheckBox() {
        return new CheckBox(" Stay logged in", checkBoxStyle);
    }

    public TextButton primaryButton(String text) {
        return button(text, "green");
    }

    public TextButton secondaryButton(String text) {
        return button(text, "brown");
    }

    public TextButton tertiaryButton(String text) {
        return button(text, "purple");
    }

    private TextButton button(String text, String styleName) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle(
            skin.get(styleName, TextButton.TextButtonStyle.class)
        );
        style.font = buttonFont;
        style.fontColor = Color.WHITE;
        return new TextButton(text, style);
    }

    public Label statusLabel() {
        Label label = bodyLabel("");
        label.setColor(DARK_BROWN);
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

    @Override
    public void dispose() {
        titleFont.dispose();
        headingFont.dispose();
        bodyFont.dispose();
        fieldFont.dispose();
        buttonFont.dispose();
    }
}
