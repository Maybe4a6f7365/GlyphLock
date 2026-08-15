package dev.glyphlock.wallpaper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class ThemeCatalogTest {
    @Test
    public void aestheticPackContainsFourteenDistinctThemes() {
        assertEquals(14, DemoCatalog.Theme.values().length);
        assertNotEquals(
                DemoCatalog.Theme.EVENT_HORIZON.atmosphereColor,
                DemoCatalog.Theme.CRYO_VAULT.atmosphereColor
        );
    }
}
