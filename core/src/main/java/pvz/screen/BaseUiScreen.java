package pvz.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.viewport.FitViewport;
import pvz.PvzApplication;

public abstract class BaseUiScreen extends ScreenAdapter {
    protected static final float WORLD_WIDTH = 1280f;
    protected static final float WORLD_HEIGHT = 720f;
    protected static final float FIELD_WIDTH = 390f;

    protected final PvzApplication app;
    protected final FitViewport viewport;
    protected final Stage stage;
    protected final Table root;

    protected BaseUiScreen(PvzApplication app) {
        this.app = app;
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT);
        stage = new Stage(viewport);
        root = new Table();
        root.setFillParent(true);
        stage.addActor(root);
    }

    protected TextField textField(String hint) {
        TextField field = new TextField("", app.assets().skin());
        field.setMessageText(hint);
        return field;
    }

    protected TextField passwordField(String hint) {
        TextField field = textField(hint);
        field.setPasswordMode(true);
        field.setPasswordCharacter('*');
        return field;
    }

    protected Label statusLabel() {
        Label label = new Label("", app.assets().skin(), "secondary");
        label.setWrap(true);
        return label;
    }

    protected void addField(Table table, String title, TextField field) {
        table.add(new Label(title, app.assets().skin(), "secondary")).left();
        table.row().padTop(4f);
        table.add(field).width(FIELD_WIDTH).height(48f);
        table.row().padTop(12f);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        app.assets().update();
        Gdx.gl.glClearColor(0.09f, 0.13f, 0.09f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(Math.min(delta, 1f / 30f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void hide() {
        if (Gdx.input.getInputProcessor() == stage) {
            Gdx.input.setInputProcessor(null);
        }
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
