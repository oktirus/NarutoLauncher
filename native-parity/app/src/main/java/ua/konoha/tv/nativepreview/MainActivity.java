package ua.konoha.tv.nativepreview;

import ua.konoha.tv.nativepreview.updates.UpdateController;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;

import ua.konoha.tv.nativepreview.apps.AppManager;
import ua.konoha.tv.nativepreview.apps.HomeRedirectService;
import ua.konoha.tv.nativepreview.apps.HomeScreenSetup;
import ua.konoha.tv.nativepreview.audio.AudioController;
import ua.konoha.tv.nativepreview.model.AppItem;
import ua.konoha.tv.nativepreview.model.CatalogData;
import ua.konoha.tv.nativepreview.model.Quote;
import ua.konoha.tv.nativepreview.model.Scene;
import ua.konoha.tv.nativepreview.model.Technique;
import ua.konoha.tv.nativepreview.panels.*;
import ua.konoha.tv.nativepreview.prefs.PreferencesManager;
import ua.konoha.tv.nativepreview.theme.KonohaTheme;
import ua.konoha.tv.nativepreview.views.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private PreferencesManager prefs;
    private AppManager appManager;
    private HomeScreenSetup homeScreenSetup;
    private AudioController audioController;
    private TechniqueRenderer techniqueRenderer;

    private FrameLayout rootLayout;
    private LinearLayout mainShell;
    private VillageBackgroundView backgroundView;
    private ChakraFocusView chakraFocusView;
    private DockView dockView;
    private ToastOverlay toastOverlay;
    private AmbientOverlay ambientOverlay;

    // UI elements
    private TextView timeView;
    private TextView dateView;
    private TextView updateNotice;
    private LauncherResourceMonitor resources;
    private HeroTitleView heroTitleView;
    private TextView heroDescView;
    private TextView quoteAuthorView;
    private OutlinedGlyphButton sceneChoiceBtn;
    private TechniqueShortcutButton techniqueBtn;
    private FrameLayout brandBtn;
    private OutlinedGlyphButton gameBtn;
    private OutlinedGlyphButton settingsBtn;

    // Panels
    private FrameLayout modalContainer;
    private FrameLayout contextMenuContainer;
    private LibraryPanel libraryPanel;
    private OrderPanel orderPanel;
    private ScenesPanel scenesPanel;
    private TechniquesPanel techniquesPanel;
    private SettingsPanel settingsPanel;
    private RunnerGameView runnerGameView;
    private View activePanel = null;
    private View lastFocusedView = null;
    private String lastFocusedPackage = null;
    private boolean lastFocusedIsLibrary = false;
    private boolean activityResumed;
    private UpdateController updateController;
    private final Runnable updateCheckRunnable = new Runnable() {
        @Override public void run() {
            if (!activityResumed) return;
            updateController.checkAutomatically();
            mainHandler.postDelayed(this, 60_000L);
        }
    };
    private boolean appInventoryReady;
    private boolean focusFirstAppOnHomeEntry = true;
    private ViewTreeObserver.OnGlobalFocusChangeListener focusChangeListener;
    private final ViewTreeObserver.OnGlobalLayoutListener homeEntryLayoutListener = () -> {
        if (focusFirstAppOnHomeEntry) restoreHomeFocus();
    };

    // Timers
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable rotationRunnable;
    private Runnable idleRunnable;
    private final Runnable clockRunnable = new Runnable() {
        @Override
        public void run() {
            updateClock();
            mainHandler.postDelayed(this, 10000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Повноекранний TV-режим
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );

        // 1. Ініціалізація підсистем
        prefs = new PreferencesManager(this);
        appManager = new AppManager(this, prefs);
        homeScreenSetup = new HomeScreenSetup(this, savedInstanceState);
        if (HomeRedirectService.ACTION_REDIRECTED_HOME.equals(getIntent().getAction())) {
            homeScreenSetup.onRedirectedHome();
        }
        updateController = new UpdateController(this, () -> activityResumed && hasWindowFocus()
                && activePanel == null && ambientOverlay != null && ambientOverlay.getVisibility() != View.VISIBLE
                && !isContextMenuOpen()
                && !homeScreenSetup.isPromptShowing() && !audioController.isLaunching()
                && !updateController.isShowing(), this::showAvailableUpdate);
        audioController = new AudioController(this, prefs);
        techniqueRenderer = new TechniqueRenderer(this);

        // 2. Створення верстки
        buildLayout();
        updateController.restoreAvailability();

        // 3. Завантаження початкового стану
        applyScene(prefs.getScene(), false);
        applyTechnique(prefs.getTechnique());
        dockView.render();
        wireSpatialNavigation();
        updateClock();
        restoreHomeFocus();

        // 4. Запуск аудіо та таймерів
        audioController.updateMusic();
        setupTimers();
        resetIdleTimer();

    }

    private void buildLayout() {
        rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(Color.parseColor("#071c27"));

        // Фонове село
        backgroundView = new VillageBackgroundView(this);
        backgroundView.setMotionEnabled(prefs.isMotionEnabled());
        rootLayout.addView(backgroundView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ));

        // Головна оболонка
        mainShell = new LinearLayout(this);
        mainShell.setOrientation(LinearLayout.VERTICAL);
        mainShell.setPadding(dp(40), dp(20), dp(40), dp(8));

        // Header (Топбар)
        LinearLayout topBar = createTopBar();
        mainShell.addView(topBar);

        // Hero (Секція цитат і сцени)
        LinearLayout heroSection = createHeroSection();
        mainShell.addView(heroSection, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f
        ));

        // Док додатків (Сувої розваг)
        LinearLayout appSection = createAppSection();
        mainShell.addView(appSection);

        // Footer (Підказки керування)
        LinearLayout footer = createFooter();
        mainShell.addView(footer);

        // Оверлей чакри для фокусу (додається ПЕРЕД mainShell, щоб малюватися позаду карток)
        chakraFocusView = new ChakraFocusView(this, techniqueRenderer);
        chakraFocusView.setMotionEnabled(prefs.isMotionEnabled());
        chakraFocusView.setTechnique(prefs.getTechnique());
        rootLayout.addView(chakraFocusView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ));

        mainShell.setElevation(10f);
        rootLayout.addView(mainShell, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ));

        // Контейнер модальних панелей
        modalContainer = new FrameLayout(this);
        modalContainer.setVisibility(View.GONE);
        modalContainer.setElevation(50f);
        rootLayout.addView(modalContainer, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ));

        // Оверлей тихої миті
        ambientOverlay = new AmbientOverlay(this);
        ambientOverlay.setVisibility(View.GONE);
        ambientOverlay.setElevation(60f);
        ambientOverlay.setOnAmbientExitListener(this::exitAmbient);
        rootLayout.addView(ambientOverlay, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ));

        // Спливаючі сповіщення
        toastOverlay = new ToastOverlay(this);
        rootLayout.addView(toastOverlay, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ));

        // Контейнер контекстного меню застосунків
        contextMenuContainer = new FrameLayout(this);
        contextMenuContainer.setVisibility(View.GONE);
        contextMenuContainer.setElevation(70f);
        rootLayout.addView(contextMenuContainer, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ));

        setContentView(rootLayout);
        focusChangeListener = (oldFocus, newFocus) -> {
            if (isInside(newFocus, dockView)) {
                chakraFocusView.setTargetView(newFocus);
            } else if (newFocus != null && newFocus != rootLayout) {
                chakraFocusView.setTargetView(null);
            }
            if (activePanel == null && newFocus != null && newFocus != rootLayout
                    && !isInside(newFocus, modalContainer) && !isInside(newFocus, ambientOverlay)
                    && !isInside(newFocus, contextMenuContainer)) rememberHomeFocus(newFocus);
        };
        rootLayout.getViewTreeObserver().addOnGlobalFocusChangeListener(focusChangeListener);
        rootLayout.getViewTreeObserver().addOnGlobalLayoutListener(homeEntryLayoutListener);
    }

    private LinearLayout createTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(64)
        ));

        // Бренд / Лого (#home-btn / .brand)
        brandBtn = new FrameLayout(this);
        brandBtn.setFocusable(true);
        brandBtn.setFocusableInTouchMode(true);
        brandBtn.setClickable(true);
        ImageView logo = new ImageView(this);
        logo.setScaleType(ImageView.ScaleType.FIT_START);
        try (var is = getAssets().open("branding/naruto-logo.webp")) {
            logo.setImageBitmap(android.graphics.BitmapFactory.decodeStream(is));
        } catch (Exception ignored) {}
        brandBtn.addView(logo, new FrameLayout.LayoutParams(dp(150), dp(54), Gravity.CENTER_VERTICAL));
        brandBtn.setOnFocusChangeListener((v, hasFocus) -> {
            v.animate().scaleX(hasFocus ? 1.05f : 1.0f).scaleY(hasFocus ? 1.05f : 1.0f).setDuration(150).start();
        });
        brandBtn.setOnClickListener(v -> {
            audioController.playUiSound();
            restoreHomeFocus();
        });
        bar.addView(brandBtn);

        View spacer = new View(this);
        bar.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1.0f));

        // Top Status (Годинник, дата + кнопки мінігри і налаштувань)
        LinearLayout topStatus = new LinearLayout(this);
        topStatus.setOrientation(LinearLayout.HORIZONTAL);
        topStatus.setGravity(Gravity.CENTER_VERTICAL);

        updateNotice = new TextView(this);
        updateNotice.setTextColor(KonohaTheme.COLOR_ORANGE);
        updateNotice.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        updateNotice.setTypeface(KonohaTheme.getNunitoBold(this));
        updateNotice.setSingleLine(true);
        updateNotice.setEllipsize(android.text.TextUtils.TruncateAt.END);
        updateNotice.setMaxWidth(dp(130));
        updateNotice.setPaddingRelative(0, 0, dp(12), 0);
        updateNotice.setVisibility(View.GONE);
        topStatus.addView(updateNotice);

        LinearLayout clockLayout = new LinearLayout(this);
        clockLayout.setOrientation(LinearLayout.VERTICAL);
        clockLayout.setGravity(Gravity.END);
        clockLayout.setPadding(0, 0, dp(12), 0);

        timeView = new TextView(this);
        timeView.setTextColor(KonohaTheme.COLOR_PAPER);
        timeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        timeView.setTypeface(KonohaTheme.getNunitoBold(this));
        clockLayout.addView(timeView);

        dateView = new TextView(this);
        dateView.setTextColor(Color.parseColor("#d3d3bc"));
        dateView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        dateView.setTypeface(KonohaTheme.getNunito(this));
        clockLayout.addView(dateView);

        topStatus.addView(clockLayout);

        // Кнопка мінігри (елегантний безрамковий гліф)
        gameBtn = new OutlinedGlyphButton(this);
        gameBtn.setBorderless(true);
        gameBtn.setGlyph(GlyphIconView.Glyph.GAMEPAD);
        gameBtn.setIconSize(18);
        gameBtn.setOnClickListener(v -> openGame());
        topStatus.addView(gameBtn);

        // Кнопка налаштувань (елегантний безрамковий гліф)
        settingsBtn = new OutlinedGlyphButton(this);
        settingsBtn.setBorderless(true);
        settingsBtn.setGlyph(GlyphIconView.Glyph.SCROLL);
        settingsBtn.setIconSize(18);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        slp.setMargins(dp(6), 0, 0, 0);
        settingsBtn.setLayoutParams(slp);
        settingsBtn.setOnClickListener(v -> openSettings());
        topStatus.addView(settingsBtn);

        bar.addView(topStatus);
        return bar;
    }

    private void showAvailableUpdate(String versionName) {
        updateNotice.setText(versionName == null ? null : "Нова версія " + versionName);
        updateNotice.setVisibility(versionName == null ? View.GONE : View.VISIBLE);
    }

    private LinearLayout createHeroSection() {
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.BOTTOM);
        hero.setPadding(0, 0, 0, dp(8));

        // Keep the caption and selector at their natural height on shorter TV viewports.
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        hero.addView(copy, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        heroTitleView = new HeroTitleView(this);
        copy.addView(heroTitleView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        heroDescView = new TextView(this);
        heroDescView.setTextColor(Color.parseColor("#d9d7bf"));
        heroDescView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        heroDescView.setTypeface(KonohaTheme.getNunito(this));
        heroDescView.setPadding(0, dp(4), 0, 0);
        copy.addView(heroDescView);

        quoteAuthorView = new TextView(this);
        quoteAuthorView.setTextColor(Color.parseColor("#b9cac1"));
        quoteAuthorView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        quoteAuthorView.setTypeface(KonohaTheme.getNunitoBold(this));
        quoteAuthorView.setPadding(0, dp(2), 0, dp(10));
        copy.addView(quoteAuthorView);

        sceneChoiceBtn = new OutlinedGlyphButton(this);
        sceneChoiceBtn.setPill(true);
        sceneChoiceBtn.setGlyph(GlyphIconView.Glyph.WALLPAPERS);
        sceneChoiceBtn.setOnClickListener(v -> openScenes());
        LinearLayout.LayoutParams scLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        sceneChoiceBtn.setLayoutParams(scLp);
        copy.addView(sceneChoiceBtn);

        return hero;
    }

    private LinearLayout createAppSection() {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.setPaddingRelative(dp(9), 0, 0, dp(4));

        // Tiny seal 忍 badge (.tiny-seal)
        TextView sealBadge = new TextView(this);
        sealBadge.setText("忍");
        sealBadge.setTextColor(KonohaTheme.COLOR_ORANGE);
        sealBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        sealBadge.setTypeface(Typeface.SERIF);
        sealBadge.setGravity(Gravity.CENTER);
        sealBadge.setRotation(-6f);
        android.graphics.drawable.GradientDrawable sealBg = new android.graphics.drawable.GradientDrawable();
        sealBg.setColor(Color.TRANSPARENT);
        sealBg.setStroke(dp(1), Color.parseColor("#44ed943f"));
        sealBg.setCornerRadius(dp(3));
        sealBadge.setBackground(sealBg);
        int sealW = dp(18);
        int sealH = dp(20);
        LinearLayout.LayoutParams sealLp = new LinearLayout.LayoutParams(sealW, sealH);
        int labelStart = sceneChoiceBtn.getPaddingStart() + dp(16) + dp(8);
        sealLp.setMarginEnd(labelStart - heading.getPaddingStart() - sealW);
        heading.addView(sealBadge, sealLp);

        TextView title = new TextView(this);
        title.setText("Сувої розваг");
        title.setTextColor(KonohaTheme.COLOR_PAPER);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        title.setTypeface(KonohaTheme.getNunitoBold(this));
        heading.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        techniqueBtn = new TechniqueShortcutButton(this);
        techniqueBtn.setOnClickListener(v -> openTechniques());
        heading.addView(techniqueBtn);

        section.addView(heading);

        // Док
        dockView = new DockView(this, appManager);
        dockView.setOnDockItemClickListener(new DockView.OnDockItemClickListener() {
            @Override public void onAppClick(AppItem app) { launchApp(app); }
            @Override public void onAppLongClick(View view, AppItem app, int position) {
                showContextMenu(app, true, position, view);
            }
            @Override public void onLibraryClick() { openLibrary(); }
        });
        dockView.setOnDockItemFocusListener(new DockView.OnDockItemFocusListener() {
            @Override public void onItemFocused(View view, AppItem app) {
                if (activePanel != null || (ambientOverlay != null && ambientOverlay.getVisibility() == View.VISIBLE)) return;
                lastFocusedView = view;
                lastFocusedPackage = app != null ? app.getPackageName() : null;
                lastFocusedIsLibrary = false;
                chakraFocusView.setTargetView(view);
                resetIdleTimer();
            }
            @Override public void onLibraryFocused(View view) {
                if (activePanel != null || (ambientOverlay != null && ambientOverlay.getVisibility() == View.VISIBLE)) return;
                lastFocusedView = view;
                lastFocusedPackage = null;
                lastFocusedIsLibrary = true;
                chakraFocusView.setTargetView(view);
                resetIdleTimer();
            }
            @Override public void onDockFocusCleared() {
                chakraFocusView.setTargetView(null);
            }
        });
        section.addView(dockView);

        return section;
    }

    private LinearLayout createFooter() {
        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(0, dp(14), 0, dp(4));

        TextView guide = new TextView(this);
        guide.setText("↔ обрати  |  OK відкрити  |  ☰ налаштування");
        guide.setTextColor(Color.parseColor("#92aaa4"));
        guide.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        guide.setTypeface(KonohaTheme.getNunito(this));
        footer.addView(guide, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView resourceStats = new TextView(this);
        resourceStats.setTextColor(KonohaTheme.COLOR_PAPER);
        resourceStats.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        resourceStats.setTypeface(KonohaTheme.getNunito(this));
        resourceStats.setSingleLine(true);
        resourceStats.setPaddingRelative(dp(8), 0, dp(12), 0);
        resourceStats.setVisibility(View.GONE);
        footer.addView(resourceStats);
        resources = new LauncherResourceMonitor(resourceStats, mainHandler);

        TextView tag = new TextView(this);
        tag.setText("Коноха · Воля вогню");
        tag.setTextColor(KonohaTheme.COLOR_ORANGE);
        tag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tag.setTypeface(KonohaTheme.getNunito(this));
        footer.addView(tag);

        return footer;
    }

    public void wireSpatialNavigation() {
        View firstCard = dockView != null ? dockView.getFirstCard() : null;
        View archiveCard = dockView != null ? dockView.getArchiveCard() : null;

        if (sceneChoiceBtn != null) {
            if (sceneChoiceBtn.getId() == View.NO_ID) sceneChoiceBtn.setId(View.generateViewId());
            if (firstCard != null) {
                if (firstCard.getId() == View.NO_ID) firstCard.setId(View.generateViewId());
                firstCard.setNextFocusUpId(sceneChoiceBtn.getId());
                firstCard.setNextFocusLeftId(firstCard.getId());
                sceneChoiceBtn.setNextFocusDownId(firstCard.getId());
            }
            if (brandBtn != null) {
                if (brandBtn.getId() == View.NO_ID) brandBtn.setId(View.generateViewId());
                sceneChoiceBtn.setNextFocusUpId(brandBtn.getId());
                brandBtn.setNextFocusDownId(sceneChoiceBtn.getId());
            }
        }

        if (brandBtn != null && gameBtn != null) {
            if (brandBtn.getId() == View.NO_ID) brandBtn.setId(View.generateViewId());
            if (gameBtn.getId() == View.NO_ID) gameBtn.setId(View.generateViewId());
            brandBtn.setNextFocusRightId(gameBtn.getId());
            gameBtn.setNextFocusLeftId(brandBtn.getId());
        }

        if (gameBtn != null && settingsBtn != null) {
            if (gameBtn.getId() == View.NO_ID) gameBtn.setId(View.generateViewId());
            if (settingsBtn.getId() == View.NO_ID) settingsBtn.setId(View.generateViewId());
            gameBtn.setNextFocusRightId(settingsBtn.getId());
            settingsBtn.setNextFocusLeftId(gameBtn.getId());
        }

        if (techniqueBtn != null) {
            if (techniqueBtn.getId() == View.NO_ID) techniqueBtn.setId(View.generateViewId());
            if (archiveCard != null) {
                if (archiveCard.getId() == View.NO_ID) archiveCard.setId(View.generateViewId());
                archiveCard.setNextFocusUpId(techniqueBtn.getId());
                archiveCard.setNextFocusRightId(archiveCard.getId());
                techniqueBtn.setNextFocusDownId(archiveCard.getId());
            }
            if (gameBtn != null) {
                if (gameBtn.getId() == View.NO_ID) gameBtn.setId(View.generateViewId());
                techniqueBtn.setNextFocusUpId(gameBtn.getId());
                gameBtn.setNextFocusDownId(techniqueBtn.getId());
            }
        }
    }

    private void updateClock() {
        Date now = new Date();
        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", new Locale("uk", "UA"));
        SimpleDateFormat dateFmt = new SimpleDateFormat("EEEE, d MMMM", new Locale("uk", "UA"));
        if (timeView != null) timeView.setText(timeFmt.format(now));
        if (dateView != null) dateView.setText(dateFmt.format(now));
    }

    private void applyScene(String sceneId, boolean animate) {
        Scene scene = CatalogData.SCENES.stream()
                .filter(s -> s.getId().equals(sceneId)).findFirst().orElse(CatalogData.SCENES.get(0));
        prefs.setScene(scene.getId());
        backgroundView.setScene(scene, animate);

        Quote q = CatalogData.QUOTES.get(scene.getId());
        if (q != null) {
            heroTitleView.setTitle(scene.getTitle());
            heroDescView.setText("«" + q.getText() + "»");
            quoteAuthorView.setText(q.getCharacter());
        }

        if (sceneChoiceBtn != null) {
            sceneChoiceBtn.setText(scene.getName());
            sceneChoiceBtn.setContentDescription("Обрати шпалери: " + scene.getName());
        }
    }

    private void applyTechnique(String techniqueId) {
        Technique tech = CatalogData.TECHNIQUES.stream()
                .filter(t -> t.getId().equals(techniqueId)).findFirst().orElse(CatalogData.TECHNIQUES.get(0));
        prefs.setTechnique(tech.getId());
        chakraFocusView.setTechnique(tech.getId());
        techniqueRenderer.trimMemory(TechniqueRenderer.platesFor(tech.getId()));
        if (techniqueBtn != null) {
            techniqueBtn.setTechnique(tech.getName(), tech.getColor());
        }
    }

    private void launchApp(AppItem app) {
        if (audioController.isLaunching()) return; // Захист від повторного OK

        chakraFocusView.triggerBurst();
        if (prefs.isAudioEnabled()) {
            String techName = prefs.getTechnique();
            Technique t = CatalogData.TECHNIQUES.stream()
                    .filter(x -> x.getId().equals(techName)).findFirst().orElse(null);
            String name = t != null ? t.getName() : "Техніка";
            toastOverlay.show(name + "… Back — скасувати", 8000);
        }

        audioController.launchWithTechnique(prefs.getTechnique(), () -> {
            toastOverlay.hide();
            appManager.launchApp(app.getPackageName());
        });
    }

    // Відкриття та закриття модальних панелей
    private void showPanel(View panel) {
        if (activePanel == panel) return;
        if (activePanel == null) {
            rememberHomeFocus();
            // Opening a panel takes precedence over pending cold HOME focus.
            focusFirstAppOnHomeEntry = false;
        }
        closeActivePanel();
        activePanel = panel;
        mainShell.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        modalContainer.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        backgroundView.setDecorPaused(true);
        chakraFocusView.setTargetView(null);
        modalContainer.removeAllViews();
        modalContainer.addView(panel);
        modalContainer.setVisibility(View.VISIBLE);
        panel.requestFocus();
    }

    private boolean isInside(View view, View ancestor) {
        for (View current = view; current != null; ) {
            if (current == ancestor) return true;
            android.view.ViewParent parent = current.getParent();
            current = parent instanceof View ? (View) parent : null;
        }
        return false;
    }

    private void rememberHomeFocus() {
        rememberHomeFocus(rootLayout.findFocus());
    }

    private void rememberHomeFocus(View focused) {
        if (focused == null || isInside(focused, modalContainer) || isInside(focused, ambientOverlay) || isInside(focused, contextMenuContainer)) return;
        lastFocusedView = focused;
        Object tag = focused.getTag();
        lastFocusedPackage = tag instanceof AppItem ? ((AppItem) tag).getPackageName() : null;
        lastFocusedIsLibrary = "archive".equals(tag);
    }

    private void restoreHomeFocus() {
        if (!activityResumed || activePanel != null || ambientOverlay.getVisibility() == View.VISIBLE
                || isContextMenuOpen()) return;
        View target = focusFirstAppOnHomeEntry ? dockView.getFirstCard() : null;
        if (!focusFirstAppOnHomeEntry && lastFocusedIsLibrary) {
            target = dockView.getArchiveCard();
        } else if (!focusFirstAppOnHomeEntry && lastFocusedPackage != null) {
            target = dockView.findCardByPackage(lastFocusedPackage);
        }
        if (target == null && lastFocusedView != null && lastFocusedView.isAttachedToWindow()) {
            target = lastFocusedView;
        }
        if (target == null) {
            target = dockView.getFirstCard();
        }
        if (target != null) {
            lastFocusedView = target;
            boolean inDock = isInside(target, dockView);
            chakraFocusView.setTargetView(inDock ? target : null);
            target.requestFocusFromTouch();
            if (target.requestFocus() && appInventoryReady && target.isLaidOut()) focusFirstAppOnHomeEntry = false;
            final View fTarget = target;
            fTarget.post(() -> {
                if (!activityResumed || activePanel != null || ambientOverlay.getVisibility() == View.VISIBLE
                        || !fTarget.isAttachedToWindow() || lastFocusedView != fTarget) return;
                fTarget.requestFocusFromTouch();
                if (fTarget.requestFocus()) {
                    chakraFocusView.setTargetView(isInside(fTarget, dockView) ? fTarget : null);
                    if (appInventoryReady && fTarget.isLaidOut()) focusFirstAppOnHomeEntry = false;
                }
            });
        }
    }

    public void closeActivePanel() {
        closeContextMenu(false);
        if (activePanel != null) {
            View closing = activePanel;
            if (closing instanceof RunnerGameView) {
                ((RunnerGameView) closing).release();
                runnerGameView = null;
            } else if (closing == scenesPanel) {
                scenesPanel.release();
                scenesPanel = null;
            } else if (closing == techniquesPanel) {
                techniquesPanel.release();
                techniquesPanel = null;
            } else if (closing == libraryPanel) {
                libraryPanel.close();
                libraryPanel = null;
            } else if (closing == orderPanel) {
                orderPanel = null;
            } else if (closing == settingsPanel) {
                settingsPanel = null;
            }
            modalContainer.removeAllViews();
            modalContainer.setVisibility(View.GONE);
            modalContainer.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
            activePanel = null;
            mainShell.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
            techniqueRenderer.trimMemory(TechniqueRenderer.platesFor(prefs.getTechnique()));
            backgroundView.setDecorPaused(false);
            backgroundView.setMotionEnabled(prefs.isMotionEnabled());
            chakraFocusView.setMotionEnabled(prefs.isMotionEnabled());
            chakraFocusView.setTechnique(prefs.getTechnique());
            setupTimers();
            resetIdleTimer();
            wireSpatialNavigation();
            restoreHomeFocus();
            updateController.onHomeAvailable();
        }
    }

    private void openLibrary() {
        if (libraryPanel == null) {
            libraryPanel = new LibraryPanel(this, appManager, dockView, techniqueRenderer);
            libraryPanel.setOnLibraryActionListener(new LibraryPanel.OnLibraryActionListener() {
                @Override public void onAppLaunch(AppItem app) { launchApp(app); }
                @Override public void onAppContextMenuRequested(View view, AppItem app) {
                    showContextMenu(app, false, -1, view);
                }
                @Override public void onOrderPanelRequested() { openOrder(); }
                @Override public void onCloseRequested() { closeActivePanel(); }
                @Override public void onAppFocused(View view, AppItem app) {
                    audioController.playFocusSound();
                }
            });
        }
        libraryPanel.setTechnique(prefs.getTechnique());
        libraryPanel.setMotionEnabled(prefs.isMotionEnabled());
        showPanel(libraryPanel);
        libraryPanel.open();
        audioController.playUiSound();
    }

    private void openOrder() {
        if (orderPanel == null) {
            orderPanel = new OrderPanel(this, appManager, dockView);
            orderPanel.setOnOrderActionListener(new OrderPanel.OnOrderActionListener() {
                @Override public void onBackToLibraryRequested() { openLibrary(); }
                @Override public void onCloseRequested() { closeActivePanel(); }
            });
        }
        showPanel(orderPanel);
        orderPanel.renderList();
        audioController.playUiSound();
    }

    private void openScenes() {
        if (scenesPanel == null) {
            scenesPanel = new ScenesPanel(this, prefs);
            scenesPanel.setOnSceneSelectedListener(new ScenesPanel.OnSceneSelectedListener() {
                @Override public void onSceneSelected(Scene scene) {
                    applyScene(scene.getId(), true);
                    closeActivePanel();
                }
                @Override public void onCloseRequested() { closeActivePanel(); }
            });
        }
        showPanel(scenesPanel);
        scenesPanel.renderScenes();
        audioController.playUiSound();
    }

    private void openTechniques() {
        if (techniquesPanel == null) {
            techniquesPanel = new TechniquesPanel(this, prefs, techniqueRenderer);
            techniquesPanel.setOnTechniqueSelectedListener(new TechniquesPanel.OnTechniqueSelectedListener() {
                @Override public void onTechniqueSelected(Technique technique) {
                    applyTechnique(technique.getId());
                    View first = dockView.getFirstCard();
                    if (first != null) {
                        lastFocusedView = first;
                        lastFocusedPackage = first.getTag() instanceof AppItem ? ((AppItem) first.getTag()).getPackageName() : null;
                        lastFocusedIsLibrary = false;
                    }
                    closeActivePanel();
                    if (first != null) {
                        chakraFocusView.setTargetView(first);
                        first.requestFocusFromTouch();
                        first.requestFocus();
                        first.post(() -> {
                            first.requestFocusFromTouch();
                            first.requestFocus();
                            chakraFocusView.setTargetView(first);
                        });
                    }
                    chakraFocusView.triggerBurst();
                    audioController.playUiSound();
                }
                @Override public void onCloseRequested() { closeActivePanel(); }
            });
        }
        showPanel(techniquesPanel);
        techniquesPanel.renderTechniques();
        audioController.playUiSound();
    }

    private void openSettings() {
        if (settingsPanel == null) {
            settingsPanel = new SettingsPanel(this, prefs, audioController);
            settingsPanel.setOnSettingsActionListener(new SettingsPanel.OnSettingsActionListener() {
                @Override public void onTechniquesRequested() { openTechniques(); }
                @Override public void onAmbientRequested() { enterAmbient(); }
                @Override public void onSystemSettingsRequested(String target) { appManager.openSettings(target); }
                @Override public void onHomeScreenRequested() { homeScreenSetup.openSelection(); }
                @Override public void onHomeRedirectRequested() { homeScreenSetup.showRedirectSettings(); }
                @Override public void onUpdatesRequested() { updateController.checkManually(); }
                @Override public void onRefreshAppsRequested() {
                    appManager.reloadAppsAsync(() -> {
                        if (!activityResumed || isDestroyed()) return;
                        dockView.render();
                        wireSpatialNavigation();
                        toastOverlay.show("Архів застосунків оновлено", 2500);
                    });
                }
                @Override public void onAboutRequested() { showAboutDialog(); }
                @Override public void onResourcesChanged() { updateResourceDisplay(); }
                @Override public void onCloseRequested() { closeActivePanel(); }
            });
        }
        showPanel(settingsPanel);
        settingsPanel.updateState();
        updateHomeSettingsStatus();
        settingsPanel.focusInitial();
        audioController.playUiSound();
    }

    private void updateHomeSettingsStatus() {
        if (settingsPanel == null) return;
        settingsPanel.setHomeScreenActive(homeScreenSetup.isDefaultHome());
        settingsPanel.setHomeRedirectEnabled(HomeRedirectService.isEnabled(this));
    }

    private void updateResourceDisplay() {
        resources.setActive(activityResumed && mainShell.getVisibility() == View.VISIBLE
                && prefs.shouldShowResources());
    }

    private void openGame() {
        if (runnerGameView == null) {
            audioController.prepareRunnerSounds();
            runnerGameView = new RunnerGameView(this, prefs);
            runnerGameView.setOnExitListener(this::closeActivePanel);
            runnerGameView.setSoundCallbacks(audioController::playRunnerSound, audioController::stopRunnerSounds);
        }
        showPanel(runnerGameView);
        runnerGameView.onResumeLifecycle();
        audioController.playUiSound();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Коноха · Воля вогню")
                .setMessage("Основна версія Naruto · Коноха.\n"
                        + "Розробник: Oktirus\n\n"
                        + "Твій домашній екран у світі Наруто:\n"
                        + "• 18 шпалер Конохи з паралаксом і частинками\n"
                        + "• 10 технік навігації та звуки дзюцу\n"
                        + "• Улюблені застосунки у власному порядку\n"
                        + "• Мінігра «Шлях шинобі» з Наруто та Гамабунтою\n"
                        + "• Музика Naruto OST MUSASHI PROJECT\n\n"
                        + "Нативний лаунчер — основний. Версія на Chromium залишається альтернативною.\n\n"
                        + "Права на Naruto належать їхнім власникам. Фанатський лаунчер.")
                .setPositiveButton("Зрозуміло", null)
                .show();
    }

    private View lastContextMenuOriginView;

    private boolean isContextMenuOpen() {
        return contextMenuContainer != null && contextMenuContainer.getVisibility() == View.VISIBLE;
    }

    public void showContextMenu(AppItem app, boolean isFromDock, int dockPosition, View originView) {
        if (!activityResumed || isDestroyed() || isFinishing()) return;
        this.lastContextMenuOriginView = originView;
        mainShell.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        if (activePanel != null && modalContainer != null) {
            modalContainer.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        }
        contextMenuContainer.removeAllViews();
        List<AppItem> dockApps = appManager.getDockApps();
        AppContextMenuPanel panel = new AppContextMenuPanel(this, app, isFromDock,
                dockPosition, dockApps.size(), new AppContextMenuPanel.OnContextMenuActionListener() {
            @Override
            public void onLaunch(AppItem a) {
                closeContextMenu();
                launchApp(a);
            }

            @Override
            public void onTogglePin(AppItem a) {
                lastContextMenuOriginView = null;
                closeContextMenu();
                boolean nowPinned = appManager.togglePin(a.getPackageName());
                if (dockView != null) dockView.render();
                wireSpatialNavigation();
                if (libraryPanel != null) libraryPanel.renderApps();
                if (orderPanel != null) orderPanel.renderList();
                audioController.playUiSound();
                String message = "☆ Прибрано з обраного";
                if (nowPinned) {
                    boolean visibleOnHome = dockView.findCardByPackage(a.getPackageName()) != null;
                    message = visibleOnHome ? "★ Додано до обраного й показано на головному екрані"
                            : "★ Додано до обраного · поза головним екраном";
                }
                toastOverlay.show(message, 2500);
                if (activePanel == libraryPanel && libraryPanel != null) {
                    View newCard = libraryPanel.findCardByPackage(a.getPackageName());
                    if (newCard != null) newCard.post(newCard::requestFocus);
                } else if (dockView != null) {
                    View newCard = dockView.findCardByPackage(a.getPackageName());
                    if (newCard != null) newCard.post(newCard::requestFocus);
                    else restoreHomeFocus();
                } else {
                    restoreHomeFocus();
                }
            }

            @Override
            public void onMove(AppItem a, int direction) {
                lastContextMenuOriginView = null;
                closeContextMenu();
                appManager.moveFavorite(a.getPackageName(), direction);
                if (dockView != null) dockView.render();
                wireSpatialNavigation();
                if (orderPanel != null) orderPanel.renderList();
                audioController.playUiSound();
                if (dockView != null) {
                    View newCard = dockView.findCardByPackage(a.getPackageName());
                    if (newCard != null) {
                        newCard.post(newCard::requestFocus);
                    }
                }
            }

            @Override
            public void onOpenOrder() {
                lastContextMenuOriginView = null;
                closeContextMenu();
                openOrder();
            }

            @Override
            public void onAppInfo(AppItem a) {
                closeContextMenu();
                openAppInfo(a);
            }

            @Override
            public void onDismiss() {
                closeContextMenu();
            }
        });

        contextMenuContainer.addView(panel);
        contextMenuContainer.setVisibility(View.VISIBLE);
        audioController.playUiSound();
    }

    public void closeContextMenu() {
        closeContextMenu(true);
    }

    private void closeContextMenu(boolean restoreFocus) {
        if (isContextMenuOpen()) {
            contextMenuContainer.removeAllViews();
            contextMenuContainer.setVisibility(View.GONE);
            if (modalContainer != null) {
                modalContainer.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
            }
            if (activePanel == null) {
                mainShell.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
            }
            if (restoreFocus && lastContextMenuOriginView != null && lastContextMenuOriginView.isAttachedToWindow()) {
                // Restore before starting an external activity so the library can retain the package on resume.
                lastContextMenuOriginView.requestFocus();
            } else if (restoreFocus && activePanel == null) {
                restoreHomeFocus();
            }
            lastContextMenuOriginView = null;
        }
    }

    public void openAppInfo(AppItem app) {
        if (app == null) return;
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(android.net.Uri.parse("package:" + app.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Не вдалося відкрити налаштування", Toast.LENGTH_SHORT).show();
        }
    }

    private void enterAmbient() {
        if (activePanel == null) rememberHomeFocus();
        closeActivePanel();
        chakraFocusView.setTargetView(null);
        ambientOverlay.start();
        mainShell.setVisibility(View.INVISIBLE);
        updateResourceDisplay();
        mainShell.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        backgroundView.setDecorPaused(true);
    }

    private void exitAmbient() {
        ambientOverlay.stop();
        mainShell.setVisibility(View.VISIBLE);
        updateResourceDisplay();
        mainShell.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        backgroundView.setDecorPaused(false);
        resetIdleTimer();
        restoreHomeFocus();
        updateController.onHomeAvailable();
    }

    // Таймери ротації та спокою
    private void setupTimers() {
        mainHandler.removeCallbacks(clockRunnable);
        mainHandler.post(clockRunnable);

        setupRotationTimer();
    }

    private void setupRotationTimer() {
        if (rotationRunnable != null) mainHandler.removeCallbacks(rotationRunnable);
        int rotSec = prefs.getRotationSeconds();
        if (rotSec > 0) {
            rotationRunnable = new Runnable() {
                @Override
                public void run() {
                    if (activePanel == null && ambientOverlay.getVisibility() != View.VISIBLE) {
                        int currentIndex = 0;
                        for (int i = 0; i < CatalogData.SCENES.size(); i++) {
                            if (CatalogData.SCENES.get(i).getId().equals(prefs.getScene())) {
                                currentIndex = i;
                                break;
                            }
                        }
                        int nextIndex = (currentIndex + 1) % CatalogData.SCENES.size();
                        applyScene(CatalogData.SCENES.get(nextIndex).getId(), true);
                    }
                    mainHandler.postDelayed(this, rotSec * 1000L);
                }
            };
            mainHandler.postDelayed(rotationRunnable, rotSec * 1000L);
        }
    }

    private void resetIdleTimer() {
        if (idleRunnable != null) mainHandler.removeCallbacks(idleRunnable);
        int idleSec = prefs.getIdleSeconds();
        if (idleSec > 0) {
            idleRunnable = () -> {
                if (activePanel == null && ambientOverlay.getVisibility() != View.VISIBLE
                        && !homeScreenSetup.isPromptShowing() && !updateController.isShowing()) {
                    enterAmbient();
                } else {
                    resetIdleTimer();
                }
            };
            mainHandler.postDelayed(idleRunnable, idleSec * 1000L);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        resetIdleTimer();

        int keyCode = event.getKeyCode();
        boolean back = keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE;
        boolean menu = keyCode == KeyEvent.KEYCODE_MENU;
        if (event.getAction() == KeyEvent.ACTION_DOWN && (back || menu) && event.getRepeatCount() > 0) {
            return true;
        }

        // 1. Контекстне меню перехоплює Back, Escape та Menu для закриття
        if (isContextMenuOpen()) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && (back || menu)) {
                closeContextMenu();
                return true;
            }
            return super.dispatchKeyEvent(event);
        }

        // 2. Скасування очікування запуску або закриття активних панелей по Back / Escape, відкриття налаштувань по Menu
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (back) {
                if (audioController.cancelPendingLaunch()) {
                    toastOverlay.hide();
                    updateController.onHomeAvailable();
                    return true;
                }
                if (ambientOverlay.getVisibility() == View.VISIBLE) {
                    exitAmbient();
                    return true;
                }
                if (activePanel != null) {
                    if (activePanel != settingsPanel || !settingsPanel.goBack()) closeActivePanel();
                    return true;
                }
                if (brandBtn != null) {
                    brandBtn.requestFocus();
                }
                return true;
            }

            if (menu) {
                if (ambientOverlay.getVisibility() == View.VISIBLE) {
                    exitAmbient();
                    return true;
                }
                if (activePanel instanceof RunnerGameView) {
                    return runnerGameView.onKeyDown(keyCode, event);
                }
                if (settingsPanel != null && activePanel == settingsPanel) {
                    closeActivePanel();
                } else {
                    openSettings();
                }
                return true;
            }
        }

        if (event.getAction() != KeyEvent.ACTION_DOWN || !activityResumed || activePanel != null
                || ambientOverlay.getVisibility() == View.VISIBLE) {
            return super.dispatchKeyEvent(event);
        }
        final int direction;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT: direction = View.FOCUS_LEFT; break;
            case KeyEvent.KEYCODE_DPAD_RIGHT: direction = View.FOCUS_RIGHT; break;
            case KeyEvent.KEYCODE_DPAD_UP: direction = View.FOCUS_UP; break;
            case KeyEvent.KEYCODE_DPAD_DOWN: direction = View.FOCUS_DOWN; break;
            default: return super.dispatchKeyEvent(event);
        }

        // Home intents, layout and inventory refresh can all change focus without navigation input.
        View before = rootLayout.findFocus();
        boolean handled = super.dispatchKeyEvent(event);
        View after = rootLayout.findFocus();
        if (!handled && before != null && after == before) {
            View next = before.focusSearch(direction);
            if (next != before && isInside(next, dockView) && next.requestFocus(direction)) {
                after = next;
                handled = true;
            }
        }
        if (after != before && isInside(after, dockView)) {
            focusFirstAppOnHomeEntry = false;
            audioController.playFocusSound();
        }
        return handled;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        boolean redirected = HomeRedirectService.ACTION_REDIRECTED_HOME.equals(intent.getAction());
        if (redirected) homeScreenSetup.onRedirectedHome();
        if (redirected
                || (Intent.ACTION_MAIN.equals(intent.getAction()) && intent.hasCategory(Intent.CATEGORY_HOME))) {
            updateController.cancel();
            focusFirstAppOnHomeEntry = true;
            audioController.cancelPendingLaunch();
            audioController.stopTechniquePreview();
            toastOverlay.hide();
            if (ambientOverlay.getVisibility() == View.VISIBLE) exitAmbient();
            closeActivePanel();
            restoreHomeFocus();
            updateController.onHomeAvailable();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) TVKeyHelper.cancelPendingPress(getWindow().getDecorView().findFocus());
        if (hasFocus && homeScreenSetup != null) {
            homeScreenSetup.onWindowFocus();
            updateHomeSettingsStatus();
        }
        if (hasFocus && activityResumed && activePanel == null && !updateController.isShowing()
                && (ambientOverlay == null || ambientOverlay.getVisibility() != View.VISIBLE)) {
            restoreHomeFocus();
            homeScreenSetup.showFirstRunPrompt();
            updateController.onHomeAvailable();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        homeScreenSetup.saveState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (homeScreenSetup != null) homeScreenSetup.onActivityResult(requestCode);
    }

    @Override
    protected void onResume() {
        super.onResume();
        activityResumed = true;
        updateResourceDisplay();
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );
        appManager.reloadAppsAsync(() -> {
            if (!activityResumed || isDestroyed()) return;
            // Keep the entry focus request until the asynchronous app inventory is published.
            appInventoryReady = true;
            dockView.render();
            wireSpatialNavigation();
            if (activePanel == null && ambientOverlay.getVisibility() != View.VISIBLE) {
                restoreHomeFocus();
            } else if (activePanel == libraryPanel && libraryPanel != null) {
                libraryPanel.renderGrid();
            }
        });
        dockView.render();
        if (activePanel == null && ambientOverlay.getVisibility() != View.VISIBLE) {
            restoreHomeFocus();
        }
        if (ambientOverlay != null) ambientOverlay.resume();
        backgroundView.resume();
        backgroundView.setDecorPaused(activePanel != null || ambientOverlay.getVisibility() == View.VISIBLE);
        backgroundView.setMotionEnabled(prefs.isMotionEnabled());
        if (chakraFocusView != null) {
            chakraFocusView.setMotionEnabled(prefs.isMotionEnabled());
            chakraFocusView.resume();
        }
        if (libraryPanel != null) libraryPanel.resume();
        if (activePanel instanceof RunnerGameView) {
            ((RunnerGameView) activePanel).onResumeLifecycle();
        } else if (activePanel == scenesPanel && scenesPanel != null) {
            scenesPanel.renderScenes();
        } else if (activePanel == techniquesPanel && techniquesPanel != null) {
            techniquesPanel.renderTechniques();
        }
        audioController.resumeMusic();
        homeScreenSetup.onResume();
        updateHomeSettingsStatus();
        updateController.onResume();
        updateController.onHomeAvailable();
        mainHandler.removeCallbacks(updateCheckRunnable);
        mainHandler.postDelayed(updateCheckRunnable, 3000);
        setupTimers();
        resetIdleTimer();
    }

    @Override
    protected void onPause() {
        activityResumed = false;
        updateResourceDisplay();
        TVKeyHelper.cancelPendingPress(getWindow().getDecorView().findFocus());
        closeContextMenu(false);
        mainHandler.removeCallbacks(updateCheckRunnable);
        updateController.cancel();
        audioController.cancelPendingLaunch();
        toastOverlay.hide();
        audioController.pauseMusic();
        backgroundView.pause();
        if (chakraFocusView != null) chakraFocusView.pause();
        if (libraryPanel != null) libraryPanel.pause();
        if (runnerGameView != null) runnerGameView.onPauseLifecycle();
        if (ambientOverlay != null) ambientOverlay.pause();
        if (rotationRunnable != null) mainHandler.removeCallbacks(rotationRunnable);
        if (idleRunnable != null) mainHandler.removeCallbacks(idleRunnable);
        mainHandler.removeCallbacks(clockRunnable);
        super.onPause();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            if (techniquesPanel != null) techniquesPanel.clearCache();
            if (scenesPanel != null) scenesPanel.clearCache();
            if (techniqueRenderer != null) techniqueRenderer.clearCache();
            if (activityResumed) {
                if (activePanel == scenesPanel && scenesPanel != null) scenesPanel.renderScenes();
                if (activePanel == techniquesPanel && techniquesPanel != null) techniquesPanel.renderTechniques();
                if (libraryPanel != null) libraryPanel.resume();
                if (chakraFocusView != null) chakraFocusView.invalidate();
            }
        }
    }

    @Override
    protected void onDestroy() {
        closeContextMenu(false);
        resources.close();
        mainHandler.removeCallbacksAndMessages(null);
        if (focusChangeListener != null && rootLayout != null && rootLayout.getViewTreeObserver().isAlive()) {
            rootLayout.getViewTreeObserver().removeOnGlobalFocusChangeListener(focusChangeListener);
            rootLayout.getViewTreeObserver().removeOnGlobalLayoutListener(homeEntryLayoutListener);
        }
        if (runnerGameView != null) runnerGameView.release();
        if (techniquesPanel != null) techniquesPanel.release();
        if (scenesPanel != null) scenesPanel.release();
        if (libraryPanel != null) libraryPanel.close();
        if (dockView != null) dockView.release();
        if (backgroundView != null) backgroundView.release();
        if (ambientOverlay != null) ambientOverlay.pause();
        if (techniqueRenderer != null) techniqueRenderer.release();
        appManager.release();
        if (homeScreenSetup != null) homeScreenSetup.close();
        if (updateController != null) updateController.close();
        audioController.release();
        super.onDestroy();
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()
        );
    }
}
