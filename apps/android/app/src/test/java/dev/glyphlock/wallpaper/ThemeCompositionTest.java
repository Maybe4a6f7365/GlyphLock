package dev.glyphlock.wallpaper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;
import org.junit.Test;

public class ThemeCompositionTest {
    @Test
    public void catalogHasSixteenWallpaperSystems() {
        assertEquals(16, DemoCatalog.Theme.values().length);
    }

    @Test
    public void everyThemeHasAUsableSemanticRegion() {
        for (DemoCatalog.Theme theme : DemoCatalog.Theme.values()) {
            assertTrue(theme.semanticY > 0.40f && theme.semanticY < 0.70f);
            assertTrue(theme.semanticWidth >= 0.55f && theme.semanticWidth <= 0.75f);
        }
    }

    @Test
    public void allCompositionGrammarsAreRepresented() {
        EnumSet<DemoCatalog.CompositionStyle> styles = EnumSet.noneOf(DemoCatalog.CompositionStyle.class);
        for (DemoCatalog.Theme theme : DemoCatalog.Theme.values()) styles.add(theme.compositionStyle);
        assertEquals(EnumSet.allOf(DemoCatalog.CompositionStyle.class), styles);
    }

    @Test
    public void waveAndFoldMotionAreCovered() {
        EnumSet<DemoCatalog.MotionStyle> styles = EnumSet.noneOf(DemoCatalog.MotionStyle.class);
        for (DemoCatalog.Theme theme : DemoCatalog.Theme.values()) styles.add(theme.motionStyle);
        assertTrue(styles.contains(DemoCatalog.MotionStyle.WAVE));
        assertTrue(styles.contains(DemoCatalog.MotionStyle.FOLD));
    }
}
