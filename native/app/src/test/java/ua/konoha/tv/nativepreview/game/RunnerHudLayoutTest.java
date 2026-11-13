package ua.konoha.tv.nativepreview.game;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RunnerHudLayoutTest {

    private RunnerHudLayout hud;

    @Before
    public void setUp() {
        hud = new RunnerHudLayout();
    }

    @Test
    public void testPauseCenterResolvesToPauseAt1366() {
        int w = 1366;
        int h = 768;
        hud.update(w, h);

        // Exit bounds: 1266..1346
        Assert.assertEquals(1266f, hud.exitBounds.left, 0.01f);
        Assert.assertEquals(1346f, hud.exitBounds.right, 0.01f);

        // Pause bounds: 1176..1256, center = 1216
        Assert.assertEquals(1176f, hud.pauseBounds.left, 0.01f);
        Assert.assertEquals(1256f, hud.pauseBounds.right, 0.01f);
        float pauseCenter = hud.pauseBounds.centerX();
        Assert.assertEquals(1216f, pauseCenter, 0.01f);

        // Action resolution at pause center
        RunnerHudLayout.Action action = hud.resolveAction(pauseCenter, 40f, h);
        Assert.assertEquals(RunnerHudLayout.Action.PAUSE_TOGGLE, action);

        // Evidence: under old buggy proportional threshold (0.88 * 1366 = 1202.08),
        // pause center (1216) was greater than 1202.08, causing tap on Pause to wrongly EXIT!
        float oldProportionalThreshold = w * 0.88f;
        boolean oldBuggyWouldExit = pauseCenter > oldProportionalThreshold;
        Assert.assertTrue("Proving old proportional logic triggered exit on pause tap", oldBuggyWouldExit);
    }

    @Test
    public void testPauseCenterResolvesToPauseAt1920() {
        int w = 1920;
        int h = 1080;
        hud.update(w, h);

        // Exit bounds: 1820..1900
        Assert.assertEquals(1820f, hud.exitBounds.left, 0.01f);
        Assert.assertEquals(1900f, hud.exitBounds.right, 0.01f);

        // Pause bounds: 1730..1810, center = 1770
        Assert.assertEquals(1730f, hud.pauseBounds.left, 0.01f);
        Assert.assertEquals(1810f, hud.pauseBounds.right, 0.01f);
        float pauseCenter = hud.pauseBounds.centerX();
        Assert.assertEquals(1770f, pauseCenter, 0.01f);

        // Action resolution at pause center
        RunnerHudLayout.Action action = hud.resolveAction(pauseCenter, 40f, h);
        Assert.assertEquals(RunnerHudLayout.Action.PAUSE_TOGGLE, action);

        // Evidence: old proportional 0.88 * 1920 = 1689.6; pauseCenter 1770 > 1689.6 was buggy exit
        Assert.assertTrue(pauseCenter > w * 0.88f);
    }

    @Test
    public void testBoundariesAndGapsAt963() {
        int w = 963;
        int h = 540;
        hud.update(w, h);

        // Exit boundary hit
        Assert.assertEquals(RunnerHudLayout.Action.EXIT, hud.resolveAction(hud.exitBounds.left, 40f, h));
        Assert.assertEquals(RunnerHudLayout.Action.EXIT, hud.resolveAction(hud.exitBounds.right, 40f, h));

        // Pause boundary hit
        Assert.assertEquals(RunnerHudLayout.Action.PAUSE_TOGGLE, hud.resolveAction(hud.pauseBounds.left, 40f, h));
        Assert.assertEquals(RunnerHudLayout.Action.PAUSE_TOGGLE, hud.resolveAction(hud.pauseBounds.right, 40f, h));

        // Gap between Pause and Exit: resolves to NONE to prevent accidental jump
        float gapX = (hud.pauseBounds.right + hud.exitBounds.left) / 2f;
        RunnerHudLayout.Action gapAction = hud.resolveAction(gapX, 40f, h);
        Assert.assertEquals(RunnerHudLayout.Action.NONE, gapAction);
    }

    @Test
    public void testCompactVsWideTransitionAndNoOverlapAcrossWidths() {
        int[] testWidths = new int[]{390, 480, 599, 600, 640, 665, 666, 719, 720, 963, 1366, 1920};
        for (int w : testWidths) {
            hud.update(w, 540);
            Assert.assertTrue("Exit width must be positive at w=" + w, hud.exitBounds.width() > 0);
            Assert.assertTrue("Pause width must be positive at w=" + w, hud.pauseBounds.width() > 0);
            Assert.assertTrue("Hero width must be positive at w=" + w, hud.heroBounds.width() > 0);
            Assert.assertTrue("Pause must be left of Exit at w=" + w, hud.pauseBounds.right < hud.exitBounds.left);
            Assert.assertTrue("Hero must be left of Pause at w=" + w, hud.heroBounds.right < hud.pauseBounds.left);

            if (w < 720) {
                Assert.assertTrue("w=" + w + " must be compact", hud.isCompact);
                Assert.assertTrue("Compact score must be in separate row below top buttons at w=" + w,
                        hud.scoreBounds.top >= hud.exitBounds.bottom);
            } else {
                Assert.assertFalse("w=" + w + " must be wide", hud.isCompact);
                Assert.assertTrue("Hero left must never overlap score right at w=" + w,
                        hud.heroBounds.left >= hud.scoreBounds.right);
            }
        }
    }

    @Test
    public void testOffViewAndZeroSizeInputsRejected() {
        hud.update(1366, 768);
        Assert.assertEquals(RunnerHudLayout.Action.NONE, hud.resolveAction(-10f, 40f, 1366, 768));
        Assert.assertEquals(RunnerHudLayout.Action.NONE, hud.resolveAction(1400f, 40f, 1366, 768));
        Assert.assertEquals(RunnerHudLayout.Action.NONE, hud.resolveAction(500f, -5f, 1366, 768));
        Assert.assertEquals(RunnerHudLayout.Action.NONE, hud.resolveAction(500f, 800f, 1366, 768));
        Assert.assertEquals(RunnerHudLayout.Action.NONE, hud.resolveAction(500f, 40f, 0, 768));
    }

    @Test
    public void testHudHitsNeverAccidentallyJumpOrCrouch() {
        hud.update(1366, 768);
        RunnerHudLayout.Action exitAct = hud.resolveAction(hud.exitBounds.centerX(), hud.exitBounds.centerY(), 768);
        Assert.assertEquals(RunnerHudLayout.Action.EXIT, exitAct);

        RunnerHudLayout.Action pauseAct = hud.resolveAction(hud.pauseBounds.centerX(), hud.pauseBounds.centerY(), 768);
        Assert.assertEquals(RunnerHudLayout.Action.PAUSE_TOGGLE, pauseAct);

        RunnerHudLayout.Action heroAct = hud.resolveAction(hud.heroBounds.centerX(), hud.heroBounds.centerY(), 768);
        Assert.assertEquals(RunnerHudLayout.Action.HERO_TOGGLE, heroAct);

        // Lower screen gameplay touch is crouch
        RunnerHudLayout.Action crouchAct = hud.resolveAction(300f, 600f, 768);
        Assert.assertEquals(RunnerHudLayout.Action.CROUCH, crouchAct);

        // Upper screen gameplay touch is jump
        RunnerHudLayout.Action jumpAct = hud.resolveAction(300f, 300f, 768);
        Assert.assertEquals(RunnerHudLayout.Action.JUMP, jumpAct);
    }
}
