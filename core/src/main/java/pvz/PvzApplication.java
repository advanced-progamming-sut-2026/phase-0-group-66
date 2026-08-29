package pvz;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.utils.GdxRuntimeException;
import controller.ActionResult;
import pvz.app.AudioSettings;
import pvz.app.DisplaySettings;
import pvz.app.PvzServices;
import pvz.app.PvzAudio;
import pvz.assets.PvzAssets;
import pvz.screen.AdventureScreen;
import pvz.screen.BattleScreen;
import pvz.screen.ChapterLevelsScreen;
import pvz.screen.CheatScreen;
import pvz.screen.CollectionScreen;
import pvz.screen.BeghouledScreen;
import pvz.screen.ForgotPasswordScreen;
import pvz.screen.GreenhouseScreen;
import pvz.screen.LevelBriefingScreen;
import pvz.screen.LeaderboardScreen;
import pvz.screen.LoginScreen;
import pvz.screen.MainMenuScreen;
import pvz.screen.MiniGameHubScreen;
import pvz.screen.MiniGamePreviewScreen;
import pvz.screen.NewsScreen;
import pvz.screen.NetworkScreen;
import pvz.screen.IZombieScreen;
import pvz.screen.PlantSelectionScreen;
import pvz.screen.PlayerListScreen;
import pvz.screen.QuestScreen;
import pvz.screen.ProfileScreen;
import pvz.screen.RegisterScreen;
import pvz.screen.SecurityQuestionScreen;
import pvz.screen.SettingsScreen;
import pvz.screen.ShopScreen;
import pvz.screen.VasebreakerScreen;
import pvz.screen.WallnutBowlingScreen;
import pvz.screen.ZombotanyScreen;
import model.Chapter;
import model.Level;
import model.MiniGameType;
import network.game.MatchTicket;

import java.io.IOException;

public final class PvzApplication extends Game {
    private PvzAssets assets;
    private PvzServices services;
    private DisplaySettings displaySettings;
    private AudioSettings audioSettings;
    private PvzAudio audio;
    private boolean miniGamesOpenedFromQuests;

    @Override
    public void create() {
        displaySettings = new DisplaySettings();
        displaySettings.apply();
        audioSettings = new AudioSettings();

        try {
            assets = new PvzAssets();
            audio = new PvzAudio(assets, audioSettings);
            services = new PvzServices();
        } catch (IOException exception) {
            throw new GdxRuntimeException("Could not initialize PVZ data.", exception);
        }

        if (services.auth().restoreSession()) {
            showMainMenu();
        } else {
            showRegister();
        }
    }

    public PvzAssets assets() {
        return assets;
    }

    public PvzServices services() {
        return services;
    }

    public DisplaySettings displaySettings() {
        return displaySettings;
    }

    public AudioSettings audioSettings() {
        return audioSettings;
    }

    public PvzAudio audio() {
        return audio;
    }

    public void showRegister() {
        changeScreen(new RegisterScreen(this));
    }

    public void showSecurityQuestion() {
        changeScreen(new SecurityQuestionScreen(this));
    }

    public void showLogin() {
        changeScreen(new LoginScreen(this));
    }

    public void showForgotPassword() {
        changeScreen(new ForgotPasswordScreen(this));
    }

    public void showMainMenu() {
        if (!services.auth().isAuthenticated()) {
            showLogin();
            return;
        }
        audio.playMenuMusic();
        changeScreen(new MainMenuScreen(this));
    }

    public void showPlayerList() {
        if (!services.auth().isAuthenticated()) {
            showLogin();
            return;
        }
        changeScreen(new PlayerListScreen(this));
    }


    public void showAdventure() {
        if (!services.auth().isAuthenticated()) {
            showLogin();
            return;
        }
        audio.playChapterMusic(null);
        changeScreen(new AdventureScreen(this));
    }

    public void showChapterLevels(Chapter chapter) {
        if (!services.auth().isAuthenticated()) {
            showLogin();
            return;
        }
        audio.playChapterMusic(chapter);
        changeScreen(new ChapterLevelsScreen(this, chapter));
    }

    public void showLevelBriefing(Chapter chapter, Level level) {
        if (!services.auth().isAuthenticated()) {
            showLogin();
            return;
        }
        audio.playChapterMusic(chapter);
        changeScreen(new LevelBriefingScreen(this, chapter, level));
    }

    public void showPlantSelection(Chapter chapter, Level level) {
        if (!services.auth().isAuthenticated()) {
            showLogin();
            return;
        }
        audio.playChapterMusic(chapter);
        changeScreen(new PlantSelectionScreen(this, chapter, level));
    }

    public void showBattle(Chapter chapter, Level level) {
        if (!services.auth().isAuthenticated()) {
            showLogin();
            return;
        }
        audio.playChapterMusic(chapter);
        changeScreen(new BattleScreen(this, chapter, level));
    }

    public void showProfile() {
        if (!services.auth().isAuthenticated()) {
            showLogin();
            return;
        }
        changeScreen(new ProfileScreen(this));
    }

    public void showSettings() {
        if (!services.auth().isAuthenticated()) {
            showLogin();
            return;
        }
        changeScreen(new SettingsScreen(this));
    }

    public void showNews() {
        if (!services.auth().isAuthenticated()) {
            showLogin();
            return;
        }
        changeScreen(new NewsScreen(this));
    }

    public void showNetwork() {
        if (!services.auth().isAuthenticated()) {
            showLogin();
            return;
        }
        audio.playMenuMusic();
        changeScreen(new NetworkScreen(this));
    }


    public void showCollection() {
        if (!services.auth().isAuthenticated()) {
            showLogin();
            return;
        }
        changeScreen(new CollectionScreen(this));
    }

    public void showQuests() {
        if (!services.auth().isAuthenticated()) {
            showLogin();
            return;
        }
        changeScreen(new QuestScreen(this));
    }

    public void showLeaderboard() {
        if (!services.auth().isAuthenticated()) {
            showLogin();
            return;
        }
        changeScreen(new LeaderboardScreen(this));
    }

    public void showCheats() {
        if (!services.auth().isAuthenticated()) {
            showLogin();
            return;
        }
        changeScreen(new CheatScreen(this));
    }

    public void showGreenhouse() {
        if (!services.auth().isAuthenticated()) {
            showLogin();
            return;
        }
        changeScreen(new GreenhouseScreen(this));
    }

    public void showShop() {
        if (!services.auth().isAuthenticated()) {
            showLogin();
            return;
        }
        changeScreen(new ShopScreen(this));
    }

    public void showMiniGames() {
        miniGamesOpenedFromQuests = false;
        audio.playMenuMusic();
        openMiniGameHub();
    }

    public void showMiniGamesFromQuests() {
        miniGamesOpenedFromQuests = true;
        audio.playMenuMusic();
        openMiniGameHub();
    }

    public void returnToMiniGames() {
        openMiniGameHub();
    }

    public String miniGameBackText() {
        return miniGamesOpenedFromQuests ? "Back to Quests" : "Back to Adventure";
    }

    public void leaveMiniGames() {
        if (miniGamesOpenedFromQuests) {
            showQuests();
        } else {
            showAdventure();
        }
    }

    private void openMiniGameHub() {
        if (!services.auth().isAuthenticated()) {
            showLogin();
            return;
        }
        changeScreen(new MiniGameHubScreen(this));
    }

    public boolean startMiniGame(MiniGameType type, int level) {
        if (!services.auth().isAuthenticated()) {
            showLogin();
            return false;
        }
        ActionResult result = services.miniGames().startMiniGame(type.name(), level);
        if (!result.isSuccessful()) {
            return false;
        }
        changeScreen(new MiniGamePreviewScreen(this, type));
        return true;
    }

    public boolean startOnlineIZombie(MatchTicket ticket) {
        if (ticket == null || !ticket.isMatched()) {
            return false;
        }
        ActionResult result = services.miniGames().startOnlineIZombie(
            ticket.level(), ticket.matchId(), ticket.role(), ticket.opponent()
        );
        if (!result.isSuccessful()) {
            return false;
        }
        changeScreen(new IZombieScreen(this));
        return true;
    }

    public boolean startCouchIZombie(int level) {
        ActionResult result = services.miniGames().startCouchIZombie(level);
        if (!result.isSuccessful()) {
            return false;
        }
        changeScreen(new IZombieScreen(this));
        return true;
    }

    public void playMiniGame(MiniGameType type) {
        if (!services.auth().isAuthenticated()) {
            showLogin();
            return;
        }
        if (services.miniGames().getCurrentSession() == null
            || services.miniGames().getCurrentSession().getDefinition().type() != type) {
            returnToMiniGames();
            return;
        }
        switch (type) {
            case VASEBREAKER -> changeScreen(new VasebreakerScreen(this));
            case WALLNUT_BOWLING -> changeScreen(new WallnutBowlingScreen(this));
            case I_ZOMBIE -> {
                audio.playZombossMusic();
                changeScreen(new IZombieScreen(this));
            }
            case BEGHOULD -> changeScreen(new BeghouledScreen(this));
            case ZOMBOTANY -> changeScreen(new ZombotanyScreen(this));
        }
    }

    private void changeScreen(Screen next) {
        Screen previous = getScreen();
        setScreen(next);
        if (previous != null) {
            Gdx.app.postRunnable(previous::dispose);
        }
    }

    @Override
    public void dispose() {
        if (services != null) {
            services.auth().saveCurrentState();
        }
        if (getScreen() != null) {
            getScreen().dispose();
        }
        if (assets != null) {
            if (audio != null) {
                audio.dispose();
            }
            assets.dispose();
        }
    }
}
