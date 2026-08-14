package dev.glyphlock.wallpaper;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RenderQualityTest {
    @Test
    public void luxHasLargestBudgets() {
        assertTrue(RenderQuality.LUX.maxRenderWidth > RenderQuality.BALANCED.maxRenderWidth);
        assertTrue(RenderQuality.LUX.maximumParticles > RenderQuality.BALANCED.maximumParticles);
        assertTrue(RenderQuality.BALANCED.maximumParticles > RenderQuality.ECO.maximumParticles);
    }

    @Test
    public void particleBudgetClampsAtBothEnds() {
        assertTrue(RenderQuality.ECO.particleCountFor(1) >= RenderQuality.ECO.minimumParticles);
        assertTrue(RenderQuality.ECO.particleCountFor(10000) <= RenderQuality.ECO.maximumParticles);
    }
}
