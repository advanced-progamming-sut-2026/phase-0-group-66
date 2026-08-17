package pvz.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
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
    public static final String AUDIO_BAR = "IMAGE_UI_GENERIC_AUDIO_BAR";
    public static final String AUDIO_FILL = "IMAGE_UI_GENERIC_AUDIO_FILL";
    public static final String DIVIDER = "IMAGE_UI_GENERIC_4PXDIVIDER";
    public static final String SETTINGS_CARD =
        "IMAGE_UI_DIALOG_ASSET_TINT_ROUNDED_BOX_9SLICE";
    public static final String SETTINGS_BADGE = "IMAGE_UI_DIALOG_ASSET_INNER_BKGD";

    private static final String UI_FONT = "skin/FBUSV8C5EI.TTF";
    private static final String BUTTON_FONT = "skin/HOUSE OF TERROR.TTF";
    private static final int FONT_OVERSAMPLE = 2;

    private static final Color ERROR_COLOR = new Color(0.82f, 0.09f, 0.05f, 1f);
    private static final Color SUCCESS_COLOR = new Color(0.12f, 0.55f, 0.08f, 1f);
    private static final Color DARK_BROWN = new Color(0.28f, 0.24f, 0f, 1f);
    private static final Color SETTINGS_BROWN = new Color(0.23f, 0.15f, 0.05f, 1f);
    private static final Color SETTINGS_YELLOW = new Color(1f, 0.90f, 0.34f, 1f);
    private static final Color MESSAGE_GRAY = new Color(0.46f, 0.46f, 0.46f, 1f);

    private final Skin skin;
    private final UiAtlasHelper atlas;

    private final BitmapFont titleFont;
    private final BitmapFont headingFont;
    private final BitmapFont bodyFont;
    private final BitmapFont fieldFont;
    private final BitmapFont buttonFont;
    private final BitmapFont settingsTitleFont;
    private final BitmapFont settingsFont;

    private final Label.LabelStyle titleStyle;
    private final Label.LabelStyle headingStyle;
    private final Label.LabelStyle fieldLabelStyle;
    private final Label.LabelStyle bodyStyle;
    private final Label.LabelStyle settingsTitleStyle;
    private final Label.LabelStyle settingsLabelStyle;
    private final TextField.TextFieldStyle textFieldStyle;
    private final CheckBox.CheckBoxStyle checkBoxStyle;
    private final SelectBox.SelectBoxStyle selectBoxStyle;

    private final Texture gearTexture;
    private final Texture gearPressedTexture;
    private final Drawable gearDrawable;
    private final Drawable gearPressedDrawable;

    public UiTheme(Skin skin, UiAtlasHelper atlas) {
        this.skin = skin;
        this.atlas = atlas;
        enableLinearFiltering();

        titleFont = generateFont(UI_FONT, 48, 3.5f);
        headingFont = generateFont(UI_FONT, 28, 0f);
        bodyFont = generateFont(UI_FONT, 22, 0f);
        fieldFont = generateFont(UI_FONT, 24, 0f);
        buttonFont = generateFont(BUTTON_FONT, 28, 1.2f);
        settingsTitleFont = generateFont(UI_FONT, 36, 1.8f, SETTINGS_YELLOW, SETTINGS_BROWN);
        settingsFont = generateFont(UI_FONT, 23, 0f);

        titleStyle = new Label.LabelStyle(titleFont, Color.WHITE);
        headingStyle = new Label.LabelStyle(headingFont, DARK_BROWN);
        fieldLabelStyle = new Label.LabelStyle(bodyFont, DARK_BROWN);
        bodyStyle = new Label.LabelStyle(bodyFont, DARK_BROWN);
        settingsTitleStyle = new Label.LabelStyle(settingsTitleFont, Color.WHITE);
        settingsLabelStyle = new Label.LabelStyle(settingsFont, SETTINGS_BROWN);

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

        gearTexture = createGearTexture(false);
        gearPressedTexture = createGearTexture(true);
        gearDrawable = smallDrawable(gearTexture, 42f, 42f);
        gearPressedDrawable = smallDrawable(gearPressedTexture, 40f, 40f);
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
        return generateFont(path, displaySize, displayBorderWidth, Color.WHITE, Color.BLACK);
    }

    private BitmapFont generateFont(
        String path,
        int displaySize,
        float displayBorderWidth,
        Color glyphColor,
        Color borderColor
    ) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.classpath(path));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.size = displaySize * FONT_OVERSAMPLE;
        parameter.borderWidth = displayBorderWidth * FONT_OVERSAMPLE;
        parameter.borderColor = borderColor;
        parameter.color = glyphColor;
        parameter.hinting = FreeTypeFontGenerator.Hinting.AutoMedium;
        parameter.kerning = true;
        parameter.minFilter = Texture.TextureFilter.Linear;
        parameter.magFilter = Texture.TextureFilter.Linear;
        BitmapFont font = generator.generateFont(parameter);
        generator.dispose();
        font.getData().setScale(1f / FONT_OVERSAMPLE);
        return font;
    }

    private Texture createGearTexture(boolean pressed) {
        int size = 128;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.SourceOver);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.fill();

        int center = size / 2;
        int outer = pressed ? 43 : 46;
        int toothOuter = pressed ? 52 : 55;
        Color dark = pressed
            ? new Color(0.25f, 0.28f, 0.25f, 1f)
            : new Color(0.30f, 0.32f, 0.29f, 1f);
        Color metal = pressed
            ? new Color(0.54f, 0.58f, 0.53f, 1f)
            : new Color(0.68f, 0.72f, 0.66f, 1f);
        Color highlight = new Color(0.87f, 0.90f, 0.83f, 1f);

        pixmap.setColor(dark);
        for (int i = 0; i < 12; i++) {
            double angle = Math.PI * 2d * i / 12d;
            double side = Math.PI / 18d;
            int x1 = center + (int) (Math.cos(angle - side) * outer);
            int y1 = center + (int) (Math.sin(angle - side) * outer);
            int x2 = center + (int) (Math.cos(angle) * toothOuter);
            int y2 = center + (int) (Math.sin(angle) * toothOuter);
            int x3 = center + (int) (Math.cos(angle + side) * outer);
            int y3 = center + (int) (Math.sin(angle + side) * outer);
            pixmap.fillTriangle(x1, y1, x2, y2, x3, y3);
        }
        pixmap.fillCircle(center, center, outer);

        pixmap.setColor(metal);
        pixmap.fillCircle(center, center, outer - 7);
        pixmap.setColor(highlight);
        pixmap.drawCircle(center - 2, center - 2, outer - 12);
        pixmap.setColor(dark);
        pixmap.fillCircle(center, center, 14);
        pixmap.setColor(new Color(0.80f, 0.83f, 0.77f, 1f));
        pixmap.drawCircle(center, center, 14);

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    private Drawable smallDrawable(Texture texture, float width, float height) {
        TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(texture));
        drawable.setMinWidth(width);
        drawable.setMinHeight(height);
        return drawable;
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

    public Table settingsCardPanel(float padding) {
        Table table = new Table();
        Drawable background = atlas.ninePatchDrawable(SETTINGS_CARD, 14, 14, 14, 14);
        if (background == null) {
            background = skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10");
        }
        table.setBackground(background);
        table.pad(padding);
        return table;
    }

    public Table settingsBadgePanel(float padding) {
        Table table = new Table();
        Drawable background = atlas.ninePatchDrawable(SETTINGS_BADGE, 12, 12, 12, 12);
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

    public Label settingsTitle(String text) {
        Label label = new Label(text, settingsTitleStyle);
        label.setAlignment(Align.center);
        return label;
    }

    public Label settingsLabel(String text) {
        Label label = new Label(text, settingsLabelStyle);
        label.setAlignment(Align.left);
        label.setWrap(false);
        return label;
    }

    public Slider audioSlider(float value) {
        Slider.SliderStyle style = new Slider.SliderStyle();
        Drawable bar = audioBarDrawable(AUDIO_BAR, 48, 34, 12, 12, 8, 8);
        Drawable fill = audioBarDrawable(AUDIO_FILL, 35, 24, 8, 8, 6, 6);
        if (bar == null || fill == null) {
            Slider.SliderStyle fallback = skin.get("default-horizontal", Slider.SliderStyle.class);
            style.background = fallback.background;
            style.knobBefore = fallback.knobBefore;
        } else {
            style.background = bar;
            style.knobBefore = fill;
        }
        style.knob = gearDrawable;
        style.knobOver = gearDrawable;
        style.knobDown = gearPressedDrawable;

        Slider slider = new Slider(0f, 1f, 0.01f, false, style);
        slider.setValue(value);
        return slider;
    }

    private Drawable audioBarDrawable(
        String imageId,
        int expectedWidth,
        int expectedHeight,
        int left,
        int right,
        int top,
        int bottom
    ) {
        if (atlas.region(imageId) == null) {
            return null;
        }
        NinePatch patch = new NinePatch(atlas.region(imageId), left, right, top, bottom);
        NinePatchDrawable drawable = new NinePatchDrawable(patch);
        drawable.setMinWidth(expectedWidth);
        drawable.setMinHeight(expectedHeight);
        return drawable;
    }

    public Image divider() {
        Image image = image(DIVIDER);
        if (image != null) {
            image.setScaling(Scaling.stretch);
        }
        return image;
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

    public CheckBox checkBox(String text, boolean checked) {
        CheckBox box = new CheckBox(text, checkBoxStyle);
        box.setChecked(checked);
        return box;
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
        settingsTitleFont.dispose();
        settingsFont.dispose();
        gearTexture.dispose();
        gearPressedTexture.dispose();
    }
}
