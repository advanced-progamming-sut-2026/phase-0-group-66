package pvz.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
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
        Drawable fallback = skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10");
        Image image = new Image(fallback);
        image.setScaling(Scaling.fill);
        return image;
    }

    public Image pvzLogo() {
        return atlas.image(PVZ2_LOGO, Scaling.fit);
    }

    public BorderedTable dialogPanel() {
        BorderedTable panel = new BorderedTable();
        panel.pad(30f, 40f, 32f, 40f);
        return panel;
    }

    public Label title(String text) {
        Label label = new Label(text, skin, "big_outline");
        label.setAlignment(Align.center);
        return label;
    }

    public Label fieldLabel(String text) {
        Label label = new Label(text, skin, "secondary");
        label.setAlignment(Align.left);
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

    public TextButton primaryButton(String text) {
        return new TextButton(text, skin, "green");
    }

    public TextButton secondaryButton(String text) {
        return new TextButton(text, skin, "brown");
    }

    public Label statusLabel() {
        Label label = new Label("", skin, "secondary");
        label.setAlignment(Align.center);
        label.setWrap(true);
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
