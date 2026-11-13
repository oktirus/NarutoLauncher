package ua.konoha.tv.nativepreview.panels;

import org.junit.Assert;
import org.junit.Test;

public class AdaptiveGridHelperTest {

    @Test public void compactHighDensityViewportFitsEvenBelowPreferredMinimums() {
        for (int width : new int[]{180, 240, 294, 390, 768, 1366, 1920}) {
            AdaptiveGridHelper.LayoutResult result = AdaptiveGridHelper.compute(width, 360, 480, 18, 2, 8);
            Assert.assertTrue(result.columnCount * (result.cardWidthPx + 36) <= width);
            Assert.assertTrue(result.cardWidthPx > 0);
        }
    }

    @Test
    public void testComputeFallbackOnNonPositiveWidth() {
        AdaptiveGridHelper.LayoutResult res = AdaptiveGridHelper.compute(0, 150, 250, 10, 2, 6);
        Assert.assertEquals(2, res.columnCount);
        Assert.assertEquals(150, res.cardWidthPx);

        res = AdaptiveGridHelper.compute(-50, 180, 280, 8, 3, 5);
        Assert.assertEquals(3, res.columnCount);
        Assert.assertEquals(180, res.cardWidthPx);
    }

    @Test
    public void testComputeScenesGridAcrossResolutions() {
        // Scenes: min 180px, max 320px, margin 6px, minCols 1, maxCols 6
        // 1. Mobile compact (390px)
        AdaptiveGridHelper.LayoutResult r390 = AdaptiveGridHelper.compute(390, 180, 320, 6, 1, 6);
        // itemSlotMin = 180 + 12 = 192. 390 / 192 = 2 cols.
        // totalMargins = 2 * 12 = 24. cardWidth = (390 - 24) / 2 = 183.
        Assert.assertEquals(2, r390.columnCount);
        Assert.assertEquals(183, r390.cardWidthPx);

        // 2. Small TV / 720p (1280px)
        AdaptiveGridHelper.LayoutResult r1280 = AdaptiveGridHelper.compute(1280, 180, 320, 6, 1, 6);
        // 1280 / 192 = 6 cols. totalMargins = 6 * 12 = 72. (1280 - 72) / 6 = 201px.
        Assert.assertEquals(6, r1280.columnCount);
        Assert.assertEquals(201, r1280.cardWidthPx);

        // 3. Full HD (1920px)
        AdaptiveGridHelper.LayoutResult r1920 = AdaptiveGridHelper.compute(1920, 180, 320, 6, 1, 6);
        // maxCols clamped to 6. totalMargins = 72. (1920 - 72) / 6 = 308px <= 320px.
        Assert.assertEquals(6, r1920.columnCount);
        Assert.assertEquals(308, r1920.cardWidthPx);
    }

    @Test
    public void testComputeTechniquesGridAcrossResolutions() {
        // Techniques: min 240px, max 420px, margin 8px, minCols 1, maxCols 4
        // itemSlotMin = 240 + 16 = 256.
        // 1. 480px width
        AdaptiveGridHelper.LayoutResult r480 = AdaptiveGridHelper.compute(480, 240, 420, 8, 1, 4);
        // 480 / 256 = 1 col. (480 - 16) / 1 = 464 -> clamped to max 420.
        Assert.assertEquals(1, r480.columnCount);
        Assert.assertEquals(420, r480.cardWidthPx);

        // 2. 960px width
        AdaptiveGridHelper.LayoutResult r960 = AdaptiveGridHelper.compute(960, 240, 420, 8, 1, 4);
        // 960 / 256 = 3 cols. totalMargins = 3 * 16 = 48. (960 - 48) / 3 = 304px.
        Assert.assertEquals(3, r960.columnCount);
        Assert.assertEquals(304, r960.cardWidthPx);

        // 3. 1920px width
        AdaptiveGridHelper.LayoutResult r1920 = AdaptiveGridHelper.compute(1920, 240, 420, 8, 1, 4);
        // 1920 / 256 = 7 -> clamped to max 4 cols. totalMargins = 4 * 16 = 64. (1920 - 64) / 4 = 464 -> clamped to 420.
        Assert.assertEquals(4, r1920.columnCount);
        Assert.assertEquals(420, r1920.cardWidthPx);
    }

    @Test
    public void testComputeLibraryGridAcrossResolutions() {
        // Library: min 110px, max 160px, margin 6px, minCols 2, maxCols 8
        // itemSlotMin = 110 + 12 = 122.
        // 1. 390px width
        AdaptiveGridHelper.LayoutResult r390 = AdaptiveGridHelper.compute(390, 110, 160, 6, 2, 8);
        // 390 / 122 = 3 cols. totalMargins = 36. (390 - 36) / 3 = 118px.
        Assert.assertEquals(3, r390.columnCount);
        Assert.assertEquals(118, r390.cardWidthPx);

        // 2. 1280px width
        AdaptiveGridHelper.LayoutResult r1280 = AdaptiveGridHelper.compute(1280, 110, 160, 6, 2, 8);
        // 1280 / 122 = 10 -> max 8 cols. totalMargins = 96. (1280 - 96) / 8 = 148px.
        Assert.assertEquals(8, r1280.columnCount);
        Assert.assertEquals(148, r1280.cardWidthPx);
    }
}
