package dev.glyphlock.wallpaper;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic spatial matcher used to transport source artwork glyphs into semantic targets.
 *
 * The original prototype scanned every source for every target. Besides being quadratic, that
 * allowed adjacent letters to borrow glyphs from opposite sides of the wallpaper and produced
 * visible crossing. This matcher searches a local grid first and adds an ordering penalty for
 * consecutive text targets on the same baseline. The result is both faster and visually calmer.
 */
final class SpatialGlyphMatcher {
    private static final int COLUMNS = 18;
    private static final int ROWS = 36;
    private static final int MAX_LOCAL_RADIUS = 7;

    private SpatialGlyphMatcher() {}

    static int[] match(
            float[] sourceX,
            float[] sourceY,
            float[] sourceAlpha,
            float[] targetX,
            float[] targetY,
            boolean[] textTarget,
            int width,
            int height
    ) {
        int sourceCount = sourceX.length;
        int targetCount = targetX.length;
        if (sourceY.length != sourceCount || sourceAlpha.length != sourceCount) {
            throw new IllegalArgumentException("source arrays must have equal length");
        }
        if (targetY.length != targetCount || textTarget.length != targetCount) {
            throw new IllegalArgumentException("target arrays must have equal length");
        }

        @SuppressWarnings("unchecked")
        List<Integer>[] buckets = new List[COLUMNS * ROWS];
        for (int i = 0; i < buckets.length; i++) buckets[i] = new ArrayList<>();
        for (int i = 0; i < sourceCount; i++) {
            buckets[bucketIndex(sourceX[i], sourceY[i], width, height)].add(i);
        }

        boolean[] used = new boolean[sourceCount];
        int[] targetToSource = new int[targetCount];
        java.util.Arrays.fill(targetToSource, -1);

        float previousTargetX = Float.NaN;
        float previousTargetY = Float.NaN;
        float previousSourceX = Float.NaN;
        boolean previousWasText = false;

        for (int target = 0; target < targetCount; target++) {
            int centerColumn = clampColumn((int) (targetX[target] / Math.max(1f, width) * COLUMNS));
            int centerRow = clampRow((int) (targetY[target] / Math.max(1f, height) * ROWS));
            int best = -1;
            float bestScore = Float.MAX_VALUE;

            for (int radius = 0; radius <= MAX_LOCAL_RADIUS && best < 0; radius++) {
                int minColumn = Math.max(0, centerColumn - radius);
                int maxColumn = Math.min(COLUMNS - 1, centerColumn + radius);
                int minRow = Math.max(0, centerRow - radius);
                int maxRow = Math.min(ROWS - 1, centerRow + radius);

                for (int row = minRow; row <= maxRow; row++) {
                    for (int column = minColumn; column <= maxColumn; column++) {
                        if (radius > 0
                                && column > minColumn && column < maxColumn
                                && row > minRow && row < maxRow) {
                            continue;
                        }
                        for (int candidate : buckets[row * COLUMNS + column]) {
                            if (used[candidate]) continue;
                            float score = score(
                                    sourceX[candidate],
                                    sourceY[candidate],
                                    sourceAlpha[candidate],
                                    targetX[target],
                                    targetY[target],
                                    textTarget[target],
                                    width,
                                    height
                            );
                            if (textTarget[target]
                                    && previousWasText
                                    && Math.abs(targetY[target] - previousTargetY) < height * 0.018f
                                    && !Float.isNaN(previousSourceX)) {
                                boolean targetMovesRight = targetX[target] >= previousTargetX;
                                boolean sourceMovesRight = sourceX[candidate] >= previousSourceX;
                                if (targetMovesRight != sourceMovesRight) {
                                    score += width * width * 0.24f;
                                }
                            }
                            if (score < bestScore) {
                                bestScore = score;
                                best = candidate;
                            }
                        }
                    }
                }
            }

            if (best < 0) {
                for (int candidate = 0; candidate < sourceCount; candidate++) {
                    if (used[candidate]) continue;
                    float score = score(
                            sourceX[candidate],
                            sourceY[candidate],
                            sourceAlpha[candidate],
                            targetX[target],
                            targetY[target],
                            textTarget[target],
                            width,
                            height
                    );
                    if (score < bestScore) {
                        bestScore = score;
                        best = candidate;
                    }
                }
            }

            if (best < 0) break;
            used[best] = true;
            targetToSource[target] = best;
            previousTargetX = targetX[target];
            previousTargetY = targetY[target];
            previousSourceX = sourceX[best];
            previousWasText = textTarget[target];
        }
        return targetToSource;
    }

    private static float score(
            float sourceX,
            float sourceY,
            float sourceAlpha,
            float targetX,
            float targetY,
            boolean textTarget,
            int width,
            int height
    ) {
        float dx = sourceX - targetX;
        float verticalWeight = textTarget ? 1.34f : 1.08f;
        float dy = (sourceY - targetY) * verticalWeight;
        float alphaReward = sourceAlpha * width * width * (textTarget ? 0.020f : 0.009f);
        float travel = dx * dx + dy * dy;
        float diagonal = Math.max(1f, (float) Math.hypot(width, height));
        float normalizedTravel = (float) Math.sqrt(travel) / diagonal;
        float longTravelPenalty = normalizedTravel > (textTarget ? 0.38f : 0.27f)
                ? width * width * (normalizedTravel - 0.27f) * 0.70f
                : 0f;
        return travel - alphaReward + longTravelPenalty;
    }

    private static int bucketIndex(float x, float y, int width, int height) {
        int column = clampColumn((int) (x / Math.max(1f, width) * COLUMNS));
        int row = clampRow((int) (y / Math.max(1f, height) * ROWS));
        return row * COLUMNS + column;
    }

    private static int clampColumn(int value) {
        return Math.max(0, Math.min(COLUMNS - 1, value));
    }

    private static int clampRow(int value) {
        return Math.max(0, Math.min(ROWS - 1, value));
    }
}
