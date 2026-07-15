public class User {
    private String username;
    private String passwordHash;
    private String nickname;
    private String email;
    private String gender;
    private int difficultyLevel;
    private GameProgress progress;
    private Wallet wallet;
    private Inventory inventory;
    private CollectionBook collectionBook;
    private News news;

    public void changeUsername(String newUsername) {
    }

    public void changeNickname(String newNickname) {
    }

    public void changeEmail(String newEmail) {
    }

    public void changePassword(String newPasswordHash) {
    }

    public boolean checkPassword(String password) {
        return false;
    }

    public GameProgress getProgress() {
        return null;
    }
}
