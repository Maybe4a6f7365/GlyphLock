package dev.glyphlock.wallpaper;

import android.content.Context;
import android.content.SharedPreferences;

final class DemoPreferences {
    private static final String FILE = "glyphlock_demo";
    private static final String KEY_THEME = "theme";
    private static final String KEY_EVENT = "event";
    private static final String KEY_AUTO_REVEAL = "auto_reveal";

    private DemoPreferences() {}

    static DemoCatalog.Theme theme(Context context) {
        String raw = prefs(context).getString(KEY_THEME, DemoCatalog.Theme.SENTINEL.name());
        try {
            return DemoCatalog.Theme.valueOf(raw);
        } catch (IllegalArgumentException ignored) {
            return DemoCatalog.Theme.SENTINEL;
        }
    }

    static void setTheme(Context context, DemoCatalog.Theme theme) {
        prefs(context).edit().putString(KEY_THEME, theme.name()).apply();
    }

    static int eventIndex(Context context) {
        return Math.floorMod(prefs(context).getInt(KEY_EVENT, 0), DemoCatalog.EVENTS.size());
    }

    static void setEventIndex(Context context, int index) {
        prefs(context).edit().putInt(KEY_EVENT, Math.floorMod(index, DemoCatalog.EVENTS.size())).apply();
    }

    static boolean autoReveal(Context context) {
        return prefs(context).getBoolean(KEY_AUTO_REVEAL, true);
    }

    static void setAutoReveal(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_AUTO_REVEAL, enabled).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
}
