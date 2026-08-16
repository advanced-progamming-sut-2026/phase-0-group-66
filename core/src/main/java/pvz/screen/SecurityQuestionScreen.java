package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import controller.ActionResult;
import model.SecurityQuestion;
import pvz.PvzApplication;

public final class SecurityQuestionScreen extends BaseUiScreen {
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
        Table form = new Table();
        form.add(new Label("SECURITY QUESTION", app.assets().skin(), "big_outline"));
        form.row().padTop(22f);
        form.add(new Label("Choose a recovery question", app.assets().skin(), "secondary")).left();
        form.row().padTop(5f);
        form.add(questions).width(520f).height(50f);
        form.row().padTop(14f);
        addField(form, "Answer", answer);
        addField(form, "Confirm answer", answerConfirm);

        TextButton finish = new TextButton("Create account", app.assets().skin(), "green");
        TextButton back = new TextButton("Back", app.assets().skin(), "brown");
        UiActions.onClick(finish, this::finishRegistration);
        UiActions.onClick(back, app::showRegister);

        Table buttons = new Table();
        buttons.add(back).width(170f).height(58f).padRight(12f);
        buttons.add(finish).width(230f).height(58f);
        form.add(buttons);
        form.row().padTop(12f);
        form.add(status).width(540f);
        root.add(form);
        stage.setKeyboardFocus(answer);
    }

    private void finishRegistration() {
        int questionNumber = questions.getSelectedIndex() + 1;
        ActionResult result = app.services().auth().pickSecurityQuestion(
            questionNumber,
            answer.getText(),
            answerConfirm.getText()
        );
        status.setText(result.getMessage());
        if (result.isSuccessful()) {
            app.showLogin();
        }
    }
}
