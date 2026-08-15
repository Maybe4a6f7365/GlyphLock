package dev.glyphlock.wallpaper;

/** Explicit renderer budgets. Prototype 0 defaults to LUX for visual review. */
enum RenderQuality {
    ECO(540, 12, 520, 980, 0.64f),
    BALANCED(720, 9, 920, 2050, 0.88f),
    LUX(960, 7, 1450, 3400, 1.18f);

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
        int divisor = this == LUX ? 138 : this == BALANCED ? 102 : 76;
        return Math.max(minimumGlyphStep, Math.round(width / (float) divisor));
    }

    int particleCountFor(int desired) {
        return Math.min(maximumParticles, Math.max(minimumParticles, desired));
    }
}
