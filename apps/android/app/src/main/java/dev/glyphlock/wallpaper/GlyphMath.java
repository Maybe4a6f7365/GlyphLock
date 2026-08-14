package dev.glyphlock.wallpaper;

final class GlyphMath {
    private GlyphMath() {}

    static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    static float mix(float a, float b, float t) {
        return a + (b - a) * t;
    }

    static float smooth(float value) {
        float x = clamp01(value);
        return x * x * (3f - 2f * x);
    }

    static float easeOutCubic(float value) {
        float x = clamp01(value);
        float inv = 1f - x;
        return 1f - inv * inv * inv;
    }

    static float staggeredProgress(float value, float delay, float duration) {
        return smooth((value - delay) / Math.max(0.001f, duration));
    }

    static float approach(float current, float target, float factor) {
        return mix(current, target, clamp01(factor));
    }

    static float hash(float x, float y, float seed) {
        double n = Math.sin(x * 127.1d + y * 311.7d + seed * 74.7d) * 43758.5453123d;
        return (float) (n - Math.floor(n));
    }
}
