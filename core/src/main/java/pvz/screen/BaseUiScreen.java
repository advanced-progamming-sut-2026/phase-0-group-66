package pvz.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.math.Vector2;
import pvz.PvzApplication;
import pvz.ui.UiTheme;

public abstract class BaseUiScreen extends ScreenAdapter {
    protected static final float WORLD_WIDTH = 1280f;
    protected static final float WORLD_HEIGHT = 720f;
    protected static final float FIELD_WIDTH = 390f;
    protected static final float FIELD_HEIGHT = 56f;

    protected final PvzApplication app;
    protected final FitViewport viewport;
    protected final Stage stage;
    protected final Stack layers;
    protected final Table root;
    protected final UiTheme theme;
    private final Image cursor;
    private final Table cursorLayer;
    private final Vector2 cursorPosition = new Vector2();

    protected BaseUiScreen(PvzApplication app) {
        this.app = app;
        theme = app.assets().uiTheme();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT);
        stage = new Stage(viewport);
        layers = new Stack();
        layers.setFillParent(true);

        Image background = theme.screenBackground();
        root = new Table();
        root.setFillParent(true);

        layers.add(background);
        layers.add(root);
        cursor = theme.image(UiTheme.CURSOR_ICON);
        cursorLayer = new Table();
        cursorLayer.setFillParent(true);
        cursorLayer.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
        cursorLayer.top().left();
        if (cursor != null) {
            cursor.setSize(34f, 42f);
            cursor.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
            cursor.setVisible(false);
            cursorLayer.add(cursor).size(34f, 42f);
        }
        layers.add(cursorLayer);
        stage.addActor(layers);
    }

    protected TextField textField(String hint) {
        return theme.textField(hint);
    }

    protected TextField passwordField(String hint) {
        return theme.passwordField(hint);
    }

    protected Label statusLabel() {
        return theme.statusLabel();
    }

    protected void addField(Table table, String title, TextField field) {
        table.add(theme.fieldLabel(title)).left();
        table.row().padTop(4f);
        table.add(field).width(FIELD_WIDTH).height(FIELD_HEIGHT);
        table.row().padTop(12f);
    }

    @Override
    public void show() {
        if (cursor != null) {
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.None);
        }
        Gdx.input.setInputProcessor(new InputMultiplexer(stage, new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    handleEscape();
                    return true;
                }
                return false;
            }
        }));
    }

    protected void handleEscape() {
        // Screens with a dedicated escape action override this hook.
    }

    @Override
    public void render(float delta) {
        app.assets().update();
        Gdx.gl.glClearColor(0.02f, 0.04f, 0.07f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(Math.min(delta, 1f / 30f));
        updateCursor();
        stage.draw();
    }

    private void updateCursor() {
        if (cursor == null) {
            return;
        }
        cursorPosition.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(cursorPosition);
        cursor.setPosition(cursorPosition.x - 4f, cursorPosition.y - cursor.getHeight() + 4f);
        cursor.setVisible(true);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void hide() {
        if (cursor != null) {
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
        }
        if (Gdx.input.getInputProcessor() instanceof InputMultiplexer) {
            Gdx.input.setInputProcessor(null);
        }
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
