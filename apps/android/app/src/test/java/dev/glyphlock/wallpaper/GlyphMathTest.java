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
    public void smoothIsMonotonicForRepresentativeSamples() {
        float previous = -1f;
        for (int i = 0; i <= 100; i++) {
            float current = GlyphMath.smooth(i / 100f);
            assertTrue(current >= previous);
            previous = current;
        }
    }
}
