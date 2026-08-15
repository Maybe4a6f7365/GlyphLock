package dev.glyphlock.wallpaper;

/** Explicit renderer budgets. Live surfaces default to ECO to protect launcher frame time. */
enum RenderQuality {
    ECO(540, 12, 1200, 3600, 0.72f),
    BALANCED(720, 9, 1800, 5000, 0.98f),
    LUX(900, 7, 2400, 6500, 1.16f);

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
