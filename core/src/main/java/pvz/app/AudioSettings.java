package pvz.app;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.math.MathUtils;

public final class AudioSettings {
    private static final String PREFERENCES_NAME = "pvz2-audio-settings";
    private static final String MUSIC_VOLUME_KEY = "musicVolume";
    private static final String SFX_VOLUME_KEY = "sfxVolume";

    private static final float DEFAULT_MUSIC_VOLUME = 0.80f;
    private static final float DEFAULT_SFX_VOLUME = 0.90f;

    private final Preferences preferences;
    private float musicVolume;
    private float sfxVolume;

    public AudioSettings() {
        preferences = Gdx.app.getPreferences(PREFERENCES_NAME);
        musicVolume = preferences.getFloat(MUSIC_VOLUME_KEY, DEFAULT_MUSIC_VOLUME);
        sfxVolume = preferences.getFloat(SFX_VOLUME_KEY, DEFAULT_SFX_VOLUME);
    }

    public float getMusicVolume() {
        return musicVolume;
    }

    public float getSfxVolume() {
        return sfxVolume;
    }

    public void setMusicVolume(float volume) {
        musicVolume = MathUtils.clamp(volume, 0f, 1f);
        save();
    }

    public void setSfxVolume(float volume) {
        sfxVolume = MathUtils.clamp(volume, 0f, 1f);
        save();
    }

    public void resetDefaults() {
        musicVolume = DEFAULT_MUSIC_VOLUME;
        sfxVolume = DEFAULT_SFX_VOLUME;
        save();
    }

    private void save() {
        preferences.putFloat(MUSIC_VOLUME_KEY, musicVolume);
        preferences.putFloat(SFX_VOLUME_KEY, sfxVolume);
        preferences.flush();
    }
}
