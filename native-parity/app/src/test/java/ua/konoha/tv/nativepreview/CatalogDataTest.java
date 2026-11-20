package ua.konoha.tv.nativepreview;

import org.junit.Assert;
import org.junit.Test;
import ua.konoha.tv.nativepreview.model.CatalogData;
import ua.konoha.tv.nativepreview.model.Scene;
import ua.konoha.tv.nativepreview.model.Technique;

import java.util.HashSet;
import java.util.Set;

public class CatalogDataTest {

    @Test
    public void testEighteenScenes() {
        Assert.assertEquals(18, CatalogData.SCENES.size());
        Set<String> sceneIds = new HashSet<>();
        for (Scene s : CatalogData.SCENES) {
            sceneIds.add(s.getId());
            Assert.assertNotNull(s.getName());
            Assert.assertNotNull(s.getImageAsset());
            Assert.assertNotNull(s.getThumbAsset());
            Assert.assertNotNull("Кожна сцена повинна мати цитату", CatalogData.QUOTES.get(s.getId()));
        }
        Assert.assertEquals(18, sceneIds.size());
        Assert.assertTrue(sceneIds.contains("dusk"));
        Assert.assertTrue(sceneIds.contains("team7"));
        Assert.assertTrue(sceneIds.contains("minato"));
        Assert.assertTrue(sceneIds.contains("sai"));
        Assert.assertFalse("Застаріла сцена нічного тренування не повинна бути в активному каталозі", sceneIds.contains("night"));
    }

    @Test
    public void testTenTechniques() {
        Assert.assertEquals(10, CatalogData.TECHNIQUES.size());
        Assert.assertEquals("gamabunta", CatalogData.TECHNIQUES.get(0).getId());
        Assert.assertEquals("rasengan", CatalogData.TECHNIQUES.get(1).getId());
        Assert.assertEquals("rasenshuriken", CatalogData.TECHNIQUES.get(2).getId());
        Set<String> techIds = new HashSet<>();
        for (Technique t : CatalogData.TECHNIQUES) {
            techIds.add(t.getId());
            Assert.assertNotNull(t.getName());
            Assert.assertNotNull(t.getCue());
            Assert.assertNotNull(t.getDescription());
        }
        String[] required = new String[]{
                "rasengan", "chidori", "sharingan", "kurama", "summoning",
                "hiraishin", "fireball", "amaterasu", "rasenshuriken", "gamabunta"
        };
        for (String req : required) {
            Assert.assertTrue("Техніка " + req + " має бути присутня", techIds.contains(req));
        }
    }

    @Test
    public void testMusicTracksAndHighlightSounds() {
        Assert.assertEquals(3, CatalogData.MUSIC_TRACKS.size());
        Assert.assertEquals(6, CatalogData.HIGHLIGHT_SOUNDS.size());
    }

    @Test
    public void testAppAliasMatching() {
        Assert.assertEquals("youtube", CatalogData.matchKind("com.google.android.youtube.tv", "YouTube on TV"));
        Assert.assertEquals("smarttube", CatalogData.matchKind("com.teamsmart.videomanager.tv", "SmartTube"));
        Assert.assertEquals("lampa", CatalogData.matchKind("top.rootintech.lampa", "Lampa"));
        Assert.assertEquals("uakino", CatalogData.matchKind("ua.kino.client", "UAKino"));
        Assert.assertEquals("tvbro", CatalogData.matchKind("com.phlox.tvwebbrowser", "Browser"));
        Assert.assertEquals("kyivstar", CatalogData.matchKind("ua.kyivstar.tv", "Київстар ТБ"));
        Assert.assertEquals("sweettv", CatalogData.matchKind("tv.sweet.player", "SWEET.TV"));
        Assert.assertEquals("primevideo", CatalogData.matchKind("com.amazon.amazonvideo.livingroom", "Prime Video"));
    }
}
