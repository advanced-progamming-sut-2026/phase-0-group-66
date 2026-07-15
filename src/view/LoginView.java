package view;

public class LoginView {
    public void showLoginForm() {
        System.out.println("login -u <username> -p <password> [-stay-logged-in]");
        System.out.println("forget password -u <username> -e <email>");
        System.out.println("answer -a <answer>");
        System.out.println("reset password -p <new_password> <password_confirm>");
    }

    public void showForgetPasswordForm() {
        System.out.println("forget password -u <username> -e <email>");
    }

    public void showMessage(String message) {
        System.out.println(message);
    }
}
