package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import controller.ActionResult;
import pvz.PvzApplication;
import pvz.skin.BorderedTable;

public final class ForgotPasswordScreen extends BaseUiScreen {
    private static final float PANEL_WIDTH = 690f;
    private static final float PANEL_HEIGHT = 585f;
    private static final float CONTENT_WIDTH = 500f;

    private enum Step {
        ACCOUNT,
        QUESTION,
        RESET
    }

    private Step step = Step.ACCOUNT;
    private String questionText = "";
    private String statusMessage = "";
    private boolean statusError;

    public ForgotPasswordScreen(PvzApplication app) {
        super(app);
        rebuild();
    }

    private void rebuild() {
        root.clearChildren();
        BorderedTable panel = theme.dialogPanel();
        panel.padTop(26f).padBottom(14f);
        addHeader(panel);
        if (step == Step.ACCOUNT) {
            buildAccountStep(panel);
        } else if (step == Step.QUESTION) {
            buildQuestionStep(panel);
        } else {
            buildResetStep(panel);
        }
        addStatus(panel);
        root.add(panel).width(PANEL_WIDTH).height(PANEL_HEIGHT);
    }

    private void addHeader(Table panel) {
        Image logo = theme.pvzLogo();
        if (logo != null) {
            panel.add(logo).width(260f).height(44f);
            panel.row().padTop(4f);
        }
        panel.add(theme.title("PASSWORD RECOVERY"));
        panel.row().padTop(16f);
    }

    private void buildAccountStep(Table panel) {
        panel.add(theme.bodyLabel("Enter the username and email connected to your account."))
            .width(CONTENT_WIDTH);
        panel.row().padTop(16f);
        TextField username = textField("Username");
        TextField email = textField("Email");
        addRecoveryField(panel, "Username", username);
        addRecoveryField(panel, "Email", email);
        addButtons(panel, "Back to login", app::showLogin, "Continue", () -> startRecovery(username, email));
        stage.setKeyboardFocus(username);
    }

    private void buildQuestionStep(Table panel) {
        Table questionBox = theme.insetPanel(16f);
        questionBox.add(theme.heading("SECURITY QUESTION"));
        questionBox.row().padTop(6f);
        questionBox.add(theme.bodyLabel(questionText)).width(440f);
        panel.add(questionBox).width(CONTENT_WIDTH);
        panel.row().padTop(18f);
        TextField answer = textField("Security answer");
        addRecoveryField(panel, "Answer", answer);
        addButtons(panel, "Back", this::restartRecovery, "Continue", () -> verifyAnswer(answer));
        stage.setKeyboardFocus(answer);
    }

    private void buildResetStep(Table panel) {
        panel.add(theme.bodyLabel("Choose a new password for your account.")).width(CONTENT_WIDTH);
        panel.row().padTop(16f);
        TextField password = passwordField("New password");
        TextField confirm = passwordField("Confirm new password");
        addRecoveryField(panel, "New password", password);
        addRecoveryField(panel, "Confirm password", confirm);
        addButtons(panel, "Cancel", app::showLogin, "Reset password", () -> resetPassword(password, confirm));
        stage.setKeyboardFocus(password);
    }

    private void addRecoveryField(Table panel, String title, TextField field) {
        panel.add(theme.fieldLabel(title)).left().width(CONTENT_WIDTH);
        panel.row().padTop(3f);
        panel.add(field).width(CONTENT_WIDTH).height(FIELD_HEIGHT);
        panel.row().padTop(12f);
    }

    private void addButtons(Table panel, String leftText, Runnable leftAction,
                            String rightText, Runnable rightAction) {
        TextButton left = theme.secondaryButton(leftText);
        TextButton right = theme.primaryButton(rightText);
        UiActions.onClick(left, leftAction);
        UiActions.onClick(right, rightAction);
        Table buttons = new Table();
        buttons.add(left).width(200f).height(56f).padRight(12f);
        buttons.add(right).width(230f).height(56f);
        panel.add(buttons);
    }

    private void addStatus(Table panel) {
        panel.row().padTop(10f);
        Label status = statusLabel();
        if (!statusMessage.isBlank()) {
            if (statusError) {
                theme.showError(status, statusMessage);
            } else {
                theme.showSuccess(status, statusMessage);
            }
        }
        panel.add(status).width(CONTENT_WIDTH).height(46f);
    }

    private void startRecovery(TextField username, TextField email) {
        ActionResult result = app.services().auth().forgetPassword(
            username.getText().trim(), email.getText().trim());
        if (!result.isSuccessful()) {
            setStatus(result.getMessage(), true);
            rebuild();
            return;
        }
        questionText = extractQuestion(result.getMessage());
        setStatus("Account found. Answer your security question.", false);
        step = Step.QUESTION;
        rebuild();
    }

    private void verifyAnswer(TextField answer) {
        ActionResult result = app.services().auth().answerSecurityQuestion(answer.getText());
        if (!result.isSuccessful()) {
            step = Step.ACCOUNT;
            setStatus(result.getMessage() + " Start recovery again.", true);
            rebuild();
            return;
        }
        step = Step.RESET;
        setStatus("Security answer verified.", false);
        rebuild();
    }

    private void resetPassword(TextField password, TextField confirm) {
        ActionResult result = app.services().auth().resetPassword(password.getText(), confirm.getText());
        if (!result.isSuccessful()) {
            setStatus(result.getMessage(), true);
            rebuild();
            return;
        }
        app.showLogin();
    }

    private void restartRecovery() {
        step = Step.ACCOUNT;
        questionText = "";
        setStatus("", false);
        rebuild();
    }

    private void setStatus(String message, boolean error) {
        statusMessage = message == null ? "" : message;
        statusError = error;
    }

    private String extractQuestion(String message) {
        String prefix = "Security question:";
        if (message != null && message.startsWith(prefix)) {
            return message.substring(prefix.length()).trim();
        }
        return message == null ? "Security question" : message;
    }
}
