package dev.glyphlock.wallpaper;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/** Minimal local control surface for installing and evaluating Prototype 0. */
public final class MainActivity extends Activity {
    private TextView selectionLabel;
    private ImageView hero;
    private Switch autoRevealSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        setContentView(buildContent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshSelection();
    }

    private View buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(4, 6, 7));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(28), dp(24), dp(42));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        TextView overline = text("PRIVATE VISUAL RESEARCH BUILD", 11, Color.rgb(159, 221, 238));
        overline.setLetterSpacing(0.18f);
        root.addView(overline);

        TextView title = text("GLYPHLOCK", 34, Color.rgb(239, 245, 247));
        title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        title.setLetterSpacing(0.10f);
        LinearLayout.LayoutParams titleParams = verticalParams(dp(4), 0);
        root.addView(title, titleParams);

        TextView subtitle = text(
                "Eight procedural glyph systems now move continuously, wake with a ripple, and transform into useful lock-screen information without becoming notification cards.",
                15,
                Color.argb(168, 226, 234, 238)
        );
        subtitle.setLineSpacing(0f, 1.24f);
        root.addView(subtitle, verticalParams(dp(10), dp(24)));

        FrameLayout heroFrame = new FrameLayout(this);
        heroFrame.setBackground(rounded(Color.BLACK, Color.argb(38, 255, 255, 255), dp(24), 1));
        heroFrame.setClipToOutline(true);
        hero = new ImageView(this);
        hero.setScaleType(ImageView.ScaleType.CENTER_CROP);
        hero.setImageResource(DemoPreferences.theme(this).maskResource);
        hero.setAlpha(0.78f);
        heroFrame.addView(hero, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        LinearLayout heroCaption = new LinearLayout(this);
        heroCaption.setOrientation(LinearLayout.VERTICAL);
        heroCaption.setPadding(dp(18), dp(18), dp(18), dp(18));
        heroCaption.setGravity(Gravity.BOTTOM);
        FrameLayout.LayoutParams captionParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        heroFrame.addView(heroCaption, captionParams);

        TextView localBadge = text("8 ART SYSTEMS · 5 MOTION GRAMMARS · LOCAL ONLY", 10, Color.rgb(159, 221, 238));
        localBadge.setLetterSpacing(0.10f);
        heroCaption.addView(localBadge);
        selectionLabel = text("", 19, Color.WHITE);
        selectionLabel.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        heroCaption.addView(selectionLabel, verticalParams(dp(5), 0));

        LinearLayout.LayoutParams heroParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(330)
        );
        heroParams.bottomMargin = dp(22);
        root.addView(heroFrame, heroParams);

        Button preview = primaryButton("OPEN INTERACTIVE PREVIEW");
        preview.setOnClickListener(v -> startActivity(new Intent(this, PreviewActivity.class)));
        root.addView(preview, verticalParams(0, dp(10)));

        Button install = secondaryButton("SET AS LIVE WALLPAPER");
        install.setOnClickListener(v -> openWallpaperPicker());
        root.addView(install, verticalParams(0, dp(22)));

        TextView selectionTitle = sectionLabel("DEMO CONFIGURATION");
        root.addView(selectionTitle, verticalParams(0, dp(10)));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setWeightSum(2f);
        Button artwork = secondaryButton("NEXT ARTWORK");
        Button event = secondaryButton("NEXT EVENT");
        artwork.setOnClickListener(v -> {
            DemoCatalog.Theme next = DemoPreferences.theme(this).next();
            DemoPreferences.setTheme(this, next);
            refreshSelection();
        });
        event.setOnClickListener(v -> {
            DemoPreferences.setEventIndex(this, DemoPreferences.eventIndex(this) + 1);
            refreshSelection();
        });
        row.addView(artwork, weightedButtonParams(1f, dp(5), 0));
        row.addView(event, weightedButtonParams(1f, dp(5), 0));
        root.addView(row, verticalParams(0, dp(12)));

        autoRevealSwitch = new Switch(this);
        autoRevealSwitch.setText("Transform automatically when the wallpaper becomes visible");
        autoRevealSwitch.setTextColor(Color.argb(210, 230, 237, 240));
        autoRevealSwitch.setTextSize(14f);
        autoRevealSwitch.setChecked(DemoPreferences.autoReveal(this));
        autoRevealSwitch.setPadding(0, dp(6), 0, dp(6));
        autoRevealSwitch.setOnCheckedChangeListener((buttonView, checked) -> DemoPreferences.setAutoReveal(this, checked));
        root.addView(autoRevealSwitch, verticalParams(0, dp(22)));

        TextView controlsTitle = sectionLabel("INTERACTION");
        root.addView(controlsTitle, verticalParams(0, dp(8)));
        TextView controls = text(
                "The field moves continuously. Tap the lower command zone to reveal. Hold to simulate voice against the active event. Swipe left or right between fixtures. Swipe upward to dissolve the information back into the artwork.",
                14,
                Color.argb(158, 226, 234, 238)
        );
        controls.setLineSpacing(0f, 1.28f);
        root.addView(controls);

        TextView boundary = text(
                "HERMES BOUNDARY\nThere is deliberately no agent client, connector, account permission, microphone permission, or internet permission in this build. The rendering and interaction contract can be evaluated in isolation.",
                12,
                Color.argb(126, 226, 234, 238)
        );
        boundary.setTypeface(Typeface.MONOSPACE);
        boundary.setLineSpacing(0f, 1.28f);
        root.addView(boundary, verticalParams(dp(28), 0));

        refreshSelection();
        return scroll;
    }

    private void openWallpaperPicker() {
        ComponentName component = new ComponentName(this, GlyphWallpaperService.class);
        Intent direct = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
        direct.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component);
        try {
            startActivity(direct);
        } catch (ActivityNotFoundException error) {
            try {
                startActivity(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER));
            } catch (ActivityNotFoundException unavailable) {
                Toast.makeText(this, "This device does not expose a live-wallpaper picker.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void refreshSelection() {
        if (selectionLabel == null || hero == null) return;
        DemoCatalog.Theme theme = DemoPreferences.theme(this);
        DemoCatalog.Event event = DemoCatalog.eventAt(DemoPreferences.eventIndex(this));
        selectionLabel.setText(theme.label.toUpperCase() + "  /  " + event.title);
        hero.setImageResource(theme.maskResource);
        if (autoRevealSwitch != null) autoRevealSwitch.setChecked(DemoPreferences.autoReveal(this));
    }

    private TextView text(String value, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        return view;
    }

    private TextView sectionLabel(String value) {
        TextView view = text(value, 11, Color.argb(145, 226, 234, 238));
        view.setLetterSpacing(0.15f);
        view.setTypeface(Typeface.MONOSPACE);
        return view;
    }

    private Button primaryButton(String value) {
        Button button = baseButton(value);
        button.setTextColor(Color.rgb(2, 7, 9));
        button.setBackground(rounded(Color.rgb(218, 238, 245), Color.TRANSPARENT, dp(14), 0));
        return button;
    }

    private Button secondaryButton(String value) {
        Button button = baseButton(value);
        button.setTextColor(Color.argb(224, 235, 241, 244));
        button.setBackground(rounded(Color.argb(20, 255, 255, 255), Color.argb(42, 255, 255, 255), dp(14), 1));
        return button;
    }

    private Button baseButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(12f);
        button.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        button.setLetterSpacing(0.08f);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(54));
        button.setPadding(dp(12), 0, dp(12), 0);
        return button;
    }

    private GradientDrawable rounded(int fill, int stroke, int radius, int strokeWidthDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(radius);
        if (strokeWidthDp > 0) drawable.setStroke(dp(strokeWidthDp), stroke);
        return drawable;
    }

    private LinearLayout.LayoutParams verticalParams(int topMargin, int bottomMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = topMargin;
        params.bottomMargin = bottomMargin;
        return params;
    }

    private LinearLayout.LayoutParams weightedButtonParams(float weight, int startMargin, int endMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight);
        params.setMarginStart(startMargin);
        params.setMarginEnd(endMargin);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
