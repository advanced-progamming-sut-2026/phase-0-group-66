package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import controller.ActionResult;
import pvz.PvzApplication;

public final class ForgotPasswordScreen extends BaseUiScreen {
    private enum Step {
        ACCOUNT,
        QUESTION,
        RESET
    }

    private Step step = Step.ACCOUNT;
    private String prompt = "Enter your username and email.";

    public ForgotPasswordScreen(PvzApplication app) {
        super(app);
        rebuild();
    }

    private void rebuild() {
        root.clearChildren();
        Table form = new Table();
        form.add(new Label("PASSWORD RECOVERY", app.assets().skin(), "big_outline"));
        form.row().padTop(18f);
        Label info = new Label(prompt, app.assets().skin(), "secondary");
        info.setWrap(true);
        form.add(info).width(540f);
        form.row().padTop(18f);

        if (step == Step.ACCOUNT) {
            buildAccountStep(form);
        } else if (step == Step.QUESTION) {
            buildQuestionStep(form);
        } else {
            buildResetStep(form);
        }
        root.add(form);
    }

    private void buildAccountStep(Table form) {
        TextField username = textField("Username");
        TextField email = textField("Email");
        addField(form, "Username", username);
        addField(form, "Email", email);
        TextButton next = new TextButton("Continue", app.assets().skin(), "green");
        TextButton back = new TextButton("Back to login", app.assets().skin(), "brown");
        UiActions.onClick(next, () -> startRecovery(username, email));
        UiActions.onClick(back, app::showLogin);
        addButtons(form, back, next);
        stage.setKeyboardFocus(username);
    }

    private void buildQuestionStep(Table form) {
        TextField answer = textField("Security answer");
        addField(form, "Answer", answer);
        TextButton verify = new TextButton("Verify", app.assets().skin(), "green");
        TextButton cancel = new TextButton("Cancel", app.assets().skin(), "brown");
        UiActions.onClick(verify, () -> verifyAnswer(answer));
        UiActions.onClick(cancel, app::showLogin);
        addButtons(form, cancel, verify);
        stage.setKeyboardFocus(answer);
    }

    private void buildResetStep(Table form) {
        TextField password = passwordField("New password");
        TextField confirm = passwordField("Confirm new password");
        addField(form, "New password", password);
        addField(form, "Confirm password", confirm);
        TextButton reset = new TextButton("Reset password", app.assets().skin(), "green");
        TextButton cancel = new TextButton("Cancel", app.assets().skin(), "brown");
        UiActions.onClick(reset, () -> resetPassword(password, confirm));
        UiActions.onClick(cancel, app::showLogin);
        addButtons(form, cancel, reset);
        stage.setKeyboardFocus(password);
    }

    private void startRecovery(TextField username, TextField email) {
        ActionResult result = app.services().auth().forgetPassword(
            username.getText().trim(), email.getText().trim());
        prompt = result.getMessage();
        if (result.isSuccessful()) {
            step = Step.QUESTION;
        }
        rebuild();
    }

    private void verifyAnswer(TextField answer) {
        ActionResult result = app.services().auth().answerSecurityQuestion(answer.getText());
        prompt = result.getMessage();
        if (result.isSuccessful()) {
            step = Step.RESET;
        } else {
            step = Step.ACCOUNT;
        }
        rebuild();
    }

    private void resetPassword(TextField password, TextField confirm) {
        ActionResult result = app.services().auth().resetPassword(password.getText(), confirm.getText());
        prompt = result.getMessage();
        if (result.isSuccessful()) {
            app.showLogin();
            return;
        }
        rebuild();
    }

    private void addButtons(Table form, TextButton left, TextButton right) {
        Table buttons = new Table();
        buttons.add(left).width(190f).height(58f).padRight(12f);
        buttons.add(right).width(220f).height(58f);
        form.add(buttons);
    }
}
