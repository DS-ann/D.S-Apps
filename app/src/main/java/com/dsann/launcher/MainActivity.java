package com.dsann.launcher;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQUEST_PICK_WIDGET = 701;
    private static final int REQUEST_BIND_WIDGET = 702;
    private static final int REQUEST_CONFIGURE_WIDGET = 703;
    private static final int REQUEST_WALLPAPER_IMAGE = 704;
    private static final int HOST_ID = 1407;
    private static final String PREFS = "nova_launcher_prefs";
    private static final String KEY_WALLPAPER = "wallpaper_style";
    private static final String KEY_IMAGE_URI = "wallpaper_image_uri";
    private static final String KEY_WIDGETS = "widget_ids";

    private final int BG = Color.rgb(10, 13, 24);
    private final int MUTED = Color.rgb(157, 169, 194);
    private final Handler handler = new Handler();
    private PackageManager packageManager;
    private AppWidgetManager widgetManager;
    private AppWidgetHost widgetHost;
    private SharedPreferences prefs;
    private LinearLayout homeRoot;
    private LinearLayout widgetContainer;
    private GridView appGrid;
    private EditText searchBox;
    private TextView clockText;
    private TextView dateText;
    private List<ResolveInfo> allApps = new ArrayList<>();
    private List<ResolveInfo> visibleApps = new ArrayList<>();
    private AppAdapter adapter;
    private boolean customizationOpen = false;
    private boolean widgetListening = false;
    private final List<Integer> widgetIds = new ArrayList<>();
    private final Runnable clockUpdater = new Runnable() {
        @Override public void run() {
            updateClock();
            handler.postDelayed(this, 60000L);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        packageManager = getPackageManager();
        widgetManager = AppWidgetManager.getInstance(this);
        widgetHost = new AppWidgetHost(this, HOST_ID);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildHome();
        loadApps();
        updateClock();
        restoreWidgetIds();
        restoreWallpaper();
    }

    private void buildHome() {
        homeRoot = new LinearLayout(this);
        homeRoot.setOrientation(LinearLayout.VERTICAL);
        homeRoot.setPadding(dp(20), dp(18), dp(20), dp(8));
        applyWallpaperBackground();

        LinearLayout topRow = new LinearLayout(this);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text("NOVA", 13, Color.WHITE, true);
        brand.setLetterSpacing(0.18f);
        TextView status = text("LIGHTWEIGHT  •  FAST", 9, MUTED, true);
        status.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        topRow.addView(brand, new LinearLayout.LayoutParams(0, dp(28), 1f));
        topRow.addView(status, new LinearLayout.LayoutParams(-2, dp(28)));
        homeRoot.addView(topRow);
        installLongPress(topRow);
        installLongPress(brand);
        installLongPress(status);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(20), dp(18), dp(20), dp(18));
        hero.setBackground(roundRect(new int[]{Color.rgb(49, 73, 143), Color.rgb(91, 65, 157)}, dp(24)));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(16);
        homeRoot.addView(hero, hp);
        TextView greeting = text("YOUR SPACE. YOUR PACE.", 10, Color.rgb(222, 229, 255), true);
        greeting.setLetterSpacing(0.12f);
        hero.addView(greeting);
        clockText = text("--:--", 42, Color.WHITE, true);
        clockText.setIncludeFontPadding(false);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.topMargin = dp(5);
        hero.addView(clockText, cp);
        dateText = text("", 13, Color.rgb(226, 228, 250), false);
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(-1, -2);
        dateParams.topMargin = dp(4);
        hero.addView(dateText, dateParams);
        installLongPress(hero);
        installLongPress(greeting);
        installLongPress(clockText);
        installLongPress(dateText);

        widgetContainer = new LinearLayout(this);
        widgetContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams widgetsParams = new LinearLayout.LayoutParams(-1, -2);
        widgetsParams.topMargin = dp(8);
        homeRoot.addView(widgetContainer, widgetsParams);
        installLongPress(widgetContainer);

        LinearLayout search = new LinearLayout(this);
        search.setGravity(Gravity.CENTER_VERTICAL);
        search.setPadding(dp(14), 0, dp(12), 0);
        search.setBackground(roundRect(new int[]{Color.rgb(28, 34, 51), Color.rgb(23, 29, 45)}, dp(17)));
        TextView searchIcon = text("⌕", 27, Color.rgb(157, 174, 255), false);
        searchIcon.setGravity(Gravity.CENTER);
        search.addView(searchIcon, new LinearLayout.LayoutParams(dp(28), dp(48)));
        searchBox = new EditText(this);
        searchBox.setSingleLine(true);
        searchBox.setTextSize(14);
        searchBox.setTextColor(Color.WHITE);
        searchBox.setHintTextColor(Color.rgb(132, 144, 169));
        searchBox.setHint("Search your apps");
        searchBox.setBackgroundColor(Color.TRANSPARENT);
        searchBox.setPadding(dp(8), 0, 0, 0);
        searchBox.setSelectAllOnFocus(false);
        search.addView(searchBox, new LinearLayout.LayoutParams(0, dp(48), 1f));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, -2);
        searchParams.topMargin = dp(12);
        homeRoot.addView(search, searchParams);
        installLongPress(searchIcon);

        LinearLayout section = new LinearLayout(this);
        section.setGravity(Gravity.CENTER_VERTICAL);
        TextView sectionTitle = text("YOUR APPS", 12, Color.WHITE, true);
        sectionTitle.setLetterSpacing(0.12f);
        section.addView(sectionTitle, new LinearLayout.LayoutParams(0, dp(42), 1f));
        TextView count = text("TAP TO OPEN", 9, MUTED, true);
        section.addView(count);
        LinearLayout.LayoutParams sectionParams = new LinearLayout.LayoutParams(-1, -2);
        sectionParams.topMargin = dp(2);
        homeRoot.addView(section, sectionParams);
        installLongPress(section);
        installLongPress(sectionTitle);
        installLongPress(count);

        appGrid = new GridView(this);
        appGrid.setNumColumns(4);
        appGrid.setVerticalSpacing(dp(8));
        appGrid.setHorizontalSpacing(dp(4));
        appGrid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        appGrid.setSelector(android.R.color.transparent);
        appGrid.setClipToPadding(false);
        homeRoot.addView(appGrid, new LinearLayout.LayoutParams(-1, 0, 1f));

        TextView footer = text("HOLD HOME FOR 3 SECONDS TO CUSTOMIZE", 8, Color.rgb(132, 145, 174), true);
        footer.setGravity(Gravity.CENTER);
        footer.setLetterSpacing(0.06f);
        footer.setPadding(0, dp(9), 0, dp(6));
        homeRoot.addView(footer);
        installLongPress(footer);
        setContentView(homeRoot);

        adapter = new AppAdapter();
        appGrid.setAdapter(adapter);
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterApps(s == null ? "" : s.toString());
            }
            @Override public void afterTextChanged(Editable e) {}
        });
    }

    private void installLongPress(final View view) {
        final Runnable openCustomization = new Runnable() {
            @Override public void run() {
                if (!customizationOpen && !isFinishing()) {
                    view.setTag("long_press_triggered");
                    showCustomization();
                }
            }
        };
        view.setOnTouchListener(new View.OnTouchListener() {
            float downX, downY;
            @Override public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getX();
                        downY = event.getY();
                        handler.postDelayed(openCustomization, 3000L);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (Math.abs(event.getX() - downX) > dp(14) ||
                                Math.abs(event.getY() - downY) > dp(14)) handler.removeCallbacks(openCustomization);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        handler.removeCallbacks(openCustomization);
                        view.setTag(null);
                        break;
                }
                return false;
            }
        });
    }

    @Override protected void onResume() {
        super.onResume();
        handler.removeCallbacks(clockUpdater);
        handler.post(clockUpdater);
        if (appGrid != null && !customizationOpen) loadApps();
        if (widgetHost != null && !widgetListening) {
            try { widgetHost.startListening(); widgetListening = true; } catch (Exception ignored) {}
        }
    }

    @Override protected void onPause() {
        handler.removeCallbacks(clockUpdater);
        if (widgetHost != null && widgetListening) {
            try { widgetHost.stopListening(); } catch (Exception ignored) {}
            widgetListening = false;
        }
        super.onPause();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void updateClock() {
        if (clockText == null || dateText == null) return;
        Date now = new Date();
        clockText.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(now));
        dateText.setText(new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now));
    }

    private void loadApps() {
        Intent query = new Intent(Intent.ACTION_MAIN, null);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> result = packageManager.queryIntentActivities(query, 0);
        Collections.sort(result, new Comparator<ResolveInfo>() {
            @Override public int compare(ResolveInfo a, ResolveInfo b) {
                return a.loadLabel(packageManager).toString().compareToIgnoreCase(
                        b.loadLabel(packageManager).toString());
            }
        });
        allApps = result;
        filterApps(searchBox == null ? "" : searchBox.getText().toString());
    }

    private void filterApps(String query) {
        String needle = query.trim().toLowerCase(Locale.getDefault());
        visibleApps = new ArrayList<>();
        for (ResolveInfo app : allApps) {
            String label = app.loadLabel(packageManager).toString().toLowerCase(Locale.getDefault());
            if (needle.length() == 0 || label.contains(needle)) visibleApps.add(app);
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void showCustomization() {
        customizationOpen = true;
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(26), dp(22), dp(24));
        page.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(13, 18, 34), BG, Color.rgb(15, 19, 31)}));
        scroll.addView(page);
        TextView eyebrow = text("NOVA  /  HOME STUDIO", 10, Color.rgb(157, 174, 255), true);
        eyebrow.setLetterSpacing(0.12f);
        page.addView(eyebrow);
        TextView heading = text("Make it yours.", 30, Color.WHITE, true);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(-1, -2);
        headingParams.topMargin = dp(8);
        page.addView(heading, headingParams);
        TextView intro = text("Personalize your home screen without heavy extras.", 14, MUTED, false);
        LinearLayout.LayoutParams introParams = new LinearLayout.LayoutParams(-1, -2);
        introParams.topMargin = dp(6);
        page.addView(intro, introParams);

        addSectionLabel(page, "WALLPAPERS");
        addAction(page, "Midnight blue", "Deep blue gradient", new int[]{Color.rgb(11, 22, 51), Color.rgb(20, 42, 88), Color.rgb(10, 13, 24)}, new Runnable() {
            @Override public void run() { saveWallpaper(0, null); returnHome(); }
        });
        addAction(page, "Violet horizon", "Blue-to-purple gradient", new int[]{Color.rgb(37, 33, 90), Color.rgb(85, 49, 126), Color.rgb(15, 19, 31)}, new Runnable() {
            @Override public void run() { saveWallpaper(1, null); returnHome(); }
        });
        addAction(page, "Emerald night", "Dark teal glow", new int[]{Color.rgb(8, 43, 47), Color.rgb(18, 76, 76), Color.rgb(10, 13, 24)}, new Runnable() {
            @Override public void run() { saveWallpaper(2, null); returnHome(); }
        });
        addAction(page, "Choose a photo", "Use an image from your device", null, new Runnable() {
            @Override public void run() {
                Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                pick.addCategory(Intent.CATEGORY_OPENABLE);
                pick.setType("image/*");
                pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                try { startActivityForResult(pick, REQUEST_WALLPAPER_IMAGE); }
                catch (Exception e) { Toast.makeText(MainActivity.this, "No image picker available", Toast.LENGTH_SHORT).show(); }
            }
        });

        addSectionLabel(page, "HOME SCREEN WIDGETS");
        TextView widgetHelp = text("Add clock, calendar, weather and other widgets supported by your installed apps.", 13, MUTED, false);
        page.addView(widgetHelp);
        addAction(page, "＋  Add widget", "Choose from installed widget providers", new int[]{Color.rgb(45, 64, 120), Color.rgb(73, 54, 120)}, new Runnable() {
            @Override public void run() { launchWidgetPicker(); }
        });
        addAction(page, "Done", "Return to your home screen", new int[]{Color.rgb(33, 42, 61), Color.rgb(26, 32, 48)}, new Runnable() {
            @Override public void run() { returnHome(); }
        });
        setContentView(scroll);
    }

    private void addSectionLabel(LinearLayout page, String title) {
        TextView label = text(title, 10, Color.rgb(157, 174, 255), true);
        label.setLetterSpacing(0.13f);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(26);
        p.bottomMargin = dp(10);
        page.addView(label, p);
    }

    private void addAction(LinearLayout page, String title, String subtitle, int[] colors, final Runnable action) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackground(roundRect(colors == null
                ? new int[]{Color.rgb(30, 37, 56), Color.rgb(23, 29, 45)} : colors, dp(17)));
        TextView name = text(title, 15, Color.WHITE, true);
        card.addView(name);
        TextView detail = text(subtitle, 12, Color.rgb(190, 199, 220), false);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2);
        detailParams.topMargin = dp(4);
        card.addView(detail, detailParams);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(9);
        page.addView(card, p);
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { action.run(); }
        });
    }

    private void saveWallpaper(int style, String imageUri) {
        prefs.edit().putInt(KEY_WALLPAPER, style)
                .putString(KEY_IMAGE_URI, imageUri == null ? "" : imageUri).apply();
        Toast.makeText(this, "Wallpaper updated", Toast.LENGTH_SHORT).show();
    }

    private void applyWallpaperBackground() {
        if (homeRoot == null) return;
        int style = prefs == null ? 0 : prefs.getInt(KEY_WALLPAPER, 0);
        String imageUri = prefs == null ? "" : prefs.getString(KEY_IMAGE_URI, "");
        if (imageUri != null && imageUri.length() > 0) {
            try {
                Bitmap bitmap = decodeSampledBitmap(Uri.parse(imageUri), getResources().getDisplayMetrics().widthPixels,
                        getResources().getDisplayMetrics().heightPixels);
                if (bitmap != null) {
                    homeRoot.setBackground(new BitmapDrawable(getResources(), bitmap));
                    return;
                }
            } catch (Exception ignored) {}
        }
        int[] colors;
        if (style == 1) colors = new int[]{Color.rgb(37, 33, 90), Color.rgb(85, 49, 126), Color.rgb(15, 19, 31)};
        else if (style == 2) colors = new int[]{Color.rgb(8, 43, 47), Color.rgb(18, 76, 76), Color.rgb(10, 13, 24)};
        else colors = new int[]{Color.rgb(11, 22, 51), Color.rgb(20, 42, 88), BG};
        homeRoot.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors));
    }

    private void restoreWallpaper() { applyWallpaperBackground(); }

    private Bitmap decodeSampledBitmap(Uri uri, int reqWidth, int reqHeight) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        InputStream first = getContentResolver().openInputStream(uri);
        if (first == null) return null;
        try { BitmapFactory.decodeStream(first, null, bounds); } finally { first.close(); }
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = 1;
        while (bounds.outHeight / opts.inSampleSize > reqHeight * 1.5 ||
                bounds.outWidth / opts.inSampleSize > reqWidth * 1.5) opts.inSampleSize *= 2;
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        InputStream second = getContentResolver().openInputStream(uri);
        if (second == null) return null;
        try { return BitmapFactory.decodeStream(second, null, opts); } finally { second.close(); }
    }

    private void returnHome() {
        customizationOpen = false;
        buildHome();
        loadApps();
        updateClock();
        restoreWidgets();
        applyWallpaperBackground();
    }

    private void launchWidgetPicker() {
        int appWidgetId = widgetHost.allocateAppWidgetId();
        Intent pick = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
        pick.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        try { startActivityForResult(pick, REQUEST_PICK_WIDGET); }
        catch (Exception e) {
            widgetHost.deleteAppWidgetId(appWidgetId);
            Toast.makeText(this, "Widget picker unavailable on this device", Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_WALLPAPER_IMAGE) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                Uri uri = data.getData();
                try {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    saveWallpaper(3, uri.toString());
                    returnHome();
                } catch (Exception e) {
                    Toast.makeText(this, "Could not use that image", Toast.LENGTH_SHORT).show();
                }
            }
            return;
        }
        if (requestCode == REQUEST_PICK_WIDGET) {
            if (resultCode == RESULT_OK && data != null) {
                int id = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
                if (id >= 0) {
                    AppWidgetProviderInfo info = widgetManager.getAppWidgetInfo(id);
                    if (info != null && info.configure != null) {
                        Intent configure = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
                        configure.setComponent(info.configure);
                        configure.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
                        try { startActivityForResult(configure, REQUEST_CONFIGURE_WIDGET); }
                        catch (Exception e) { finishAddingWidget(id); }
                    } else finishAddingWidget(id);
                }
            } else if (data != null) {
                int id = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
                if (id >= 0) widgetHost.deleteAppWidgetId(id);
            }
            return;
        }
        if (requestCode == REQUEST_BIND_WIDGET) {
            if (resultCode == RESULT_OK && data != null) {
                finishAddingWidget(data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1));
            } else if (data != null) {
                int id = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
                if (id >= 0) widgetHost.deleteAppWidgetId(id);
            }
            return;
        }
        if (requestCode == REQUEST_CONFIGURE_WIDGET && data != null) {
            int id = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if (resultCode == RESULT_OK) finishAddingWidget(id);
            else if (id >= 0) widgetHost.deleteAppWidgetId(id);
        }
    }

    private void finishAddingWidget(int id) {
        if (id < 0) return;
        AppWidgetProviderInfo info = widgetManager.getAppWidgetInfo(id);
        if (info == null) {
            widgetHost.deleteAppWidgetId(id);
            Toast.makeText(this, "Could not add this widget", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!widgetIds.contains(id)) widgetIds.add(id);
        persistWidgetIds();
        if (customizationOpen) returnHome();
        else restoreWidgets();
    }

    private void restoreWidgetIds() {
        widgetIds.clear();
        String saved = prefs.getString(KEY_WIDGETS, "");
        if (saved != null && !saved.isEmpty()) {
            for (String part : saved.split(",")) {
                try {
                    int id = Integer.parseInt(part);
                    if (widgetManager.getAppWidgetInfo(id) != null) widgetIds.add(id);
                    else widgetHost.deleteAppWidgetId(id);
                } catch (Exception ignored) {}
            }
        }
        persistWidgetIds();
        restoreWidgets();
    }

    private void persistWidgetIds() {
        StringBuilder value = new StringBuilder();
        for (Integer id : widgetIds) {
            if (value.length() > 0) value.append(',');
            value.append(id);
        }
        prefs.edit().putString(KEY_WIDGETS, value.toString()).apply();
    }

    private void restoreWidgets() {
        if (widgetContainer == null) return;
        widgetContainer.removeAllViews();
        for (Integer id : new ArrayList<>(widgetIds)) {
            try {
                AppWidgetProviderInfo info = widgetManager.getAppWidgetInfo(id);
                if (info == null) continue;
                AppWidgetHostView hostView = widgetHost.createView(this, id, info);
                hostView.setAppWidget(id, info);
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
                p.bottomMargin = dp(8);
                widgetContainer.addView(hostView, p);
            } catch (Exception ignored) {}
        }
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private GradientDrawable roundRect(int[] colors, int radiusDp) {
        GradientDrawable shape = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
        shape.setCornerRadius(dp(radiusDp));
        return shape;
    }

    private int dp(float value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private class AppAdapter extends BaseAdapter {
        @Override public int getCount() { return visibleApps == null ? 0 : visibleApps.size(); }
        @Override public Object getItem(int position) { return visibleApps.get(position); }
        @Override public long getItemId(int position) { return position; }
        @Override public View getView(int position, View recycled, ViewGroup parent) {
            LinearLayout cell;
            ImageView icon;
            TextView label;
            if (recycled instanceof LinearLayout) {
                cell = (LinearLayout) recycled;
                icon = (ImageView) cell.getChildAt(0);
                label = (TextView) cell.getChildAt(1);
            } else {
                cell = new LinearLayout(MainActivity.this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER);
                cell.setPadding(dp(2), dp(7), dp(2), dp(7));
                icon = new ImageView(MainActivity.this);
                icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(48), dp(48));
                ip.gravity = Gravity.CENTER;
                cell.addView(icon, ip);
                label = text("", 10, Color.rgb(236, 239, 249), false);
                label.setGravity(Gravity.CENTER);
                label.setMaxLines(2);
                label.setEllipsize(android.text.TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(32));
                lp.topMargin = dp(5);
                cell.addView(label, lp);
                cell.setBackground(roundRect(new int[]{Color.argb(18, 255, 255, 255),
                        Color.argb(5, 255, 255, 255)}, dp(15)));
            }
            final ResolveInfo info = visibleApps.get(position);
            icon.setImageDrawable(info.loadIcon(packageManager));
            label.setText(info.loadLabel(packageManager));
            cell.setContentDescription(info.loadLabel(packageManager));
            cell.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    Intent launch = packageManager.getLaunchIntentForPackage(info.activityInfo.packageName);
                    if (launch != null) {
                        try {
                            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(launch);
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "Unable to open app", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
            return cell;
        }
    }
}
