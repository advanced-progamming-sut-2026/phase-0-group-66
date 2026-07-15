package view;

public class SettingsView {
    public void showSettings() {
        System.out.println("menu settings change-difficulty -l <difficulty_level>");
    }

    public void showDifficultyOptions() {
        System.out.println("Difficulty level must be between 1 and 5.");
    }

    public void showMessage(String message) {
        System.out.println(message);
    }
}
