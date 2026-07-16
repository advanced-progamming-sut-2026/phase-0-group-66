package controller;

import java.util.Locale;
import java.util.regex.Pattern;

public final class InputValidator {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9-]+");
    private static final Pattern EMAIL_LOCAL_PATTERN =
        Pattern.compile("[A-Za-z0-9](?:[A-Za-z0-9._-]*[A-Za-z0-9])?");
    private static final Pattern DOMAIN_LABEL_PATTERN =
        Pattern.compile("[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?");
    private static final String ALLOWED_SPECIALS = "!#$%^&*()=+}{[]|/\\:;'\",><?";

    private InputValidator() {
    }

    public static String validateUsername(String username) {
        if (username == null || username.isEmpty()) {
            return "Username cannot be empty.";
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return "Username may only contain English letters, digits, and '-'.";
        }
        return null;
    }

    public static String validatePassword(String password) {
        if (password == null || password.length() < 8) {
            return "Password must contain at least 8 characters.";
        }
        boolean hasLowercase = false;
        boolean hasUppercase = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char character : password.toCharArray()) {
            if (character >= 'a' && character <= 'z') {
                hasLowercase = true;
            } else if (character >= 'A' && character <= 'Z') {
                hasUppercase = true;
            } else if (Character.isDigit(character)) {
                hasDigit = true;
            } else if (ALLOWED_SPECIALS.indexOf(character) >= 0) {
                hasSpecial = true;
            } else {
                return "Password contains an unsupported character.";
            }
        }
        if (!hasLowercase) {
            return "Password must contain a lowercase letter.";
        }
        if (!hasUppercase) {
            return "Password must contain an uppercase letter.";
        }
        if (!hasDigit) {
            return "Password must contain a digit.";
        }
        if (!hasSpecial) {
            return "Password must contain a special character.";
        }
        return null;
    }

    public static String validateNickname(String nickname) {
        if (nickname == null || nickname.length() < 3 || nickname.length() > 30) {
            return "Nickname length must be between 3 and 30 characters.";
        }
        return null;
    }

    public static String validateEmail(String email) {
        if (email == null || email.chars().filter(character -> character == '@').count() != 1) {
            return "Email must contain exactly one '@'.";
        }
        String[] parts = email.split("@", -1);
        String localPart = parts[0];
        String domain = parts[1];

        if (!EMAIL_LOCAL_PATTERN.matcher(localPart).matches() || localPart.contains("..")) {
            return "Email username is invalid.";
        }
        if (domain.contains("..") || !domain.contains(".")) {
            return "Email domain is invalid.";
        }
        String[] labels = domain.split("\\.", -1);
        for (String label : labels) {
            if (!DOMAIN_LABEL_PATTERN.matcher(label).matches()) {
                return "Email domain is invalid.";
            }
        }
        String topLevelDomain = labels[labels.length - 1];
        if (topLevelDomain.length() < 2 || !topLevelDomain.chars().allMatch(Character::isLetter)) {
            return "Email domain suffix must contain at least two letters.";
        }
        return null;
    }

    public static String validateGender(String gender) {
        if (normalizeGender(gender) == null) {
            return "Gender must be male/female";
        }
        return null;
    }

    public static String normalizeGender(String gender) {
        if (gender == null) {
            return null;
        }
        String normalized = gender.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("male") || normalized.equals("man")) {
            return "male";
        }
        if (normalized.equals("female") || normalized.equals("woman")) {
            return "female";
        }
        return null;
    }
}
