package dev.glyphlock.wallpaper;

/** Explicit renderer budgets. Prototype 0 defaults to LUX for design review. */
enum RenderQuality {
    ECO(540, 12, 420, 760, 0.60f),
    BALANCED(720, 9, 680, 1320, 0.82f),
    LUX(900, 7, 980, 2300, 1.00f);

    final int maxRenderWidth;
    final int minimumGlyphStep;
    final int minimumParticles;
    final int maximumParticles;
    final float liveDensity;

    RenderQuality(
            int maxRenderWidth,
            int minimumGlyphStep,
            int minimumParticles,
            int maximumParticles,
            float liveDensity
    ) {
        this.maxRenderWidth = maxRenderWidth;
        this.minimumGlyphStep = minimumGlyphStep;
        this.minimumParticles = minimumParticles;
        this.maximumParticles = maximumParticles;
        this.liveDensity = liveDensity;
    }

    int glyphStepFor(int width) {
        int divisor = this == LUX ? 122 : this == BALANCED ? 94 : 72;
        return Math.max(minimumGlyphStep, Math.round(width / (float) divisor));
    }

    int particleCountFor(int desired) {
        return Math.min(maximumParticles, Math.max(minimumParticles, desired));
    }
}
