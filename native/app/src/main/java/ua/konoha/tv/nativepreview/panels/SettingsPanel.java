package ua.konoha.tv.nativepreview.panels;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import ua.konoha.tv.nativepreview.audio.AudioController;
import ua.konoha.tv.nativepreview.model.CatalogData;
import ua.konoha.tv.nativepreview.model.HighlightSound;
import ua.konoha.tv.nativepreview.model.MusicTrack;
import ua.konoha.tv.nativepreview.model.Technique;
import ua.konoha.tv.nativepreview.prefs.PreferencesManager;

public class SettingsPanel extends ScrollView {
    public interface OnSettingsActionListener {
        void onTechniquesRequested();
        void onAmbientRequested();
        void onSystemSettingsRequested(String target);
        void onRefreshAppsRequested();
        void onAboutRequested();
        void onCloseRequested();
    }

    private final PreferencesManager prefs;
    private final AudioController audioController;
    private final LinearLayout content;

    private Button techniqueBtn;
    private Spinner highlightSoundSpinner;
    private Switch motionSwitch;
    private Switch audioSwitch;
    private Switch musicSwitch;
    private SeekBar fxVolumeBar;
    private TextView fxVolumeText;
    private Spinner musicSpinner;
    private SeekBar musicVolumeBar;
    private TextView musicVolumeText;
    private Spinner rotationSpinner;
    private Spinner idleSpinner;
    private Button soundReadyBtn;
    private Button soundConfirmBtn;

    private OnSettingsActionListener listener;

    public SettingsPanel(Context context, PreferencesManager prefs, AudioController audioController) {
        super(context);
        this.prefs = prefs;
        this.audioController = audioController;

        setBackgroundColor(Color.parseColor("#f00d2936"));
        setPadding(dp(24), dp(20), dp(24), dp(20));

        content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);

        // Header
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(context);
        title.setText("Шлях ніндзя");
        title.setTextColor(Color.parseColor("#f2c576"));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button closeBtn = new Button(context);
        closeBtn.setText("✕ Закрити");
        closeBtn.setTextColor(Color.WHITE);
        closeBtn.setBackgroundColor(Color.parseColor("#254756"));
        closeBtn.setOnClickListener(v -> {
            if (listener != null) listener.onCloseRequested();
        });
        header.addView(closeBtn);
        content.addView(header);

        // Кнопка вибору техніки
        techniqueBtn = createActionButton("Техніка навігації", v -> {
            if (listener != null) listener.onTechniquesRequested();
        });
        content.addView(techniqueBtn);

        // Звук виділення
        content.addView(createSettingLabel("Звук виділення", "Один спільний звук фокусу для всіх застосунків."));
        highlightSoundSpinner = new Spinner(context);
        setupHighlightSoundSpinner();
        content.addView(highlightSoundSpinner);

        // Тогли
        motionSwitch = createSwitchRow("Живий світ", "Листя, паралакс, сліди чакри та рух.", prefs.isMotionEnabled(), (btn, isChecked) -> {
            prefs.setMotionEnabled(isChecked);
            audioController.playUiSound();
        });
        content.addView(motionSwitch);

        audioSwitch = createSwitchRow("Звуки технік", "Чакра, блискавка та печатки з аніме.", prefs.isAudioEnabled(), (btn, isChecked) -> {
            prefs.setAudioEnabled(isChecked);
            audioController.updateMusic();
            audioController.playUiSound();
        });
        content.addView(audioSwitch);

        musicSwitch = createSwitchRow("Музика Naruto", "Офіційні прев’ю OST замість тиші.", prefs.isAmbienceEnabled(), (btn, isChecked) -> {
            prefs.setAmbienceEnabled(isChecked);
            audioController.updateMusic();
            audioController.playUiSound();
        });
        content.addView(musicSwitch);

        // Слайдер гучності ефектів
        content.addView(createSettingLabel("Гучність ефектів", "Окремий баланс для звуків технік."));
        LinearLayout fxVolLayout = new LinearLayout(context);
        fxVolLayout.setOrientation(LinearLayout.HORIZONTAL);
        fxVolLayout.setGravity(Gravity.CENTER_VERTICAL);

        fxVolumeBar = new SeekBar(context);
        fxVolumeBar.setMax(100);
        fxVolumeBar.setProgress((int) (prefs.getVolume() * 100));
        fxVolLayout.addView(fxVolumeBar, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        fxVolumeText = new TextView(context);
        fxVolumeText.setText(fxVolumeBar.getProgress() + "%");
        fxVolumeText.setTextColor(Color.WHITE);
        fxVolumeText.setPadding(dp(8), 0, 0, 0);
        fxVolLayout.addView(fxVolumeText);
        content.addView(fxVolLayout);

        fxVolumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    prefs.setVolume(progress / 100f);
                    fxVolumeText.setText(progress + "%");
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Селектор музичного треку
        content.addView(createSettingLabel("Музика", "Оригінальний Naruto OST · MUSASHI PROJECT"));
        musicSpinner = new Spinner(context);
        setupMusicSpinner();
        content.addView(musicSpinner);

        // Слайдер гучності музики
        content.addView(createSettingLabel("Гучність музики", "Окремо від звуків навігації."));
        LinearLayout musicVolLayout = new LinearLayout(context);
        musicVolLayout.setOrientation(LinearLayout.HORIZONTAL);
        musicVolLayout.setGravity(Gravity.CENTER_VERTICAL);

        musicVolumeBar = new SeekBar(context);
        musicVolumeBar.setMax(100);
        musicVolumeBar.setProgress((int) (prefs.getMusicVolume() * 100));
        musicVolLayout.addView(musicVolumeBar, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        musicVolumeText = new TextView(context);
        musicVolumeText.setText(musicVolumeBar.getProgress() + "%");
        musicVolumeText.setTextColor(Color.WHITE);
        musicVolumeText.setPadding(dp(8), 0, 0, 0);
        musicVolLayout.addView(musicVolumeText);
        content.addView(musicVolLayout);

        musicVolumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    prefs.setMusicVolume(progress / 100f);
                    musicVolumeText.setText(progress + "%");
                    audioController.updateMusic();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Кнопки тесту аудіо
        soundReadyBtn = createActionButton("Прослухати звук виділення", v -> {
            prefs.setAudioEnabled(true);
            audioSwitch.setChecked(true);
            audioController.playFocusSound();
        });
        content.addView(soundReadyBtn);

        soundConfirmBtn = createActionButton("Використати техніку", v -> {
            prefs.setAudioEnabled(true);
            audioSwitch.setChecked(true);
            audioController.launchWithTechnique(prefs.getTechnique(), () -> {});
        });
        content.addView(soundConfirmBtn);

        // Селектор зміни сцен
        content.addView(createSettingLabel("Зміна сцен", "Автоматична ротація 18 шпалер."));
        rotationSpinner = new Spinner(context);
        setupRotationSpinner();
        content.addView(rotationSpinner);

        // Селектор тихої миті
        content.addView(createSettingLabel("Тиха мить", "Перехід до режиму спокою без активності."));
        idleSpinner = new Spinner(context);
        setupIdleSpinner();
        content.addView(idleSpinner);

        // Дії
        content.addView(createActionButton("Увійти в режим спокою", v -> {
            if (listener != null) listener.onAmbientRequested();
        }));
        content.addView(createActionButton("Налаштування Android", v -> {
            if (listener != null) listener.onSystemSettingsRequested("system");
        }));
        content.addView(createActionButton("Мережа й Wi-Fi", v -> {
            if (listener != null) listener.onSystemSettingsRequested("wifi");
        }));
        content.addView(createActionButton("Оновити архів застосунків", v -> {
            if (listener != null) listener.onRefreshAppsRequested();
        }));
        content.addView(createActionButton("Про Коноху та ресурси", v -> {
            if (listener != null) listener.onAboutRequested();
        }));

        addView(content);
    }

    public void setOnSettingsActionListener(OnSettingsActionListener listener) {
        this.listener = listener;
    }

    public void updateState() {
        String techId = prefs.getTechnique();
        Technique tech = CatalogData.TECHNIQUES.stream()
                .filter(t -> t.getId().equals(techId)).findFirst().orElse(CatalogData.TECHNIQUES.get(0));
        techniqueBtn.setText("Техніка навігації: " + tech.getName());
        soundConfirmBtn.setText("Використати: " + tech.getName());
        motionSwitch.setChecked(prefs.isMotionEnabled());
        audioSwitch.setChecked(prefs.isAudioEnabled());
        musicSwitch.setChecked(prefs.isAmbienceEnabled());
    }

    private void setupHighlightSoundSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item);
        for (HighlightSound hs : CatalogData.HIGHLIGHT_SOUNDS) {
            adapter.add(hs.getName());
        }
        highlightSoundSpinner.setAdapter(adapter);

        String currentSound = prefs.getHighlightSound();
        for (int i = 0; i < CatalogData.HIGHLIGHT_SOUNDS.size(); i++) {
            if (CatalogData.HIGHLIGHT_SOUNDS.get(i).getId().equals(currentSound)) {
                highlightSoundSpinner.setSelection(i);
                break;
            }
        }

        highlightSoundSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String soundId = CatalogData.HIGHLIGHT_SOUNDS.get(position).getId();
                if (!soundId.equals(prefs.getHighlightSound())) {
                    prefs.setHighlightSound(soundId);
                    audioController.playFocusSound();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupMusicSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item);
        for (MusicTrack mt : CatalogData.MUSIC_TRACKS) {
            adapter.add(mt.getName());
        }
        musicSpinner.setAdapter(adapter);

        String currentMusic = prefs.getMusicTrack();
        for (int i = 0; i < CatalogData.MUSIC_TRACKS.size(); i++) {
            if (CatalogData.MUSIC_TRACKS.get(i).getId().equals(currentMusic)) {
                musicSpinner.setSelection(i);
                break;
            }
        }

        musicSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String trackId = CatalogData.MUSIC_TRACKS.get(position).getId();
                if (!trackId.equals(prefs.getMusicTrack())) {
                    prefs.setMusicTrack(trackId);
                    audioController.updateMusic();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupRotationSpinner() {
        String[] labels = new String[]{"Вручну", "Щохвилини", "Кожні 3 хв", "Кожні 10 хв"};
        int[] values = new int[]{0, 60, 180, 600};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, labels);
        rotationSpinner.setAdapter(adapter);

        int current = prefs.getRotationSeconds();
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                rotationSpinner.setSelection(i);
                break;
            }
        }

        rotationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.setRotationSeconds(values[position]);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupIdleSpinner() {
        String[] labels = new String[]{"Вимкнено", "Через 5 хв", "Через 15 хв"};
        int[] values = new int[]{0, 300, 900};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, labels);
        idleSpinner.setAdapter(adapter);

        int current = prefs.getIdleSeconds();
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                idleSpinner.setSelection(i);
                break;
            }
        }

        idleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.setIdleSeconds(values[position]);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private TextView createSettingLabel(String title, String desc) {
        TextView tv = new TextView(getContext());
        tv.setText(title + " — " + desc);
        tv.setTextColor(Color.parseColor("#afc1b7"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setPadding(0, dp(12), 0, dp(4));
        return tv;
    }

    private Switch createSwitchRow(String title, String desc, boolean initial, CompoundButton.OnCheckedChangeListener listener) {
        Switch sw = new Switch(getContext());
        sw.setText(title + "\n" + desc);
        sw.setTextColor(Color.WHITE);
        sw.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        sw.setChecked(initial);
        sw.setPadding(0, dp(8), 0, dp(8));
        sw.setOnCheckedChangeListener(listener);
        return sw;
    }

    private Button createActionButton(String text, OnClickListener onClick) {
        Button btn = new Button(getContext());
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundColor(Color.parseColor("#153745"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, dp(6), 0, dp(6));
        btn.setLayoutParams(lp);
        btn.setOnClickListener(onClick);
        return btn;
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()
        );
    }
}
