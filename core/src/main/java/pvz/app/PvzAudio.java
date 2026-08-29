package pvz.app;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Disposable;
import model.Chapter;
import pvz.assets.PvzAssets;

import java.util.HashMap;
import java.util.Map;

public final class PvzAudio implements Disposable {
    public static final String MENU_MUSIC = "menu backgroung audio.mp3";
    public static final String WIN_SOUND = "win audio.mp3";
    public static final String LOSS_SOUND = "loss audio.mp3";
    public static final String LAWN_MOWER_SOUND = "lownmower.mp3";
    public static final String EXPLOSION_SOUND = "explosion audio.mp3";
    public static final String ZOMBIES_SOUND = "zombies audio.mp3";
    public static final String ZOMBIES_COMING_SOUND = "zombies are comming.mp3";
    public static final String ZOMBOSS_MUSIC = "zomboss levels.mp3";

    private final FileHandle audioDirectory;
    private final AudioSettings settings;
    private final Map<String, Sound> sounds = new HashMap<>();
    private Music music;
    private String musicFileName;

    public PvzAudio(PvzAssets assets, AudioSettings settings) {
        if (assets == null || settings == null) {
            throw new IllegalArgumentException("Audio dependencies cannot be null.");
        }
        audioDirectory = assets.root().child("audio");
        this.settings = settings;
    }

    public void playMenuMusic() {
        playMusic(MENU_MUSIC);
    }

    public void playChapterMusic(Chapter chapter) {
        if (chapter == null) {
            playMenuMusic();
            return;
        }
        playMusic(switch (chapter.getSeason()) {
            case FROSTBITE_CAVES -> "frostbite caves chapter.mp3";
            case DARK_AGES -> "dark ages chapter.mp3";
            case BIG_WAVE_BEACH -> "big wave beach chapter.mp3";
            case ANCIENT_EGYPT -> "ancient egypt chapter.mp3";
        });
    }

    public void playZombossMusic() {
        playMusic(ZOMBOSS_MUSIC);
    }

    public void playSfx(String fileName) {
        Sound sound = loadSound(fileName);
        if (sound != null) {
            sound.play(settings.getSfxVolume());
        }
    }

    public void update() {
        if (music != null) {
            music.setVolume(settings.getMusicVolume());
        }
    }

    private void playMusic(String fileName) {
        if (fileName == null || fileName.equals(musicFileName)) {
            update();
            return;
        }
        FileHandle file = audioDirectory.child(fileName);
        if (!file.exists() || file.isDirectory()) {
            return;
        }
        if (music != null) {
            music.stop();
            music.dispose();
        }
        music = com.badlogic.gdx.Gdx.audio.newMusic(file);
        musicFileName = fileName;
        music.setLooping(true);
        music.setVolume(settings.getMusicVolume());
        music.play();
    }

    private Sound loadSound(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        Sound cached = sounds.get(fileName);
        if (cached != null) {
            return cached;
        }
        FileHandle file = audioDirectory.child(fileName);
        if (!file.exists() || file.isDirectory()) {
            return null;
        }
        Sound sound = com.badlogic.gdx.Gdx.audio.newSound(file);
        sounds.put(fileName, sound);
        return sound;
    }

    @Override
    public void dispose() {
        for (Sound sound : sounds.values()) {
            sound.dispose();
        }
        sounds.clear();
        if (music != null) {
            music.stop();
            music.dispose();
            music = null;
        }
        musicFileName = null;
    }
}
