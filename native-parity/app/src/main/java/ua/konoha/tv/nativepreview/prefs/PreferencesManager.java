package ua.konoha.tv.nativepreview.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ua.konoha.tv.nativepreview.model.CatalogData;
import ua.konoha.tv.nativepreview.model.HighlightSound;
import ua.konoha.tv.nativepreview.model.MusicTrack;
import ua.konoha.tv.nativepreview.model.Scene;
import ua.konoha.tv.nativepreview.model.Technique;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PreferencesManager {
    public static final String PREF_NAME = "konoha_preferences_v1";

    public static final String KEY_SCENE = "scene";
    public static final String KEY_AUDIO = "audio";
    public static final String KEY_AMBIENCE = "ambience";
    public static final String KEY_VOLUME = "volume";
    public static final String KEY_MUSIC_VOLUME = "musicVolume";
    public static final String KEY_MUSIC = "music";
    private static final String KEY_MUSIC_ROTATION = "musicRotation";
    private static final String KEY_SHOW_RESOURCES = "showResources";
    public static final String KEY_TECHNIQUE = "technique";
    public static final String KEY_HIGHLIGHT_SOUND = "highlightSound";
    public static final String KEY_MOTION = "motion";
    public static final String KEY_ROTATION = "rotation";
    public static final String KEY_IDLE = "idle";
    public static final String KEY_PINS = "pins";

    public static final String KEY_RUNNER_NARUTO = "runner_best_naruto";
    public static final String KEY_RUNNER_GAMABUNTA = "runner_best_gamabunta";
    public static final String KEY_RUNNER_HERO = "runner_last_hero";

    private final SharedPreferences prefs;

    public PreferencesManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public PreferencesManager(SharedPreferences sharedPreferences) {
        this.prefs = sharedPreferences;
    }

    public String getScene() {
        String val = prefs.getString(KEY_SCENE, "dusk");
        if ("night".equals(val)) val = "team7"; // Відповідно до app.js
        return val != null ? val : "dusk";
    }

    public void setScene(String scene) {
        prefs.edit().putString(KEY_SCENE, scene).apply();
    }

    public boolean isAudioEnabled() {
        return prefs.getBoolean(KEY_AUDIO, true);
    }

    public void setAudioEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUDIO, enabled).apply();
    }

    public boolean isAmbienceEnabled() {
        return prefs.getBoolean(KEY_AMBIENCE, true);
    }

    public void setAmbienceEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AMBIENCE, enabled).apply();
    }

    public float getVolume() {
        return prefs.getFloat(KEY_VOLUME, 0.45f);
    }

    public void setVolume(float volume) {
        float clamped = Math.max(0f, Math.min(1f, volume));
        prefs.edit().putFloat(KEY_VOLUME, clamped).apply();
    }

    public float getMusicVolume() {
        return prefs.getFloat(KEY_MUSIC_VOLUME, 0.22f);
    }

    public void setMusicVolume(float volume) {
        float clamped = Math.max(0f, Math.min(1f, volume));
        prefs.edit().putFloat(KEY_MUSIC_VOLUME, clamped).apply();
    }

    public String getMusicTrack() {
        return prefs.getString(KEY_MUSIC, "1584791975");
    }

    public void setMusicTrack(String trackId) {
        prefs.edit().putString(KEY_MUSIC, trackId).apply();
    }

    public boolean isMusicRotationEnabled() {
        return prefs.getBoolean(KEY_MUSIC_ROTATION, false);
    }

    public void setMusicRotationEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_MUSIC_ROTATION, enabled).apply();
    }

    public boolean shouldShowResources() {
        return prefs.getBoolean(KEY_SHOW_RESOURCES, false);
    }

    public void setShowResources(boolean show) {
        prefs.edit().putBoolean(KEY_SHOW_RESOURCES, show).apply();
    }

    public String getTechnique() {
        return prefs.getString(KEY_TECHNIQUE, CatalogData.DEFAULT_TECHNIQUE_ID);
    }

    public void setTechnique(String technique) {
        prefs.edit().putString(KEY_TECHNIQUE, technique).apply();
    }

    public String getHighlightSound() {
        return prefs.getString(KEY_HIGHLIGHT_SOUND, "jutsu");
    }

    public void setHighlightSound(String sound) {
        prefs.edit().putString(KEY_HIGHLIGHT_SOUND, sound).apply();
    }

    public boolean isMotionEnabled() {
        return prefs.getBoolean(KEY_MOTION, true);
    }

    public void setMotionEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_MOTION, enabled).apply();
    }

    public int getRotationSeconds() {
        int r = prefs.getInt(KEY_ROTATION, 60);
        if (r != 0 && r != 60 && r != 180 && r != 600) r = 0;
        return r;
    }

    public void setRotationSeconds(int seconds) {
        prefs.edit().putInt(KEY_ROTATION, seconds).apply();
    }

    public int getIdleSeconds() {
        int i = prefs.getInt(KEY_IDLE, 900);
        if (i != 0 && i != 300 && i != 900) i = 0;
        return i;
    }

    public void setIdleSeconds(int seconds) {
        prefs.edit().putInt(KEY_IDLE, seconds).apply();
    }

    public List<String> getPins() {
        String raw = prefs.getString(KEY_PINS, null);
        if (raw == null) return null;
        try {
            JSONArray arr = new JSONArray(raw);
            List<String> list = new ArrayList<>(arr.length());
            for (int i = 0; i < arr.length(); i++) {
                list.add(arr.getString(i));
            }
            return list;
        } catch (JSONException e) {
            return Collections.emptyList();
        }
    }

    public void setPins(List<String> pins) {
        if (pins == null) {
            prefs.edit().remove(KEY_PINS).apply();
            return;
        }
        JSONArray arr = new JSONArray();
        for (String p : pins) {
            arr.put(p);
        }
        prefs.edit().putString(KEY_PINS, arr.toString()).apply();
    }

    public int getRunnerBest(String hero) {
        if ("gamabunta".equals(hero)) {
            return prefs.getInt(KEY_RUNNER_GAMABUNTA, 0);
        }
        return prefs.getInt(KEY_RUNNER_NARUTO, 0);
    }

    public void setRunnerBest(String hero, int score) {
        String key = "gamabunta".equals(hero) ? KEY_RUNNER_GAMABUNTA : KEY_RUNNER_NARUTO;
        int current = prefs.getInt(key, 0);
        if (score > current) {
            prefs.edit().putInt(key, score).apply();
        }
    }

    public String getRunnerHero() {
        return prefs.getString(KEY_RUNNER_HERO, "naruto");
    }

    public void setRunnerHero(String hero) {
        prefs.edit().putString(KEY_RUNNER_HERO, hero).apply();
    }

    /**
     * Імпорт збережених налаштувань із формату v26 Web localStorage (`konoha.preferences.v1`)
     */
    public boolean importFromWebJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) return false;
        try {
            JSONObject obj = new JSONObject(jsonString);
            SharedPreferences.Editor edit = prefs.edit();

            if (obj.has("scene") && !obj.isNull("scene")) {
                String s = obj.getString("scene");
                if ("night".equals(s)) s = "team7";
                boolean valid = false;
                for (Scene sc : CatalogData.SCENES) {
                    if (sc.getId().equals(s)) { valid = true; break; }
                }
                edit.putString(KEY_SCENE, valid ? s : "team7");
            }
            if (obj.has("audio") && !obj.isNull("audio")) edit.putBoolean(KEY_AUDIO, obj.getBoolean("audio"));
            if (obj.has("ambience") && !obj.isNull("ambience")) edit.putBoolean(KEY_AMBIENCE, obj.getBoolean("ambience"));
            if (obj.has("volume") && !obj.isNull("volume")) {
                double v = obj.optDouble("volume", Double.NaN);
                if (!Double.isNaN(v) && !Double.isInfinite(v)) {
                    float clamped = (float) Math.max(0.0, Math.min(1.0, v));
                    edit.putFloat(KEY_VOLUME, clamped);
                }
            }
            if (obj.has("musicVolume") && !obj.isNull("musicVolume")) {
                double mv = obj.optDouble("musicVolume", Double.NaN);
                if (!Double.isNaN(mv) && !Double.isInfinite(mv)) {
                    float clamped = (float) Math.max(0.0, Math.min(1.0, mv));
                    edit.putFloat(KEY_MUSIC_VOLUME, clamped);
                }
            }
            if (obj.has("music") && !obj.isNull("music")) {
                String m = obj.optString("music", "");
                boolean valid = false;
                for (MusicTrack mt : CatalogData.MUSIC_TRACKS) {
                    if (mt.getId().equals(m)) { valid = true; break; }
                }
                edit.putString(KEY_MUSIC, valid ? m : "1584791975");
            }
            if (obj.has("technique") && !obj.isNull("technique")) {
                String t = obj.optString("technique", "");
                boolean valid = false;
                for (Technique tech : CatalogData.TECHNIQUES) {
                    if (tech.getId().equals(t)) { valid = true; break; }
                }
                edit.putString(KEY_TECHNIQUE, valid ? t : CatalogData.DEFAULT_TECHNIQUE_ID);
            }
            if (obj.has("highlightSound") && !obj.isNull("highlightSound")) {
                String hs = obj.optString("highlightSound", "");
                boolean valid = false;
                for (HighlightSound h : CatalogData.HIGHLIGHT_SOUNDS) {
                    if (h.getId().equals(hs)) { valid = true; break; }
                }
                edit.putString(KEY_HIGHLIGHT_SOUND, valid ? hs : "jutsu");
            }
            if (obj.has("motion") && !obj.isNull("motion")) edit.putBoolean(KEY_MOTION, obj.getBoolean("motion"));
            if (obj.has("rotation") && !obj.isNull("rotation")) {
                int r = obj.optInt("rotation", 0);
                if (r != 0 && r != 60 && r != 180 && r != 600) r = 0;
                edit.putInt(KEY_ROTATION, r);
            }
            if (obj.has("idle") && !obj.isNull("idle")) {
                int i = obj.optInt("idle", 0);
                if (i != 0 && i != 300 && i != 900) i = 0;
                edit.putInt(KEY_IDLE, i);
            }
            if (obj.has("pins")) {
                if (obj.isNull("pins")) {
                    edit.remove(KEY_PINS);
                } else {
                    Object pinsVal = obj.opt("pins");
                    if (pinsVal instanceof JSONArray) {
                        JSONArray arr = (JSONArray) pinsVal;
                        JSONArray validPins = new JSONArray();
                        for (int k = 0; k < arr.length(); k++) {
                            Object item = arr.opt(k);
                            if (item instanceof String) {
                                String p = ((String) item).trim();
                                if (!p.isEmpty()) {
                                    validPins.put(p);
                                }
                            }
                        }
                        edit.putString(KEY_PINS, validPins.toString());
                    }
                    // Non-array malformed values are ignored to preserve existing pins
                }
            }

            edit.apply();
            return true;
        } catch (JSONException e) {
            return false;
        }
    }
}
