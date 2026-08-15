package dev.glyphlock.wallpaper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class SpatialGlyphMatcherTest {
    @Test
    public void assignmentsAreUniqueAndLocal() {
        float[] sx = { 10, 20, 30, 40, 50, 60 };
        float[] sy = { 50, 50, 50, 50, 50, 50 };
        float[] alpha = { .5f, .6f, .7f, .8f, .9f, 1f };
        float[] tx = { 12, 31, 58 };
        float[] ty = { 50, 50, 50 };
        boolean[] text = { true, true, true };

        int[] result = SpatialGlyphMatcher.match(sx, sy, alpha, tx, ty, text, 100, 200);
        Set<Integer> unique = new HashSet<>();
        for (int index : result) {
            assertTrue(index >= 0);
            unique.add(index);
        }
        assertEquals(result.length, unique.size());
        assertTrue(Math.abs(sx[result[0]] - tx[0]) <= 12f);
        assertTrue(Math.abs(sx[result[2]] - tx[2]) <= 12f);
    }

    @Test
    public void orderedTextTargetsAvoidCrossing() {
        float[] sx = { 80, 20, 60, 40 };
        float[] sy = { 100, 100, 100, 100 };
        float[] alpha = { 1, 1, 1, 1 };
        float[] tx = { 25, 45, 65, 85 };
        float[] ty = { 100, 100, 100, 100 };
        boolean[] text = { true, true, true, true };

        int[] result = SpatialGlyphMatcher.match(sx, sy, alpha, tx, ty, text, 100, 200);
        float previous = -Float.MAX_VALUE;
        for (int index : result) {
            assertTrue(sx[index] >= previous);
            previous = sx[index];
        }
    }
}
