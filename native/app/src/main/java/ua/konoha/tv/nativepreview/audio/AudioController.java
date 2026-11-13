package ua.konoha.tv.nativepreview.audio;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;

import ua.konoha.tv.nativepreview.model.CatalogData;
import ua.konoha.tv.nativepreview.model.HighlightSound;
import ua.konoha.tv.nativepreview.prefs.PreferencesManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AudioController {
    public enum MusicState {
        IDLE, PREPARING, PREPARED, PLAYING, PAUSED, STOPPED, ERROR
    }

    private final Context context;
    private final PreferencesManager prefs;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final java.util.Set<MediaPlayer> ownedPlayers = new java.util.HashSet<>();
    private final Map<MediaPlayer, java.util.List<Runnable>> fadeCallbacks = new HashMap<>();
    private boolean disposed;

    private SoundPool soundPool;
    private final Map<String, Integer> soundMap = new HashMap<>();
    private final Map<Integer, Boolean> loadedSounds = new HashMap<>();

    private MediaPlayer musicPlayer;
    private MusicState musicState = MusicState.IDLE;
    private MediaPlayer overlapPlayer;
    private MusicState overlapState = MusicState.STOPPED;
    private MediaPlayer pendingMusicPlayer;
    private String currentMusicTrackId = null;
    private boolean isMusicPaused = false;
    private long musicGeneration = 0;
    private Runnable loopScheduleRunnable = null;

    private MediaPlayer confirmPlayer;
    private Runnable pendingLaunchRunnable;
    private Runnable failsafeRunnable;
    private boolean isLaunching = false;
    private long launchGeneration = 0;

    private int activeFocusStreamId = 0;

    public AudioController(Context context, PreferencesManager prefs) {
        this.context = context.getApplicationContext();
        this.prefs = prefs;
        initSoundPool();
    }

    private void initSoundPool() {
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(attrs)
                .build();

        soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> {
            if (status == 0) {
                loadedSounds.put(sampleId, true);
            }
        });

        // Попереднє завантаження UI звуку та звуків виділення
        loadSound("ui", "audio/v5-ui-confirm.wav");
        for (HighlightSound hs : CatalogData.HIGHLIGHT_SOUNDS) {
            loadSound(hs.getId(), hs.getCueAsset());
        }
    }

    private synchronized void loadSound(String key, String assetPath) {
        try (AssetFileDescriptor afd = context.getAssets().openFd(assetPath)) {
            int soundId = soundPool.load(afd, 1);
            soundMap.put(key, soundId);
            loadedSounds.put(soundId, false);
        } catch (IOException ignored) {}
    }

    public synchronized void playUiSound() {
        if (disposed || !prefs.isAudioEnabled()) return;
        Integer soundId = soundMap.get("ui");
        if (soundId != null && Boolean.TRUE.equals(loadedSounds.get(soundId))) {
            float vol = prefs.getVolume() * 0.4f;
            soundPool.play(soundId, vol, vol, 1, 0, 1.0f);
        }
    }

    public synchronized void playFocusSound() {
        if (disposed || !prefs.isAudioEnabled() || isLaunching) return;
        String soundKey = prefs.getHighlightSound();
        Integer soundId = soundMap.get(soundKey);
        if (soundId == null) {
            loadSound(soundKey, "audio/v7-seal-complete-focus.wav");
            soundId = soundMap.get(soundKey);
        }

        if (soundId != null && Boolean.TRUE.equals(loadedSounds.get(soundId))) {
            if (activeFocusStreamId != 0) {
                soundPool.stop(activeFocusStreamId);
            }
            float vol = prefs.getVolume() * 0.7f;
            activeFocusStreamId = soundPool.play(soundId, vol, vol, 1, 0, 1.0f);
        }
    }

    // --- Підсистема музики з явним життєвим циклом, crossfade та 3.5с перекриттям циклу ---

    public synchronized MusicState getMusicState() {
        return musicState;
    }

    public synchronized void updateMusic() {
        if (disposed) return;
        if (!prefs.isAudioEnabled() || !prefs.isAmbienceEnabled()) {
            stopMusic();
            return;
        }

        if (isMusicPaused) return;

        String trackId = prefs.getMusicTrack();
        float targetVol = prefs.getMusicVolume();

        if (musicPlayer != null && musicState != MusicState.ERROR && trackId.equals(currentMusicTrackId)) {
            if (musicState == MusicState.PLAYING) {
                try {
                    musicPlayer.setVolume(targetVol, targetVol);
                } catch (Exception ignored) {}
            } else if (musicState == MusicState.PAUSED && !isMusicPaused) {
                resumeMusic();
            }
            return;
        }

        // Перемикання треку: плавний crossfade
        if (musicPlayer != null && musicState == MusicState.PLAYING) {
            cancelFades(musicPlayer);
            safeReleasePlayer(pendingMusicPlayer);
            pendingMusicPlayer = null;
            safeReleasePlayer(overlapPlayer);
            overlapPlayer = musicPlayer;
            overlapState = MusicState.PLAYING;
            musicPlayer = null;
            musicState = MusicState.IDLE;
            startFadeOut(overlapPlayer, 1200);
        } else {
            stopMusic();
        }

        startTrack(trackId, targetVol, true);
    }

    private void startTrack(String trackId, float targetVol, boolean fadeIn) {
        if (loopScheduleRunnable != null) {
            mainHandler.removeCallbacks(loopScheduleRunnable);
            loopScheduleRunnable = null;
        }
        currentMusicTrackId = trackId;
        final long currentGen = ++musicGeneration;
        musicState = MusicState.PREPARING;
        String assetPath = "audio/" + trackId + ".m4a";

        safeReleasePlayer(pendingMusicPlayer);
        pendingMusicPlayer = null;

        MediaPlayer mp = null;
        try (AssetFileDescriptor afd = context.getAssets().openFd(assetPath)) {
            mp = new MediaPlayer();
            ownedPlayers.add(mp);
            musicPlayer = mp;
            mp.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mp.setLooping(false); // Власне потокове 3.5с перекриття замість різкого setLooping
            mp.setVolume(0.0f, 0.0f);

            final MediaPlayer playerInstance = mp;
            playerInstance.setOnPreparedListener(player -> {
                synchronized (AudioController.this) {
                    if (currentGen != musicGeneration || player != musicPlayer) {
                        safeReleasePlayer(player);
                        if (musicPlayer == player) musicPlayer = null;
                        return;
                    }
                    musicState = MusicState.PREPARED;
                    if (isMusicPaused || !prefs.isAudioEnabled() || !prefs.isAmbienceEnabled()) {
                        musicState = MusicState.PAUSED;
                        // Не стартуємо у стані паузи; гучність буде встановлена при resume
                        return;
                    }
                    try {
                        player.start();
                        musicState = MusicState.PLAYING;
                        if (fadeIn) {
                            startFadeIn(player, currentGen, 1200);
                        } else {
                            float v = prefs.getMusicVolume();
                            player.setVolume(v, v);
                        }
                        scheduleLoopOverlap(player, currentGen);
                    } catch (Exception e) {
                        if (loopScheduleRunnable != null) {
                            mainHandler.removeCallbacks(loopScheduleRunnable);
                            loopScheduleRunnable = null;
                        }
                        safeReleasePlayer(player);
                        musicPlayer = null;
                        musicState = MusicState.ERROR;
                    }
                }
            });

            playerInstance.setOnErrorListener((player, what, extra) -> {
                synchronized (AudioController.this) {
                    if (currentGen == musicGeneration && player == musicPlayer) {
                        if (loopScheduleRunnable != null) {
                            mainHandler.removeCallbacks(loopScheduleRunnable);
                            loopScheduleRunnable = null;
                        }
                        safeReleasePlayer(player);
                        musicPlayer = null;
                        musicState = MusicState.ERROR;
                    } else {
                        safeReleasePlayer(player);
                    }
                }
                return true;
            });

            playerInstance.setOnCompletionListener(player -> {
                synchronized (AudioController.this) {
                    if (currentGen == musicGeneration && player == musicPlayer) {
                        if (loopScheduleRunnable != null) {
                            mainHandler.removeCallbacks(loopScheduleRunnable);
                            loopScheduleRunnable = null;
                        }
                        safeReleasePlayer(player);
                        musicPlayer = null;
                        musicState = MusicState.IDLE;
                        updateMusic();
                    }
                }
            });

            playerInstance.prepareAsync();
        } catch (Exception e) {
            if (loopScheduleRunnable != null) {
                mainHandler.removeCallbacks(loopScheduleRunnable);
                loopScheduleRunnable = null;
            }
            if (mp != null) {
                safeReleasePlayer(mp);
            }
            musicPlayer = null;
            musicState = MusicState.ERROR;
        }
    }

    private void scheduleLoopOverlap(MediaPlayer mp, long gen) {
        if (loopScheduleRunnable != null) {
            mainHandler.removeCallbacks(loopScheduleRunnable);
            loopScheduleRunnable = null;
        }
        try {
            int duration = mp.getDuration();
            if (duration <= 8000) return;
            final int fadeMs = Math.min(3500, duration / 4);
            int triggerMs = duration - fadeMs;
            int currentPos = 0;
            try { currentPos = mp.getCurrentPosition(); } catch (Exception ignored) {}
            long delay = Math.max(50, triggerMs - currentPos);

            loopScheduleRunnable = () -> {
                synchronized (AudioController.this) {
                    if (gen != musicGeneration || mp != musicPlayer || musicState != MusicState.PLAYING || isMusicPaused) {
                        return;
                    }
                    triggerLoopOverlap(currentMusicTrackId, fadeMs, gen);
                }
            };
            mainHandler.postDelayed(loopScheduleRunnable, delay);
        } catch (Exception ignored) {}
    }

    private void triggerLoopOverlap(String trackId, int fadeMs, long parentGen) {
        if (disposed || isMusicPaused || trackId == null) return;
        String assetPath = "audio/" + trackId + ".m4a";

        safeReleasePlayer(pendingMusicPlayer);
        pendingMusicPlayer = null;
        safeReleasePlayer(overlapPlayer);
        overlapPlayer = null;
        overlapState = MusicState.STOPPED;

        MediaPlayer nextMp = null;
        try (AssetFileDescriptor afd = context.getAssets().openFd(assetPath)) {
            nextMp = new MediaPlayer();
            ownedPlayers.add(nextMp);
            pendingMusicPlayer = nextMp;
            nextMp.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            nextMp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            nextMp.setLooping(false);
            nextMp.setVolume(0f, 0f);

            final MediaPlayer candidate = nextMp;
            candidate.setOnPreparedListener(player -> {
                synchronized (AudioController.this) {
                    if (parentGen != musicGeneration || player != pendingMusicPlayer || isMusicPaused || musicState != MusicState.PLAYING) {
                        safeReleasePlayer(player);
                        if (pendingMusicPlayer == player) pendingMusicPlayer = null;
                        return;
                    }
                    pendingMusicPlayer = null;
                    try {
                        player.start();
                        safeReleasePlayer(overlapPlayer);
                        overlapPlayer = musicPlayer;
                        overlapState = MusicState.PLAYING;
                        startFadeOut(overlapPlayer, fadeMs);

                        musicPlayer = player;
                        musicState = MusicState.PLAYING;
                        startFadeIn(player, parentGen, fadeMs);
                        scheduleLoopOverlap(player, parentGen);
                    } catch (Exception e) {
                        safeReleasePlayer(player);
                        if (musicPlayer == player) {
                            musicPlayer = null;
                            musicState = MusicState.ERROR;
                        }
                    }
                }
            });

            candidate.setOnErrorListener((player, what, extra) -> {
                synchronized (AudioController.this) {
                    if (parentGen == musicGeneration) {
                        if (player == musicPlayer) {
                            if (loopScheduleRunnable != null) {
                                mainHandler.removeCallbacks(loopScheduleRunnable);
                                loopScheduleRunnable = null;
                            }
                            safeReleasePlayer(player);
                            musicPlayer = null;
                            musicState = MusicState.ERROR;
                        } else if (player == pendingMusicPlayer) {
                            safeReleasePlayer(player);
                            pendingMusicPlayer = null;
                        } else if (player == overlapPlayer) {
                            safeReleasePlayer(player);
                            overlapPlayer = null;
                            overlapState = MusicState.STOPPED;
                        } else {
                            safeReleasePlayer(player);
                        }
                    } else {
                        safeReleasePlayer(player);
                    }
                }
                return true;
            });

            candidate.setOnCompletionListener(player -> {
                synchronized (AudioController.this) {
                    if (parentGen == musicGeneration) {
                        if (player == musicPlayer) {
                            if (loopScheduleRunnable != null) {
                                mainHandler.removeCallbacks(loopScheduleRunnable);
                                loopScheduleRunnable = null;
                            }
                            safeReleasePlayer(player);
                            musicPlayer = null;
                            musicState = MusicState.IDLE;
                            updateMusic();
                        } else if (player == overlapPlayer) {
                            safeReleasePlayer(player);
                            overlapPlayer = null;
                            overlapState = MusicState.STOPPED;
                        }
                    }
                }
            });

            candidate.prepareAsync();
        } catch (Exception e) {
            if (nextMp != null) {
                safeReleasePlayer(nextMp);
                if (pendingMusicPlayer == nextMp) pendingMusicPlayer = null;
            }
        }
    }

    private void startFadeIn(MediaPlayer mp, long gen, int durationMs) {
        cancelFades(mp);
        java.util.List<Runnable> jobs = new java.util.ArrayList<>();
        fadeCallbacks.put(mp, jobs);
        final int steps = 8;
        final long interval = Math.max(20, durationMs / steps);
        for (int i = 1; i <= steps; i++) {
            final float factor = (float) i / steps;
            final boolean last = i == steps;
            Runnable job = () -> {
                synchronized (AudioController.this) {
                    if (!disposed && ownedPlayers.contains(mp) && gen == musicGeneration && !isMusicPaused && mp == musicPlayer && musicState == MusicState.PLAYING) {
                        float currentDesired = prefs.getMusicVolume();
                        float v = currentDesired * factor;
                        try { mp.setVolume(v, v); } catch (Exception ignored) {}
                    }
                    if (last && fadeCallbacks.get(mp) == jobs) fadeCallbacks.remove(mp);
                }
            };
            jobs.add(job);
            mainHandler.postDelayed(job, Math.max(20, (long) durationMs * i / steps));
        }
    }

    private void startFadeOut(MediaPlayer mp, int durationMs) {
        if (mp == null) return;
        cancelFades(mp);
        java.util.List<Runnable> jobs = new java.util.ArrayList<>();
        fadeCallbacks.put(mp, jobs);
        final int steps = 8;
        final long interval = Math.max(20, durationMs / steps);
        for (int i = 1; i <= steps; i++) {
            final float factor = 1.0f - ((float) i / steps);
            final boolean isLast = (i == steps);
            Runnable job = () -> {
                synchronized (AudioController.this) {
                  if (disposed || isMusicPaused || mp != overlapPlayer || !ownedPlayers.contains(mp)) return;
                  try {
                    float currentDesired = prefs.getMusicVolume();
                    float v = currentDesired * factor;
                    mp.setVolume(v, v);
                    if (isLast) {
                        safeReleasePlayer(mp);
                        overlapPlayer = null;
                        overlapState = MusicState.STOPPED;
                    }
                  } catch (Exception e) {
                    safeReleasePlayer(mp);
                    if (overlapPlayer == mp) {
                        overlapPlayer = null;
                        overlapState = MusicState.STOPPED;
                    }
                  }
                }
            };
            jobs.add(job);
            mainHandler.postDelayed(job, Math.max(20, (long) durationMs * i / steps));
        }
    }

    private void cancelFades(MediaPlayer player) {
        java.util.List<Runnable> jobs = fadeCallbacks.remove(player);
        if (jobs != null) for (Runnable job : jobs) mainHandler.removeCallbacks(job);
    }

    public synchronized void pauseMusic() {
        isMusicPaused = true;
        if (loopScheduleRunnable != null) {
            mainHandler.removeCallbacks(loopScheduleRunnable);
            loopScheduleRunnable = null;
        }
        safeReleasePlayer(pendingMusicPlayer);
        pendingMusicPlayer = null;
        cancelFades(musicPlayer);

        if (musicState == MusicState.PLAYING && musicPlayer != null) {
            try {
                musicPlayer.pause();
            } catch (Exception ignored) {}
            musicState = MusicState.PAUSED;
        }
        safeReleasePlayer(overlapPlayer);
        overlapPlayer = null;
        overlapState = MusicState.STOPPED;
    }

    public synchronized void resumeMusic() {
        if (disposed) return;
        isMusicPaused = false;
        if (!prefs.isAudioEnabled() || !prefs.isAmbienceEnabled()) {
            stopMusic();
            return;
        }

        if (musicState == MusicState.PAUSED && musicPlayer != null) {
            try {
                musicPlayer.start();
                musicState = MusicState.PLAYING;
                float vol = prefs.getMusicVolume();
                musicPlayer.setVolume(vol, vol);
                scheduleLoopOverlap(musicPlayer, musicGeneration);
            } catch (Exception e) {
                if (loopScheduleRunnable != null) {
                    mainHandler.removeCallbacks(loopScheduleRunnable);
                    loopScheduleRunnable = null;
                }
                safeReleasePlayer(musicPlayer);
                musicPlayer = null;
                musicState = MusicState.ERROR;
            }
        } else if (musicState == MusicState.IDLE || musicState == MusicState.STOPPED || musicState == MusicState.ERROR || musicPlayer == null) {
            updateMusic();
        }
    }

    public synchronized void stopMusic() {
        musicGeneration++;
        if (loopScheduleRunnable != null) {
            mainHandler.removeCallbacks(loopScheduleRunnable);
            loopScheduleRunnable = null;
        }
        safeReleasePlayer(pendingMusicPlayer);
        pendingMusicPlayer = null;

        safeReleasePlayer(musicPlayer);
        musicPlayer = null;
        musicState = MusicState.STOPPED;

        safeReleasePlayer(overlapPlayer);
        overlapPlayer = null;
        overlapState = MusicState.STOPPED;

        currentMusicTrackId = null;
    }

    private void safeReleasePlayer(MediaPlayer mp) {
        if (mp == null || !ownedPlayers.remove(mp)) return;
        cancelFades(mp);
        try {
            mp.setOnPreparedListener(null);
            mp.setOnCompletionListener(null);
            mp.setOnErrorListener(null);
            try {
                mp.stop();
            } catch (Exception ignored) {}
        } finally {
            try {
                mp.release();
            } catch (Exception ignored) {}
        }
    }

    // --- Підсистема реплік технік запуску (Confirm Cues) ---

    public synchronized boolean launchWithTechnique(String techniqueId, Runnable onLaunch) {
        if (disposed || isLaunching) return false; // Захист від повторного OK

        if (!prefs.isAudioEnabled()) {
            onLaunch.run();
            return true;
        }

        isLaunching = true;
        pendingLaunchRunnable = onLaunch;

        // Попередній плеєр очищається ДО запуску нового
        cleanupConfirmPlayer(true);

        final long currentLaunchGen = ++launchGeneration;

        String cueAsset;
        if ("sharingan".equals(techniqueId)) {
            cueAsset = "audio/v5-sharingan-focus.wav";
        } else {
            cueAsset = "audio/v5-" + techniqueId + "-confirm.wav";
        }

        // Failsafe таймер на 8 секунд активний на весь час підготовки та відтворення
        failsafeRunnable = () -> {
            synchronized (AudioController.this) {
                if (currentLaunchGen == launchGeneration && isLaunching) {
                    cleanupConfirmPlayer(true);
                    if (pendingLaunchRunnable != null) {
                        Runnable action = pendingLaunchRunnable;
                        pendingLaunchRunnable = null;
                        isLaunching = false;
                        action.run();
                    }
                }
            }
        };
        mainHandler.postDelayed(failsafeRunnable, 8000);

        try (AssetFileDescriptor afd = context.getAssets().openFd(cueAsset)) {
            MediaPlayer player = new MediaPlayer();
            ownedPlayers.add(player);
            confirmPlayer = player;
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());

            float vol = prefs.getVolume();
            player.setVolume(vol, vol);

            player.setOnPreparedListener(mp -> {
                synchronized (AudioController.this) {
                    if (currentLaunchGen == launchGeneration && isLaunching && mp == confirmPlayer) {
                        try {
                            mp.start();
                        } catch (Exception e) {
                            triggerFailsafeLaunch(currentLaunchGen);
                        }
                    }
                }
            });

            player.setOnCompletionListener(mp -> {
                synchronized (AudioController.this) {
                    if (currentLaunchGen == launchGeneration && isLaunching) {
                        cleanupConfirmPlayer(true);
                        if (pendingLaunchRunnable != null) {
                            Runnable action = pendingLaunchRunnable;
                            pendingLaunchRunnable = null;
                            isLaunching = false;
                            action.run();
                        }
                    }
                }
            });

            player.setOnErrorListener((mp, what, extra) -> {
                synchronized (AudioController.this) {
                    if (currentLaunchGen == launchGeneration && isLaunching) {
                        triggerFailsafeLaunch(currentLaunchGen);
                    }
                }
                return true;
            });

            player.prepareAsync();
        } catch (Exception e) {
            triggerFailsafeLaunch(currentLaunchGen);
        }

        return true;
    }

    private synchronized void triggerFailsafeLaunch(long currentLaunchGen) {
        if (currentLaunchGen == launchGeneration && isLaunching) {
            cleanupConfirmPlayer(true);
            if (pendingLaunchRunnable != null) {
                Runnable action = pendingLaunchRunnable;
                pendingLaunchRunnable = null;
                isLaunching = false;
                action.run();
            }
        }
    }

    public synchronized boolean cancelPendingLaunch() {
        if (!isLaunching) return false;
        launchGeneration++;
        isLaunching = false;
        pendingLaunchRunnable = null;
        cleanupConfirmPlayer(true);
        return true;
    }

    public synchronized boolean isLaunching() {
        return isLaunching;
    }

    private void cleanupConfirmPlayer(boolean cancelFailsafe) {
        if (cancelFailsafe && failsafeRunnable != null) {
            mainHandler.removeCallbacks(failsafeRunnable);
            failsafeRunnable = null;
        }
        if (confirmPlayer != null) {
            safeReleasePlayer(confirmPlayer);
            confirmPlayer = null;
        }
    }

    public synchronized void release() {
        disposed = true;
        stopMusic();
        cancelPendingLaunch();
        if (soundPool != null) {
            try {
                soundPool.release();
            } catch (Exception ignored) {}
            soundPool = null;
        }
    }
}
