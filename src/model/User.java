package model;

import java.io.IOException;
import java.io.ObjectInputStream;
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
    private GameProgress progress;
    private Wallet wallet;
    private Inventory inventory;
    private CollectionBook collectionBook;
    private ArrayList<News> news;
    private Greenhouse greenhouse;
    private ShopState shopState;
    private QuestLog questLog;

    public User(String username, String password, String nickname, String email, String gender,
                SecurityQuestion securityQuestion, String securityAnswer) {
        this.username = username;
        this.nickname = nickname;
        this.email = email;
        this.gender = gender.trim().toUpperCase(Locale.ROOT);
        this.difficultyLevel = 3;
        this.progress = new GameProgress();
        this.wallet = new Wallet();
        this.inventory = new Inventory();
        this.collectionBook = new CollectionBook();
        this.news = new ArrayList<>();
        this.greenhouse = new Greenhouse();
        this.shopState = new ShopState();
        this.questLog = new QuestLog();
        this.securityQuestion = securityQuestion;
        unlockStarterContent();
        setPassword(password);
        setSecurityAnswer(securityAnswer);
        news.add(new News("Welcome", "Your account is ready. Ancient Egypt level 1 is unlocked."));
    }

    public String getUsername() { return username; }
    public String getNickname() { return nickname; }
    public String getEmail() { return email; }
    public String getGender() { return gender; }
    public Gender getGenderValue() { return Gender.valueOf(gender); }
    public int getDifficultyLevel() { return difficultyLevel; }
    public SecurityQuestion getSecurityQuestion() { return securityQuestion; }
    public GameProgress getProgress() { return progress; }
    public Wallet getWallet() { return wallet; }
    public Inventory getInventory() { return inventory; }
    public CollectionBook getCollectionBook() { return collectionBook; }
    public Greenhouse getGreenhouse() { return greenhouse; }
    public ShopState getShopState() { return shopState; }
    public QuestLog getQuestLog() { return questLog; }
    public List<News> getNews() { return Collections.unmodifiableList(news); }

    public void changeUsername(String newUsername) { username = newUsername; }
    public void changeNickname(String newNickname) { nickname = newNickname; }
    public void changeEmail(String newEmail) { email = newEmail; }
    public void changePassword(String newPassword) { setPassword(newPassword); }
    public void setDifficultyLevel(int difficultyLevel) { this.difficultyLevel = difficultyLevel; }

    public boolean checkPassword(String password) {
        return CredentialHasher.matches(password, passwordSalt, passwordHash);
    }

    public boolean checkSecurityAnswer(String answer) {
        return CredentialHasher.matches(normalizeAnswer(answer), securityAnswerSalt, securityAnswerHash);
    }

    public void addNews(News item) {
        if (item != null) {
            news.add(item);
        }
    }

    public void ensureStarterContent() { unlockStarterContent(); }

    private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
        input.defaultReadObject();
        if (progress == null) {
            progress = new GameProgress();
        }
        if (wallet == null) {
            wallet = new Wallet();
        }
        if (inventory == null) {
            inventory = new Inventory();
        }
        if (collectionBook == null) {
            collectionBook = new CollectionBook();
        }
        if (news == null) {
            news = new ArrayList<>();
        }
        if (greenhouse == null) {
            greenhouse = new Greenhouse();
        }
        if (shopState == null) {
            shopState = new ShopState();
        }
        if (questLog == null) {
            questLog = new QuestLog();
        }
        if (gender != null) {
            gender = gender.trim().toUpperCase(Locale.ROOT);
        }
        ensureStarterContent();
    }

    private void unlockStarterContent() {
        collectionBook.unlockPlant("Sunflower");
        collectionBook.unlockPlant("Peashooter");
        collectionBook.unlockPlant("Wall-nut");
        progress.unlockChapterName("Ancient Egypt");
        progress.unlockLevelId("ancient-egypt-1");
        progress.unlockMiniGameLevel(MiniGameType.VASEBREAKER, 1);
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
