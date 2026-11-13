package ua.konoha.tv.nativepreview.panels;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import ua.konoha.tv.nativepreview.audio.AudioController;
import ua.konoha.tv.nativepreview.model.CatalogData;
import ua.konoha.tv.nativepreview.model.HighlightSound;
import ua.konoha.tv.nativepreview.model.MusicTrack;
import ua.konoha.tv.nativepreview.model.Technique;
import ua.konoha.tv.nativepreview.prefs.PreferencesManager;
import ua.konoha.tv.nativepreview.theme.KonohaTheme;
import ua.konoha.tv.nativepreview.views.GlyphIconView;
import ua.konoha.tv.nativepreview.views.KonohaSeekBar;
import ua.konoha.tv.nativepreview.views.KonohaSwitch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Settings panel (.scroll-panel) reproducing Chromium's "Шлях ніндзя".
 * Right side sheet (680px), custom orange switches, sliders, and exact rows.
 */
public class SettingsPanel extends FrameLayout {
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
    private final SideSheetContainer sheetContainer;
    private final ScrollView scrollView;
    private final LinearLayout content;

    private TextView techniqueValueText;
    private KonohaSwitch motionSwitch;
    private KonohaSwitch audioSwitch;
    private KonohaSwitch musicSwitch;
    private KonohaSeekBar fxVolumeBar;
    private TextView fxVolumeText;
    private KonohaSeekBar musicVolumeBar;
    private TextView musicVolumeText;

    private OnSettingsActionListener listener;

    public SettingsPanel(Context context, PreferencesManager prefs, AudioController audioController) {
        super(context);
        this.prefs = prefs;
        this.audioController = audioController;

        sheetContainer = new SideSheetContainer(context, false, "Архів Конохи", "Шлях ніндзя");
        sheetContainer.setOnCloseListener(() -> {
            if (listener != null) listener.onCloseRequested();
        });

        scrollView = new ScrollView(context);
        scrollView.setVerticalScrollBarEnabled(false);

        content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);

        buildSettingsRows(context);

        scrollView.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        sheetContainer.setBodyView(scrollView);
        addView(sheetContainer, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    public void setOnSettingsActionListener(OnSettingsActionListener listener) {
        this.listener = listener;
    }

    private void buildSettingsRows(Context context) {
        // 1. Technique link
        View techRow = createNavigationLink("Техніка навігації", getTechniqueName(prefs.getTechnique()), v -> {
            if (listener != null) listener.onTechniquesRequested();
        });
        techniqueValueText = techRow.findViewById(android.R.id.text2);
        content.addView(techRow);

        // 2. Sound option (Spinner / selector)
        Spinner soundSpinner = createStyledSpinner(context);
        setupHighlightSoundSpinner(soundSpinner);
        content.addView(createSpinnerRow("Звук виділення", "Один спільний звук фокусу для всіх застосунків.", soundSpinner));

        // 3. Motion Switch
        motionSwitch = new KonohaSwitch(context);
        content.addView(createSwitchRow("Живий світ", "Листя, паралакс, сліди чакри та рухомі печатки.",
                motionSwitch, prefs.isMotionEnabled(), (sw, checked) -> {
                    prefs.setMotionEnabled(checked);
                    audioController.playUiSound();
                }));

        // 4. Audio Effects Switch
        audioSwitch = new KonohaSwitch(context);
        content.addView(createSwitchRow("Звуки технік", "Чакра, блискавка та печатки з аніме.",
                audioSwitch, prefs.isAudioEnabled(), (sw, checked) -> {
                    prefs.setAudioEnabled(checked);
                    if (!checked) audioController.stopTechniquePreview();
                    audioController.updateMusic();
                    audioController.playUiSound();
                }));

        // 5. Music Switch
        musicSwitch = new KonohaSwitch(context);
        content.addView(createSwitchRow("Музика Naruto", "Офіційні прев’ю OST замість синтезованого фону.",
                musicSwitch, prefs.isAmbienceEnabled(), (sw, checked) -> {
                    prefs.setAmbienceEnabled(checked);
                    audioController.updateMusic();
                }));

        // 6. FX Volume
        int currentFx = (int) (prefs.getVolume() * 100);
        View fxVolRow = createSliderRow("Гучність ефектів", "Окремий баланс для цього лаунчера.", currentFx, (sb, val) -> {
            prefs.setVolume(val / 100f);
            if (fxVolumeText != null) fxVolumeText.setText(val + "%");
        });
        fxVolumeText = fxVolRow.findViewById(android.R.id.text2);
        content.addView(fxVolRow);

        // 7. Music Track selector
        Spinner musicSpinner = createStyledSpinner(context);
        setupMusicSpinner(musicSpinner);
        content.addView(createSpinnerRow("Музика", "Оригінальний Naruto OST · MUSASHI PROJECT", musicSpinner));

        // 8. Music Volume
        int currentMusicVol = (int) (prefs.getMusicVolume() * 100);
        View musicVolRow = createSliderRow("Гучність музики", "Окремо від звуків навігації.", currentMusicVol, (sb, val) -> {
            prefs.setMusicVolume(val / 100f);
            if (musicVolumeText != null) musicVolumeText.setText(val + "%");
            audioController.updateMusic();
        });
        musicVolumeText = musicVolRow.findViewById(android.R.id.text2);
        content.addView(musicVolRow);

        // 9. Audio preview buttons
        content.addView(createActionButton("Прослухати звук виділення", v -> {
            audioSwitch.setChecked(true);
            audioController.stopTechniquePreview();
            audioController.playFocusSound();
        }));
        content.addView(createActionButton("Використати: " + getTechniqueName(prefs.getTechnique()), v -> {
            audioSwitch.setChecked(true);
            audioController.playTechniquePreview(prefs.getTechnique());
        }));

        // 10. Rotation & Idle Spinners
        Spinner rotSpinner = createStyledSpinner(context);
        setupRotationSpinner(rotSpinner);
        content.addView(createSpinnerRow("Зміна сцен", "Автоматична ротація 18 шпалер.", rotSpinner));

        Spinner idleSpinner = createStyledSpinner(context);
        setupIdleSpinner(idleSpinner);
        content.addView(createSpinnerRow("Режим спокою", "Автоматичний перехід у тиху мить.", idleSpinner));

        // 11. Links & actions
        content.addView(createNavigationLink("Увійти в режим спокою", "Тиха мить", v -> {
            if (listener != null) listener.onAmbientRequested();
        }));
        content.addView(createNavigationLink("Налаштування Android TV", "Система", v -> {
            if (listener != null) listener.onSystemSettingsRequested("system");
        }));
        content.addView(createNavigationLink("Налаштування мережі та Wi-Fi", "Wi-Fi", v -> {
            if (listener != null) listener.onSystemSettingsRequested("wifi");
        }));
        content.addView(createNavigationLink("Оновити архів застосунків", "Пересканувати", v -> {
            if (listener != null) listener.onRefreshAppsRequested();
        }));
        content.addView(createNavigationLink("Про Коноху та ресурси", "Довідка", v -> {
            if (listener != null) listener.onAboutRequested();
        }));

        // Footer note
        TextView note = new TextView(context);
        note.setText("Усі параметри зберігаються автоматично на пристрої. Керування оптимізовано під стандартний пульт D-pad.");
        note.setTextColor(Color.parseColor("#8aa2ad"));
        note.setTextSize(11);
        note.setTypeface(KonohaTheme.getNunito(context));
        note.setPadding(0, KonohaTheme.dp(context, 20), 0, KonohaTheme.dp(context, 32));
        content.addView(note);
    }

    private View createSettingLabel(String title, String subtitle) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, KonohaTheme.dp(getContext(), 12), 0, KonohaTheme.dp(getContext(), 4));

        TextView tvTitle = new TextView(getContext());
        tvTitle.setText(title);
        tvTitle.setTextColor(KonohaTheme.COLOR_PAPER);
        tvTitle.setTextSize(14);
        tvTitle.setTypeface(KonohaTheme.getNunitoBold(getContext()));
        row.addView(tvTitle);

        TextView tvSub = new TextView(getContext());
        tvSub.setText(subtitle);
        tvSub.setTextColor(Color.parseColor("#afc1b7"));
        tvSub.setTextSize(11);
        tvSub.setTypeface(KonohaTheme.getNunito(getContext()));
        row.addView(tvSub);

        return row;
    }

    private View createSwitchRow(String title, String subtitle, KonohaSwitch sw, boolean initial,
            KonohaSwitch.OnCheckedChangeListener changeListener) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, KonohaTheme.dp(getContext(), 10), 0, KonohaTheme.dp(getContext(), 10));

        LinearLayout textCol = new LinearLayout(getContext());
        textCol.setOrientation(LinearLayout.VERTICAL);

        TextView tvTitle = new TextView(getContext());
        tvTitle.setText(title);
        tvTitle.setTextColor(KonohaTheme.COLOR_PAPER);
        tvTitle.setTextSize(14);
        tvTitle.setTypeface(KonohaTheme.getNunitoBold(getContext()));
        textCol.addView(tvTitle);

        TextView tvSub = new TextView(getContext());
        tvSub.setText(subtitle);
        tvSub.setTextColor(Color.parseColor("#afc1b7"));
        tvSub.setTextSize(11);
        tvSub.setTypeface(KonohaTheme.getNunito(getContext()));
        textCol.addView(tvSub);

        row.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        sw.setChecked(initial);
        sw.setOnCheckedChangeListener(changeListener);
        row.addView(sw);
        highlightControlRow(row, sw);

        return row;
    }

    private View createSliderRow(String title, String subtitle, int initial, KonohaSeekBar.OnProgressChangeListener changeListener) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, KonohaTheme.dp(getContext(), 10), 0, KonohaTheme.dp(getContext(), 10));

        LinearLayout textCol = new LinearLayout(getContext());
        textCol.setOrientation(LinearLayout.VERTICAL);

        TextView tvTitle = new TextView(getContext());
        tvTitle.setText(title);
        tvTitle.setTextColor(KonohaTheme.COLOR_PAPER);
        tvTitle.setTextSize(14);
        tvTitle.setTypeface(KonohaTheme.getNunitoBold(getContext()));
        textCol.addView(tvTitle);

        TextView tvSub = new TextView(getContext());
        tvSub.setText(subtitle);
        tvSub.setTextColor(Color.parseColor("#afc1b7"));
        tvSub.setTextSize(11);
        tvSub.setTypeface(KonohaTheme.getNunito(getContext()));
        textCol.addView(tvSub);

        row.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        LinearLayout volumeControl = new LinearLayout(getContext());
        volumeControl.setOrientation(LinearLayout.HORIZONTAL);
        volumeControl.setGravity(Gravity.CENTER_VERTICAL);

        KonohaSeekBar sb = new KonohaSeekBar(getContext());
        sb.setMax(100);
        sb.setProgress(initial);
        sb.setOnProgressChangeListener(changeListener);
        volumeControl.addView(sb, new LinearLayout.LayoutParams(KonohaTheme.dp(getContext(), 125), ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView valView = new TextView(getContext());
        valView.setId(android.R.id.text2);
        valView.setText(initial + "%");
        valView.setTextColor(KonohaTheme.COLOR_ORANGE);
        valView.setTextSize(12);
        valView.setTypeface(KonohaTheme.getNunitoBold(getContext()));
        valView.setPadding(KonohaTheme.dp(getContext(), 9), 0, 0, 0);
        valView.setMinWidth(KonohaTheme.dp(getContext(), 36));
        volumeControl.addView(valView);

        row.addView(volumeControl);
        highlightControlRow(row, sb);
        return row;
    }

    private View createNavigationLink(String title, String val, View.OnClickListener clickListener) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setFocusable(true);
        row.setFocusableInTouchMode(true);
        row.setClickable(true);
        row.setPadding(KonohaTheme.dp(getContext(), 10), KonohaTheme.dp(getContext(), 12),
                KonohaTheme.dp(getContext(), 10), KonohaTheme.dp(getContext(), 12));

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor("#122b36"));
        gd.setCornerRadius(KonohaTheme.dp(getContext(), 4));
        gd.setStroke(KonohaTheme.dp(getContext(), 1), Color.parseColor("#33dbbd7d"));
        row.setBackground(gd);

        TextView tv = new TextView(getContext());
        tv.setText(title);
        tv.setTextColor(KonohaTheme.COLOR_PAPER);
        tv.setTextSize(14);
        tv.setTypeface(KonohaTheme.getNunitoBold(getContext()));
        row.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView tvVal = new TextView(getContext());
        tvVal.setId(android.R.id.text2);
        tvVal.setText(val);
        tvVal.setTextColor(KonohaTheme.COLOR_GOLD);
        tvVal.setTextSize(12);
        tvVal.setTypeface(KonohaTheme.getNunito(getContext()));
        row.addView(tvVal);

        row.setOnClickListener(clickListener);
        row.setOnFocusChangeListener((v, hasFocus) -> {
            GradientDrawable d = new GradientDrawable();
            d.setColor(hasFocus ? Color.parseColor("#2a424e") : Color.parseColor("#122b36"));
            d.setCornerRadius(KonohaTheme.dp(getContext(), 4));
            d.setStroke(KonohaTheme.dp(getContext(), hasFocus ? 2 : 1),
                    hasFocus ? KonohaTheme.COLOR_ORANGE : Color.parseColor("#33dbbd7d"));
            v.setBackground(d);
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, KonohaTheme.dp(getContext(), 6), 0, KonohaTheme.dp(getContext(), 6));
        row.setLayoutParams(lp);

        return row;
    }

    private Button createActionButton(String text, View.OnClickListener clickListener) {
        Button btn = new Button(getContext());
        btn.setText(text);
        btn.setTextColor(KonohaTheme.COLOR_PAPER);
        btn.setTextSize(13);
        btn.setTypeface(KonohaTheme.getNunitoBold(getContext()));
        btn.setFocusable(true);
        btn.setFocusableInTouchMode(true);

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor("#173b47"));
        gd.setCornerRadius(KonohaTheme.dp(getContext(), 4));
        gd.setStroke(KonohaTheme.dp(getContext(), 1), Color.parseColor("#44dbbd7d"));
        btn.setBackground(gd);

        btn.setOnClickListener(clickListener);
        btn.setOnFocusChangeListener((v, hasFocus) -> {
            GradientDrawable d = new GradientDrawable();
            d.setColor(hasFocus ? Color.parseColor("#2a424e") : Color.parseColor("#173b47"));
            d.setCornerRadius(KonohaTheme.dp(getContext(), 4));
            d.setStroke(KonohaTheme.dp(getContext(), hasFocus ? 2 : 1),
                    hasFocus ? KonohaTheme.COLOR_ORANGE : Color.parseColor("#44dbbd7d"));
            v.setBackground(d);
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, KonohaTheme.dp(getContext(), 42)
        );
        lp.setMargins(0, KonohaTheme.dp(getContext(), 6), 0, KonohaTheme.dp(getContext(), 6));
        btn.setLayoutParams(lp);

        return btn;
    }

    private View createSpinnerRow(String title, String subtitle, Spinner spinner) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, KonohaTheme.dp(getContext(), 8), 0, KonohaTheme.dp(getContext(), 8));

        LinearLayout textCol = new LinearLayout(getContext());
        textCol.setOrientation(LinearLayout.VERTICAL);

        TextView tvTitle = new TextView(getContext());
        tvTitle.setText(title);
        tvTitle.setTextColor(KonohaTheme.COLOR_PAPER);
        tvTitle.setTextSize(14);
        tvTitle.setTypeface(KonohaTheme.getNunitoBold(getContext()));
        textCol.addView(tvTitle);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView tvSub = new TextView(getContext());
            tvSub.setText(subtitle);
            tvSub.setTextColor(Color.parseColor("#afc1b7"));
            tvSub.setTextSize(11);
            tvSub.setTypeface(KonohaTheme.getNunito(getContext()));
            textCol.addView(tvSub);
        }

        row.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        row.addView(spinner);
        highlightControlRow(row, spinner);

        return row;
    }

    private Spinner createStyledSpinner(Context context) {
        Spinner spinner = new Spinner(context, Spinner.MODE_DROPDOWN);
        spinner.setFocusable(true);
        spinner.setFocusableInTouchMode(true);
        spinner.setBackground(focusBackground(Color.parseColor("#082330"), Color.parseColor("#889e875b")));
        spinner.setPopupBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.parseColor("#082330")));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                KonohaTheme.dp(context, 190), KonohaTheme.dp(context, 38)
        );
        spinner.setLayoutParams(lp);
        return spinner;
    }

    private ArrayAdapter<String> createThemedAdapter(Context context, List<String> items) {
        return new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                if (v instanceof TextView) {
                    TextView tv = (TextView) v;
                    tv.setTextColor(KonohaTheme.COLOR_PAPER);
                    tv.setTextSize(12);
                    tv.setTypeface(KonohaTheme.getNunito(getContext()));
                    tv.setPadding(KonohaTheme.dp(getContext(), 8), 0, KonohaTheme.dp(getContext(), 8), 0);
                    tv.setSingleLine(true);
                    tv.setEllipsize(TextUtils.TruncateAt.END);
                }
                return v;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                if (v instanceof TextView) {
                    TextView tv = (TextView) v;
                    tv.setTextColor(KonohaTheme.COLOR_PAPER);
                    tv.setTextSize(12);
                    tv.setTypeface(KonohaTheme.getNunito(getContext()));
                    // ListView activates the committed value separately from its D-pad selection.
                    tv.setBackground(stateBackground(Color.parseColor("#082330"), Color.TRANSPARENT,
                            new int[]{android.R.attr.state_focused, android.R.attr.state_selected,
                                    android.R.attr.state_pressed}));
                    tv.setSingleLine(false);
                    tv.setMinHeight(KonohaTheme.dp(getContext(), 44));
                    tv.setPadding(KonohaTheme.dp(getContext(), 12), KonohaTheme.dp(getContext(), 10),
                            KonohaTheme.dp(getContext(), 12), KonohaTheme.dp(getContext(), 10));
                }
                return v;
            }
        };
    }

    private void highlightControlRow(ViewGroup row, View control) {
        int inset = KonohaTheme.dp(getContext(), 10);
        row.setPadding(inset, row.getPaddingTop(), inset, row.getPaddingBottom());
        row.setBackground(focusBackground(Color.TRANSPARENT, Color.TRANSPARENT));
        control.setOnFocusChangeListener((view, focused) -> {
            row.setActivated(focused);
            view.invalidate();
            if (focused && row.getWidth() > 0 && row.getHeight() > 0) {
                row.requestRectangleOnScreen(new android.graphics.Rect(0, 0, row.getWidth(), row.getHeight()), true);
            }
        });
    }

    private StateListDrawable focusBackground(int normalColor, int normalBorderColor) {
        return stateBackground(normalColor, normalBorderColor,
                new int[]{android.R.attr.state_focused, android.R.attr.state_selected,
                        android.R.attr.state_activated, android.R.attr.state_pressed});
    }

    private StateListDrawable stateBackground(int normalColor, int normalBorderColor, int[] highlightedStates) {
        StateListDrawable states = new StateListDrawable();
        for (int state : highlightedStates) {
            states.addState(new int[]{state}, controlBackground(Color.parseColor("#2a424e"),
                    KonohaTheme.COLOR_ORANGE, 2));
        }
        states.addState(new int[]{}, controlBackground(normalColor, normalBorderColor, 1));
        return states;
    }

    private GradientDrawable controlBackground(int fill, int stroke, int widthDp) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(fill);
        background.setCornerRadius(KonohaTheme.dp(getContext(), 4));
        background.setStroke(KonohaTheme.dp(getContext(), widthDp), stroke);
        return background;
    }

    private void setupHighlightSoundSpinner(Spinner spinner) {
        List<String> names = new ArrayList<>();
        int selectedIndex = 0;
        String cur = prefs.getHighlightSound();
        for (int i = 0; i < CatalogData.HIGHLIGHT_SOUNDS.size(); i++) {
            HighlightSound hs = CatalogData.HIGHLIGHT_SOUNDS.get(i);
            names.add(hs.getName());
            if (hs.getId().equals(cur)) selectedIndex = i;
        }

        spinner.setAdapter(createThemedAdapter(getContext(), names));
        spinner.setSelection(selectedIndex);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                prefs.setHighlightSound(CatalogData.HIGHLIGHT_SOUNDS.get(pos).getId());
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void setupMusicSpinner(Spinner spinner) {
        List<String> names = new ArrayList<>();
        int selectedIndex = 0;
        String cur = prefs.getMusicTrack();
        for (int i = 0; i < CatalogData.MUSIC_TRACKS.size(); i++) {
            MusicTrack mt = CatalogData.MUSIC_TRACKS.get(i);
            names.add(mt.getName());
            if (mt.getId().equals(cur)) selectedIndex = i;
        }

        spinner.setAdapter(createThemedAdapter(getContext(), names));
        spinner.setSelection(selectedIndex);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                prefs.setMusicTrack(CatalogData.MUSIC_TRACKS.get(pos).getId());
                audioController.updateMusic();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void setupRotationSpinner(Spinner spinner) {
        String[] options = {"Вручну", "Кожні 60 секунд", "Кожні 3 хвилини", "Кожні 10 хвилин"};
        int[] seconds = {0, 60, 180, 600};
        int selectedIndex = 0;
        int cur = prefs.getRotationSeconds();
        for (int i = 0; i < seconds.length; i++) {
            if (seconds[i] == cur) selectedIndex = i;
        }

        spinner.setAdapter(createThemedAdapter(getContext(), Arrays.asList(options)));
        spinner.setSelection(selectedIndex);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                prefs.setRotationSeconds(seconds[pos]);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void setupIdleSpinner(Spinner spinner) {
        String[] options = {"Вимкнено", "Через 5 хвилин", "Через 15 хвилин"};
        int[] seconds = {0, 300, 900};
        int selectedIndex = 0;
        int cur = prefs.getIdleSeconds();
        for (int i = 0; i < seconds.length; i++) {
            if (seconds[i] == cur) selectedIndex = i;
        }

        spinner.setAdapter(createThemedAdapter(getContext(), Arrays.asList(options)));
        spinner.setSelection(selectedIndex);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                prefs.setIdleSeconds(seconds[pos]);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private String getTechniqueName(String id) {
        for (Technique t : CatalogData.TECHNIQUES) {
            if (t.getId().equals(id)) return t.getName();
        }
        return "Расенґан";
    }

    public void updateState() {
        if (techniqueValueText != null) {
            techniqueValueText.setText(getTechniqueName(prefs.getTechnique()));
        }
        if (motionSwitch != null) {
            motionSwitch.setChecked(prefs.isMotionEnabled());
        }
        if (audioSwitch != null) {
            audioSwitch.setChecked(prefs.isAudioEnabled());
        }
        if (musicSwitch != null) {
            musicSwitch.setChecked(prefs.isAmbienceEnabled());
        }
        if (fxVolumeBar != null) {
            fxVolumeBar.setProgress((int) (prefs.getVolume() * 100));
        }
        if (fxVolumeText != null) {
            fxVolumeText.setText(((int) (prefs.getVolume() * 100)) + "%");
        }
        if (musicVolumeText != null) {
            musicVolumeText.setText(((int) (prefs.getMusicVolume() * 100)) + "%");
        }
    }

    public void updateTechnique(String name) {
        if (techniqueValueText != null) techniqueValueText.setText(name);
    }
}
