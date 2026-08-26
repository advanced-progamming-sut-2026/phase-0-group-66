package controller;

import model.SecurityQuestion;
import model.User;
import model.UserRepository;

import java.io.IOException;
import java.util.Optional;

public class AuthController {
    private final UserRepository userRepository;
    private User currentUser;
    private PendingRegistration pendingRegistration;
    private User recoveryUser;
    private boolean recoveryAnswerVerified;
    private boolean stayLoggedIn;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public ActionResult register(String username, String password, String passwordConfirm,
                                 String nickname, String email, String gender) {
        String error = InputValidator.validateUsername(username);
        if (error == null && userRepository.usernameExists(username)) {
            error = "Username already exists.";
        }
        if (error == null) {
            error = InputValidator.validatePassword(password);
        }
        if (error == null && !password.equals(passwordConfirm)) {
            error = "Password and password confirmation do not match.";
        }
        if (error == null) {
            error = InputValidator.validateNickname(nickname);
        }
        if (error == null) {
            error = InputValidator.validateEmail(email);
        }
        if (error == null) {
            error = InputValidator.validateGender(gender);
        }
        if (error != null) {
            pendingRegistration = null;
            return ActionResult.failure(error);
        }

        pendingRegistration = new PendingRegistration(username, password, nickname, email,
            InputValidator.normalizeGender(gender));
        return ActionResult.success("Registration information is valid. Choose a security question:\n"
            + SecurityQuestion.formattedList());
    }

    public ActionResult pickSecurityQuestion(int questionNumber, String answer, String answerConfirm) {
        if (pendingRegistration == null) {
            return ActionResult.failure("First enter valid registration information.");
        }
        SecurityQuestion question = SecurityQuestion.fromNumber(questionNumber);
        if (question == null) {
            return ActionResult.failure("Security question number is invalid.");
        }
        if (answer == null || answer.trim().isEmpty()) {
            return ActionResult.failure("Security answer cannot be empty.");
        }
        if (!answer.trim().equalsIgnoreCase(answerConfirm == null ? "" : answerConfirm.trim())) {
            return ActionResult.failure("Security answer and confirmation do not match.");
        }

        PendingRegistration registration = pendingRegistration;
        User user = new User(registration.username, registration.password, registration.nickname,
            registration.email, registration.gender, question, answer);
        try {
            userRepository.add(user);
            pendingRegistration = null;
            return ActionResult.success("User registered successfully. Please login.");
        } catch (IOException | IllegalArgumentException exception) {
            return ActionResult.failure("Could not save the new user: " + exception.getMessage());
        }
    }

    public ActionResult login(String username, String password, boolean keepLoggedIn) {
        Optional<User> foundUser = userRepository.findByUsername(username);
        if (foundUser.isEmpty()) {
            return ActionResult.failure("Username does not exist.");
        }
        User user = foundUser.get();
        if (!user.checkPassword(password)) {
            return ActionResult.failure("Password is incorrect.");
        }

        currentUser = user;
        stayLoggedIn = keepLoggedIn;
        recoveryUser = null;
        recoveryAnswerVerified = false;
        try {
            if (keepLoggedIn) {
                userRepository.saveSession(user.getUsername());
            } else {
                userRepository.clearSession();
            }
        } catch (IOException exception) {
            currentUser = null;
            stayLoggedIn = false;
            return ActionResult.failure("Login succeeded, but the session could not be saved.");
        }
        return ActionResult.success("Logged in successfully.");
    }

    public ActionResult logout() {
        currentUser = null;
        stayLoggedIn = false;
        pendingRegistration = null;
        recoveryUser = null;
        recoveryAnswerVerified = false;
        try {
            userRepository.clearSession();
            return ActionResult.success("Logged out successfully.");
        } catch (IOException exception) {
            return ActionResult.failure("Logged out, but the saved session could not be removed.");
        }
    }

    public ActionResult selectUser(String username) {
        Optional<User> foundUser = userRepository.findByUsername(username);
        if (foundUser.isEmpty()) {
            return ActionResult.failure("Username does not exist.");
        }
        currentUser = foundUser.get();
        try {
            if (stayLoggedIn) {
                userRepository.saveSession(currentUser.getUsername());
            }
            return ActionResult.success("Profile selected.");
        } catch (IOException exception) {
            return ActionResult.failure("Profile selected, but the session could not be saved.");
        }
    }

    public ActionResult deleteUser(String username) {
        Optional<User> foundUser = userRepository.findByUsername(username);
        if (foundUser.isEmpty()) {
            return ActionResult.failure("Username does not exist.");
        }
        try {
            if (!userRepository.delete(username)) {
                return ActionResult.failure("Could not delete the account.");
            }
            if (currentUser != null && currentUser.getUsername().equals(username)) {
                currentUser = null;
                stayLoggedIn = false;
                userRepository.clearSession();
            }
            return ActionResult.success("Account deleted.");
        } catch (IOException exception) {
            return ActionResult.failure("Could not delete the account.");
        }
    }

    public ActionResult forgetPassword(String username, String email) {
        Optional<User> foundUser = userRepository.findByUsername(username);
        if (foundUser.isEmpty() || !foundUser.get().getEmail().equals(email)) {
            recoveryUser = null;
            recoveryAnswerVerified = false;
            return ActionResult.failure("Username and email do not match an account.");
        }
        recoveryUser = foundUser.get();
        recoveryAnswerVerified = false;
        return ActionResult.success("Security question: " + recoveryUser.getSecurityQuestion().getText());
    }

    public ActionResult answerSecurityQuestion(String answer) {
        if (recoveryUser == null) {
            return ActionResult.failure("Start password recovery first.");
        }
        if (!recoveryUser.checkSecurityAnswer(answer)) {
            recoveryUser = null;
            recoveryAnswerVerified = false;
            return ActionResult.failure("Security answer is incorrect.");
        }
        recoveryAnswerVerified = true;
        return ActionResult.success("Answer is correct. Use: reset password -p <new_password> "
            + "<password_confirm>");
    }

    public ActionResult resetPassword(String newPassword, String passwordConfirm) {
        if (recoveryUser == null || !recoveryAnswerVerified) {
            return ActionResult.failure("Verify the security answer first.");
        }
        String error = InputValidator.validatePassword(newPassword);
        if (error != null) {
            return ActionResult.failure(error);
        }
        if (!newPassword.equals(passwordConfirm)) {
            return ActionResult.failure("Password and password confirmation do not match.");
        }
        if (recoveryUser.checkPassword(newPassword)) {
            return ActionResult.failure("New password must be different from the current password.");
        }

        recoveryUser.changePassword(newPassword);
        try {
            userRepository.save();
            recoveryUser = null;
            recoveryAnswerVerified = false;
            return ActionResult.success("Password changed successfully. You can now login.");
        } catch (IOException exception) {
            return ActionResult.failure("Could not save the new password.");
        }
    }

    public boolean restoreSession() {
        try {
            Optional<String> sessionUsername = userRepository.loadSessionUsername();
            if (sessionUsername.isEmpty()) {
                return false;
            }
            Optional<User> user = userRepository.findByUsername(sessionUsername.get());
            if (user.isEmpty()) {
                userRepository.clearSession();
                return false;
            }
            currentUser = user.get();
            stayLoggedIn = true;
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    public boolean isAuthenticated() {
        return currentUser != null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    public boolean isStayLoggedIn() {
        return stayLoggedIn;
    }

    public ActionResult saveCurrentState() {
        try {
            userRepository.save();
            if (stayLoggedIn && currentUser != null) {
                userRepository.saveSession(currentUser.getUsername());
            }
            return ActionResult.success("Data saved successfully.");
        } catch (IOException exception) {
            return ActionResult.failure("Could not save data: " + exception.getMessage());
        }
    }

    public void refreshSavedSessionUsername() throws IOException {
        if (stayLoggedIn && currentUser != null) {
            userRepository.saveSession(currentUser.getUsername());
        }
    }

    private static final class PendingRegistration {
        private final String username;
        private final String password;
        private final String nickname;
        private final String email;
        private final String gender;

        private PendingRegistration(String username, String password, String nickname,
                                    String email, String gender) {
            this.username = username;
            this.password = password;
            this.nickname = nickname;
            this.email = email;
            this.gender = gender;
        }
    }
}
