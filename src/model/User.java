package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String passwordHash;
    private String passwordSalt;
    private String nickname;
    private String email;
    private String gender;
    private int difficultyLevel;
    private SecurityQuestion securityQuestion;
    private String securityAnswerHash;
    private String securityAnswerSalt;
    private final GameProgress progress;
    private final Wallet wallet;
    private final Inventory inventory;
    private final CollectionBook collectionBook;
    private final ArrayList<News> news;

    public User(String username, String password, String nickname, String email, String gender,
                SecurityQuestion securityQuestion, String securityAnswer) {
        this.username = username;
        this.nickname = nickname;
        this.email = email;
        this.gender = gender;
        this.difficultyLevel = 3;
        this.progress = new GameProgress();
        this.wallet = new Wallet();
        this.inventory = new Inventory();
        this.collectionBook = new CollectionBook();
        this.news = new ArrayList<>();
        this.securityQuestion = securityQuestion;
        setPassword(password);
        setSecurityAnswer(securityAnswer);
    }

    public String getUsername() {
        return username;
    }

    public String getNickname() {
        return nickname;
    }

    public String getEmail() {
        return email;
    }

    public String getGender() {
        return gender;
    }

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public SecurityQuestion getSecurityQuestion() {
        return securityQuestion;
    }

    public GameProgress getProgress() {
        return progress;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public CollectionBook getCollectionBook() {
        return collectionBook;
    }

    public List<News> getNews() {
        return Collections.unmodifiableList(news);
    }

    public void changeUsername(String newUsername) {
        username = newUsername;
    }

    public void changeNickname(String newNickname) {
        nickname = newNickname;
    }

    public void changeEmail(String newEmail) {
        email = newEmail;
    }

    public void changePassword(String newPassword) {
        setPassword(newPassword);
    }

    public void setDifficultyLevel(int difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public boolean checkPassword(String password) {
        return CredentialHasher.matches(password, passwordSalt, passwordHash);
    }

    public boolean checkSecurityAnswer(String answer) {
        String normalized = normalizeAnswer(answer);
        return CredentialHasher.matches(normalized, securityAnswerSalt, securityAnswerHash);
    }

    public void addNews(News item) {
        if (item != null) {
            news.add(item);
        }
    }

    private void setPassword(String password) {
        passwordSalt = CredentialHasher.generateSalt();
        passwordHash = CredentialHasher.hash(password, passwordSalt);
    }

    private void setSecurityAnswer(String answer) {
        securityAnswerSalt = CredentialHasher.generateSalt();
        securityAnswerHash = CredentialHasher.hash(normalizeAnswer(answer), securityAnswerSalt);
    }

    private String normalizeAnswer(String answer) {
        return answer == null ? "" : answer.trim().toLowerCase(Locale.ROOT);
    }
}
