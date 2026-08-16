package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import controller.ActionResult;
import model.SecurityQuestion;
import pvz.PvzApplication;
import pvz.skin.BorderedTable;

public final class SecurityQuestionScreen extends BaseUiScreen {
    private static final float PANEL_WIDTH = 700f;
    private static final float PANEL_HEIGHT = 605f;
    private static final float CONTENT_WIDTH = 500f;

    private final SelectBox<String> questions;
    private final TextField answer;
    private final TextField answerConfirm;
    private final Label status;

    public SecurityQuestionScreen(PvzApplication app) {
        super(app);
        questions = new SelectBox<>(app.assets().skin());
        questions.setItems(
            SecurityQuestion.FAVORITE_COLOR.getText(),
            SecurityQuestion.FIRST_SCHOOL.getText(),
            SecurityQuestion.FAVORITE_FOOD.getText()
        );
        answer = textField("Answer");
        answerConfirm = textField("Confirm answer");
        status = statusLabel();
        buildUi();
    }

    private void buildUi() {
        BorderedTable panel = theme.dialogPanel();
        panel.padTop(26f).padBottom(14f);
        addHeader(panel);
        panel.add(theme.fieldLabel("Recovery question")).left().width(CONTENT_WIDTH);
        panel.row().padTop(4f);
        panel.add(questions).width(CONTENT_WIDTH).height(52f);
        panel.row().padTop(15f);
        addSecurityField(panel, "Answer", answer);
        addSecurityField(panel, "Confirm answer", answerConfirm);
        addActions(panel);
        panel.row().padTop(10f);
        panel.add(status).width(CONTENT_WIDTH).height(48f);
        root.add(panel).width(PANEL_WIDTH).height(PANEL_HEIGHT);
        stage.setKeyboardFocus(answer);
    }

    private void addHeader(Table panel) {
        Image logo = theme.pvzLogo();
        if (logo != null) {
            panel.add(logo).width(270f).height(46f);
            panel.row().padTop(4f);
        }
        panel.add(theme.title("SECURITY QUESTION"));
        panel.row().padTop(8f);
        panel.add(theme.bodyLabel("Choose a question you can answer later.")).width(CONTENT_WIDTH);
        panel.row().padTop(16f);
    }

    private void addSecurityField(Table panel, String title, TextField field) {
        panel.add(theme.fieldLabel(title)).left().width(CONTENT_WIDTH);
        panel.row().padTop(3f);
        panel.add(field).width(CONTENT_WIDTH).height(FIELD_HEIGHT);
        panel.row().padTop(12f);
    }

    private void addActions(Table panel) {
        TextButton back = theme.secondaryButton("Back");
        TextButton finish = theme.primaryButton("Create account");
        UiActions.onClick(back, app::showRegister);
        UiActions.onClick(finish, this::finishRegistration);
        Table buttons = new Table();
        buttons.add(back).width(180f).height(56f).padRight(12f);
        buttons.add(finish).width(250f).height(56f);
        panel.add(buttons);
    }

    private void finishRegistration() {
        int questionNumber = questions.getSelectedIndex() + 1;
        ActionResult result = app.services().auth().pickSecurityQuestion(
            questionNumber,
            answer.getText(),
            answerConfirm.getText()
        );
        if (!result.isSuccessful()) {
            theme.showError(status, result.getMessage());
            return;
        }
        theme.showSuccess(status, result.getMessage());
        app.showLogin();
    }
}
