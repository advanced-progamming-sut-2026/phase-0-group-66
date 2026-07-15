package view;

public class RegisterView {
    public void showRegisterForm() {
        System.out.println("register -u <username> -p <password> <password_confirm> "
            + "-n <nickname> -e <email> -g <gender>");
        System.out.println("pick question -q <question_number> -a <answer> -c <answer_confirm>");
        System.out.println("menu enter Login Menu");
    }

    public void showSecurityQuestionForm(String questions) {
        System.out.println(questions);
    }

    public void showMessage(String message) {
        System.out.println(message);
    }
}
