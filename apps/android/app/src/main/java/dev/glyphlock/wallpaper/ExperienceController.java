package dev.glyphlock.wallpaper;

/** Small deterministic state machine shared by the preview and live wallpaper. */
final class ExperienceController {
    enum State {
        AMBIENT,
        REVEALING,
        FOCUSED,
        LISTENING,
        RESULT_TRANSITION,
        RESULT,
        COLLAPSING
    }

    static final class Frame {
        final float revealProgress;
        final float resultProgress;
        final float wakeProgress;
        final boolean listening;
        final boolean needsAnimation;
        final int frameDelayMs;
        final State state;

        Frame(
                float revealProgress,
                float resultProgress,
                float wakeProgress,
                boolean listening,
                boolean needsAnimation,
                int frameDelayMs,
                State state
        ) {
            this.revealProgress = revealProgress;
            this.resultProgress = resultProgress;
            this.wakeProgress = wakeProgress;
            this.listening = listening;
            this.needsAnimation = needsAnimation;
            this.frameDelayMs = frameDelayMs;
            this.state = state;
        }
    }

    private State state = State.AMBIENT;
    private long stateStartedAtMs = 0L;
    private long wakeStartedAtMs = 0L;

    State state() {
        return state;
    }

    void wake(long nowMs) {
        wakeStartedAtMs = nowMs;
    }

    void reveal(long nowMs) {
        state = State.REVEALING;
        stateStartedAtMs = nowMs;
    }

    void collapse(long nowMs) {
        if (state == State.AMBIENT) return;
        state = State.COLLAPSING;
        stateStartedAtMs = nowMs;
    }

    void listen(long nowMs) {
        state = State.LISTENING;
        stateStartedAtMs = nowMs;
    }

    void resetAmbient() {
        state = State.AMBIENT;
        stateStartedAtMs = 0L;
    }

    Frame frame(long nowMs) {
        float reveal = 0f;
        float result = 0f;
        boolean listening = false;
        boolean transition = false;

        switch (state) {
            case AMBIENT:
                break;
            case REVEALING: {
                float raw = (nowMs - stateStartedAtMs) / 1850f;
                reveal = GlyphMath.clamp01(raw);
                transition = reveal < 1f;
                if (!transition) state = State.FOCUSED;
                break;
            }
            case FOCUSED:
                reveal = 1f;
                break;
            case LISTENING: {
                reveal = 1f;
                listening = true;
                transition = true;
                if (nowMs - stateStartedAtMs >= 1650L) {
                    state = State.RESULT_TRANSITION;
                    stateStartedAtMs = nowMs;
                    listening = false;
                }
                break;
            }
            case RESULT_TRANSITION: {
                reveal = 1f;
                result = GlyphMath.clamp01((nowMs - stateStartedAtMs) / 1000f);
                transition = result < 1f;
                if (!transition) state = State.RESULT;
                break;
            }
            case RESULT:
                reveal = 1f;
                result = 1f;
                break;
            case COLLAPSING: {
                reveal = 1f - GlyphMath.clamp01((nowMs - stateStartedAtMs) / 1350f);
                transition = reveal > 0f;
                if (!transition) state = State.AMBIENT;
                break;
            }
        }

        float wake = wakeStartedAtMs <= 0L
                ? 1f
                : GlyphMath.clamp01((nowMs - wakeStartedAtMs) / 2200f);
        boolean wakeAnimating = wake < 1f;
        boolean animate = transition || listening || wakeAnimating || state == State.AMBIENT || state == State.FOCUSED || state == State.RESULT;
        int delay = transition || listening || wakeAnimating ? 16 : 33;

        return new Frame(
                GlyphMath.smooth(reveal),
                GlyphMath.smooth(result),
                GlyphMath.smooth(wake),
                listening,
                animate,
                delay,
                state
        );
    }
}
