package dev.glyphlock.wallpaper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GlyphMathTest {
    @Test
    public void clampStaysWithinUnitInterval() {
        assertEquals(0f, GlyphMath.clamp01(-2f), 0f);
        assertEquals(0.5f, GlyphMath.clamp01(0.5f), 0f);
        assertEquals(1f, GlyphMath.clamp01(4f), 0f);
    }

    @Test
    public void staggeredProgressWaitsAndCompletes() {
        assertEquals(0f, GlyphMath.staggeredProgress(0.1f, 0.2f, 0.5f), 0f);
        assertTrue(GlyphMath.staggeredProgress(0.45f, 0.2f, 0.5f) > 0f);
        assertEquals(1f, GlyphMath.staggeredProgress(0.9f, 0.2f, 0.5f), 0f);
    }

    @Test
    public void approachMovesTowardTarget() {
        assertEquals(2.5f, GlyphMath.approach(0f, 10f, 0.25f), 0.0001f);
    }

    @Test
    public void distanceToRectIsZeroInsideAndEuclideanOutside() {
        assertEquals(0f, GlyphMath.distanceToRect(5f, 5f, 0f, 0f, 10f, 10f), 0f);
        assertEquals(5f, GlyphMath.distanceToRect(15f, 5f, 0f, 0f, 10f, 10f), 0.0001f);
    }

    @Test
    public void localInfluenceKeepsConfiguredIdentityFloor() {
        float near = GlyphMath.localInfluence(0f, 10f, .3f);
        float far = GlyphMath.localInfluence(100f, 10f, .3f);
        assertEquals(1f, near, 0.0001f);
        assertTrue(far >= .29f && far <= .31f);
    }

    @Test
    public void smoothIsMonotonicForRepresentativeSamples() {
        float previous = -1f;
        for (int i = 0; i <= 100; i++) {
            float current = GlyphMath.smooth(i / 100f);
            assertTrue(current >= previous);
            previous = current;
        }
    }
}
