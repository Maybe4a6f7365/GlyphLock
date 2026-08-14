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
        final boolean listening;
        final boolean needsAnimation;
        final State state;

        Frame(float revealProgress, float resultProgress, boolean listening, boolean needsAnimation, State state) {
            this.revealProgress = revealProgress;
            this.resultProgress = resultProgress;
            this.listening = listening;
            this.needsAnimation = needsAnimation;
            this.state = state;
        }
    }

    private State state = State.AMBIENT;
    private long stateStartedAtMs = 0L;

    State state() {
        return state;
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
        boolean animate = false;

        switch (state) {
            case AMBIENT:
                break;
            case REVEALING: {
                float raw = (nowMs - stateStartedAtMs) / 1750f;
                reveal = GlyphMath.clamp01(raw);
                animate = reveal < 1f;
                if (!animate) state = State.FOCUSED;
                break;
            }
            case FOCUSED:
                reveal = 1f;
                break;
            case LISTENING: {
                reveal = 1f;
                listening = true;
                animate = true;
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
                animate = result < 1f;
                if (!animate) state = State.RESULT;
                break;
            }
            case RESULT:
                reveal = 1f;
                result = 1f;
                break;
            case COLLAPSING: {
                reveal = 1f - GlyphMath.clamp01((nowMs - stateStartedAtMs) / 1250f);
                animate = reveal > 0f;
                if (!animate) state = State.AMBIENT;
                break;
            }
        }

        return new Frame(
                GlyphMath.smooth(reveal),
                GlyphMath.smooth(result),
                listening,
                animate || listening,
                state
        );
    }
}
