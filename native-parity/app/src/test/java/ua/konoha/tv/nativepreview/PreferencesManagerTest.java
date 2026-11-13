package ua.konoha.tv.nativepreview;

import android.content.SharedPreferences;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ua.konoha.tv.nativepreview.prefs.PreferencesManager;

import java.util.*;

public class PreferencesManagerTest {

    private static class MockSharedPreferences implements SharedPreferences {
        private final Map<String, Object> values = new HashMap<>();

        @Override
        public Map<String, ?> getAll() { return new HashMap<>(values); }

        @Override
        public String getString(String key, String defValue) {
            Object v = values.get(key);
            return v instanceof String ? (String) v : defValue;
        }

        @Override
        public Set<String> getStringSet(String key, Set<String> defValues) { return defValues; }

        @Override
        public int getInt(String key, int defValue) {
            Object v = values.get(key);
            return v instanceof Integer ? (Integer) v : defValue;
        }

        @Override
        public long getLong(String key, long defValue) { return defValue; }

        @Override
        public float getFloat(String key, float defValue) {
            Object v = values.get(key);
            return v instanceof Float ? (Float) v : defValue;
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            Object v = values.get(key);
            return v instanceof Boolean ? (Boolean) v : defValue;
        }

        @Override
        public boolean contains(String key) { return values.containsKey(key); }

        @Override
        public Editor edit() { return new MockEditor(this); }

        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

        private static class MockEditor implements Editor {
            private final MockSharedPreferences parent;
            private final Map<String, Object> temp = new HashMap<>();
            private final Set<String> removed = new HashSet<>();

            MockEditor(MockSharedPreferences parent) { this.parent = parent; }

            @Override
            public Editor putString(String key, String value) { temp.put(key, value); return this; }
            @Override
            public Editor putStringSet(String key, Set<String> values) { return this; }
            @Override
            public Editor putInt(String key, int value) { temp.put(key, value); return this; }
            @Override
            public Editor putLong(String key, long value) { return this; }
            @Override
            public Editor putFloat(String key, float value) { temp.put(key, value); return this; }
            @Override
            public Editor putBoolean(String key, boolean value) { temp.put(key, value); return this; }
            @Override
            public Editor remove(String key) { removed.add(key); return this; }
            @Override
            public Editor clear() { temp.clear(); removed.addAll(parent.values.keySet()); return this; }
            @Override
            public boolean commit() { apply(); return true; }
            @Override
            public void apply() {
                for (String r : removed) parent.values.remove(r);
                parent.values.putAll(temp);
            }
        }
    }

    private PreferencesManager pm;

    @Before
    public void setUp() {
        pm = new PreferencesManager(new MockSharedPreferences());
    }

    @Test
    public void testFreshNativeInstallDefaults() {
        Assert.assertEquals("dusk", pm.getScene());
        Assert.assertTrue(pm.isAudioEnabled());
        Assert.assertTrue(pm.isAmbienceEnabled());
        Assert.assertEquals(0.45f, pm.getVolume(), 0.001f);
        Assert.assertEquals(0.22f, pm.getMusicVolume(), 0.001f);
        Assert.assertEquals("1584791975", pm.getMusicTrack());
        Assert.assertEquals("rasengan", pm.getTechnique());
        Assert.assertEquals("jutsu", pm.getHighlightSound());
        Assert.assertTrue(pm.isMotionEnabled());
        Assert.assertEquals(60, pm.getRotationSeconds());
        Assert.assertEquals(900, pm.getIdleSeconds());
        Assert.assertNull(pm.getPins());
    }

    @Test
    public void testSavedSettingsSurviveNewDefaults() {
        MockSharedPreferences saved = new MockSharedPreferences();
        PreferencesManager previous = new PreferencesManager(saved);
        previous.setMotionEnabled(false);
        previous.setAudioEnabled(false);
        previous.setAmbienceEnabled(false);
        previous.setRotationSeconds(0);
        previous.setIdleSeconds(300);
        previous.setTechnique("gamabunta");
        Map<String, ?> before = saved.getAll();

        PreferencesManager updated = new PreferencesManager(saved);
        Assert.assertFalse(updated.isMotionEnabled());
        Assert.assertFalse(updated.isAudioEnabled());
        Assert.assertFalse(updated.isAmbienceEnabled());
        Assert.assertEquals(0, updated.getRotationSeconds());
        Assert.assertEquals(300, updated.getIdleSeconds());
        Assert.assertEquals("gamabunta", updated.getTechnique());
        Assert.assertEquals(before, saved.getAll());
        updated.setIdleSeconds(0);
        Assert.assertEquals("Explicitly disabled idle stays disabled", 0,
                new PreferencesManager(saved).getIdleSeconds());
    }

    @Test
    public void testImportFromWebJson() {
        String webJson = "{" +
                "\"scene\":\"kakashi\"," +
                "\"audio\":true," +
                "\"ambience\":false," +
                "\"volume\":0.8," +
                "\"musicVolume\":0.3," +
                "\"music\":\"1584792236\"," +
                "\"technique\":\"chidori\"," +
                "\"highlightSound\":\"amaterasu\"," +
                "\"motion\":false," +
                "\"rotation\":60," +
                "\"idle\":300," +
                "\"pins\":[\"pkg.a\",\"pkg.b\"]" +
                "}";

        boolean res = pm.importFromWebJson(webJson);
        Assert.assertTrue(res);
        Assert.assertEquals("kakashi", pm.getScene());
        Assert.assertTrue(pm.isAudioEnabled());
        Assert.assertFalse(pm.isAmbienceEnabled());
        Assert.assertEquals(0.8f, pm.getVolume(), 0.01f);
        Assert.assertEquals(0.3f, pm.getMusicVolume(), 0.01f);
        Assert.assertEquals("1584792236", pm.getMusicTrack());
        Assert.assertEquals("chidori", pm.getTechnique());
        Assert.assertEquals("amaterasu", pm.getHighlightSound());
        Assert.assertFalse(pm.isMotionEnabled());
        Assert.assertEquals(60, pm.getRotationSeconds());
        Assert.assertEquals(300, pm.getIdleSeconds());
        List<String> pins = pm.getPins();
        Assert.assertNotNull(pins);
        Assert.assertEquals(2, pins.size());
        Assert.assertEquals("pkg.a", pins.get(0));
        Assert.assertEquals("pkg.b", pins.get(1));
    }

    @Test
    public void testNightSceneMigratesToTeam7() {
        String webJson = "{\"scene\":\"night\"}";
        pm.importFromWebJson(webJson);
        Assert.assertEquals("team7", pm.getScene());
    }

    @Test
    public void testRealV26DefaultWithNullPins() {
        // v26 default config in localStorage
        String v26Default = "{" +
                "\"scene\":\"dusk\"," +
                "\"audio\":false," +
                "\"ambience\":true," +
                "\"volume\":0.45," +
                "\"musicVolume\":0.22," +
                "\"music\":\"1584791975\"," +
                "\"technique\":\"rasengan\"," +
                "\"highlightSound\":\"jutsu\"," +
                "\"motion\":true," +
                "\"rotation\":0," +
                "\"idle\":0," +
                "\"pins\":null" +
                "}";
        boolean ok = pm.importFromWebJson(v26Default);
        Assert.assertTrue(ok);
        Assert.assertNull(pm.getPins());
        Assert.assertEquals("dusk", pm.getScene());
        Assert.assertEquals("rasengan", pm.getTechnique());
    }

    @Test
    public void testUnknownIdsFallbackToCatalogDefaults() {
        String json = "{" +
                "\"scene\":\"unknown_scene_xyz\"," +
                "\"music\":\"unknown_music_123\"," +
                "\"technique\":\"unknown_tech_abc\"," +
                "\"highlightSound\":\"unknown_cue_999\"" +
                "}";
        boolean ok = pm.importFromWebJson(json);
        Assert.assertTrue(ok);
        Assert.assertEquals("team7", pm.getScene());
        Assert.assertEquals("1584791975", pm.getMusicTrack());
        Assert.assertEquals("rasengan", pm.getTechnique());
        Assert.assertEquals("jutsu", pm.getHighlightSound());
    }

    @Test
    public void testVolumeClampingAndNonFiniteGuards() {
        // Clamping negative and > 1
        pm.importFromWebJson("{\"volume\":-0.5, \"musicVolume\":1.75}");
        Assert.assertEquals(0.0f, pm.getVolume(), 0.001f);
        Assert.assertEquals(1.0f, pm.getMusicVolume(), 0.001f);

        // NaN and Infinity strings must not corrupt existing volume
        pm.importFromWebJson("{\"volume\":\"NaN\", \"musicVolume\":\"Infinity\"}");
        Assert.assertEquals(0.0f, pm.getVolume(), 0.001f);
        Assert.assertEquals(1.0f, pm.getMusicVolume(), 0.001f);
    }

    @Test
    public void testMalformedNonArrayPinsPreservesExistingPins() {
        // First set valid pins
        pm.setPins(Arrays.asList("com.app.one", "com.app.two"));
        Assert.assertEquals(2, pm.getPins().size());

        // Import malformed pins (non-array number/string)
        pm.importFromWebJson("{\"pins\":12345}");
        Assert.assertEquals(2, pm.getPins().size());
        Assert.assertEquals("com.app.one", pm.getPins().get(0));

        pm.importFromWebJson("{\"pins\":\"not_an_array\"}");
        Assert.assertEquals(2, pm.getPins().size());
    }

    @Test
    public void testPinsSanitizationFiltersNonStringsAndTrims() {
        String json = "{\"pins\":[123, null, \"   \", \"  com.valid.pkg  \", false, \"com.other.pkg\"]}";
        pm.importFromWebJson(json);
        List<String> pins = pm.getPins();
        Assert.assertNotNull(pins);
        Assert.assertEquals(2, pins.size());
        Assert.assertEquals("com.valid.pkg", pins.get(0));
        Assert.assertEquals("com.other.pkg", pins.get(1));
    }

    @Test
    public void testMalformedJsonFailsWithoutCorruptingState() {
        pm.setScene("dusk");
        boolean ok = pm.importFromWebJson("{unquoted_broken_json: true");
        Assert.assertFalse(ok);
        Assert.assertEquals("dusk", pm.getScene());
    }

    @Test
    public void testMidwayTypeMismatchFailsAtomicallyWithoutApplyingEarlierFields() {
        pm.setScene("kakashi");
        pm.setVolume(0.8f);

        // JSON has a valid 'scene' change first, then an invalid typed field 'audio':{} which throws JSONException
        String json = "{\"scene\":\"team7\", \"audio\":{}}";
        boolean ok = pm.importFromWebJson(json);

        Assert.assertFalse("Import must fail when field type cannot be converted", ok);
        // Assert pre-existing values are 100% intact (atomicity)
        Assert.assertEquals("kakashi", pm.getScene());
        Assert.assertEquals(0.8f, pm.getVolume(), 0.001f);
    }
}
