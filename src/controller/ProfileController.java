package controller;

import model.User;
import model.UserRepository;

import java.io.IOException;

public class ProfileController {
    private final AuthController authController;
    private final UserRepository userRepository;

    public ProfileController(AuthController authController) {
        this.authController = authController;
        this.userRepository = authController.getUserRepository();
    }

    public ActionResult showProfile() {
        if (authController.getCurrentUser() == null) {
            return ActionResult.failure("You must login first.");
        }
        return ActionResult.success("Profile loaded successfully.");
    }

    public User getCurrentUser() {
        return authController.getCurrentUser();
    }

    public ActionResult changeUsername(String newUsername) {
        User user = authController.getCurrentUser();
        if (user == null) {
            return ActionResult.failure("You must login first.");
        }
        String error = InputValidator.validateUsername(newUsername);
        if (error != null) {
            return ActionResult.failure(error);
        }
        if (user.getUsername().equals(newUsername)) {
            return ActionResult.failure("New username is the same as the current username.");
        }
        if (userRepository.usernameExists(newUsername)) {
            return ActionResult.failure("Username already exists.");
        }

        String oldUsername = user.getUsername();
        user.changeUsername(newUsername);
        try {
            userRepository.rename(oldUsername, newUsername, user);
            authController.refreshSavedSessionUsername();
            return ActionResult.success("Username changed successfully.");
        } catch (IOException | IllegalArgumentException exception) {
            user.changeUsername(oldUsername);
            return ActionResult.failure("Could not change username: " + exception.getMessage());
        }
    }

    public ActionResult changeNickname(String newNickname) {
        User user = authController.getCurrentUser();
        if (user == null) {
            return ActionResult.failure("You must login first.");
        }
        String error = InputValidator.validateNickname(newNickname);
        if (error != null) {
            return ActionResult.failure(error);
        }
        if (user.getNickname().equals(newNickname)) {
            return ActionResult.failure("New nickname is the same as the current nickname.");
        }
        user.changeNickname(newNickname);
        return saveProfileChange("Nickname changed successfully.");
    }

    public ActionResult changeEmail(String newEmail) {
        User user = authController.getCurrentUser();
        if (user == null) {
            return ActionResult.failure("You must login first.");
        }
        String error = InputValidator.validateEmail(newEmail);
        if (error != null) {
            return ActionResult.failure(error);
        }
        if (user.getEmail().equals(newEmail)) {
            return ActionResult.failure("New email is the same as the current email.");
        }
        user.changeEmail(newEmail);
        return saveProfileChange("Email changed successfully.");
    }

    public ActionResult changePassword(String oldPassword, String newPassword) {
        User user = authController.getCurrentUser();
        if (user == null) {
            return ActionResult.failure("You must login first.");
        }
        if (!user.checkPassword(oldPassword)) {
            return ActionResult.failure("Old password is incorrect.");
        }
        if (user.checkPassword(newPassword)) {
            return ActionResult.failure("New password is the same as the current password.");
        }
        String error = InputValidator.validatePassword(newPassword);
        if (error != null) {
            return ActionResult.failure(error);
        }
        user.changePassword(newPassword);
        return saveProfileChange("Password changed successfully.");
    }

    private ActionResult saveProfileChange(String successMessage) {
        try {
            userRepository.save();
            return ActionResult.success(successMessage);
        } catch (IOException exception) {
            return ActionResult.failure("Could not save profile changes.");
        }
    }
}
