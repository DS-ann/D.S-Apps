package com.dsann.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private final int BG = Color.rgb(10, 13, 24);
    private final int MUTED = Color.rgb(157, 169, 194);
    private final Handler handler = new Handler();
    private PackageManager packageManager;
    private GridView appGrid;
    private EditText searchBox;
    private TextView clockText;
    private TextView dateText;
    private List<ResolveInfo> allApps = new ArrayList<>();
    private List<ResolveInfo> visibleApps = new ArrayList<>();
    private AppAdapter adapter;
    private final Runnable clockUpdater = new Runnable() {
        @Override public void run() {
            updateClock();
            handler.postDelayed(this, 60000L);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        packageManager = getPackageManager();
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(8));
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(13, 18, 34), BG, Color.rgb(15, 19, 31)}));

        LinearLayout topRow = new LinearLayout(this);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text("NOVA", 13, Color.WHITE, true);
        brand.setLetterSpacing(0.18f);
        TextView status = text("LIGHTWEIGHT  •  FAST", 9, MUTED, true);
        status.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        topRow.addView(brand, new LinearLayout.LayoutParams(0, dp(28), 1f));
        topRow.addView(status, new LinearLayout.LayoutParams(-2, dp(28)));
        root.addView(topRow);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(20), dp(18), dp(20), dp(18));
        hero.setBackground(roundRect(new int[]{Color.rgb(49, 73, 143), Color.rgb(91, 65, 157)}, dp(24)));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(16);
        root.addView(hero, hp);

        TextView greeting = text("YOUR SPACE. YOUR PACE.", 10, Color.rgb(222, 229, 255), true);
        greeting.setLetterSpacing(0.12f);
        hero.addView(greeting);
        clockText = text("--:--", 42, Color.WHITE, true);
        clockText.setIncludeFontPadding(false);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.topMargin = dp(5);
        hero.addView(clockText, cp);
        dateText = text("", 13, Color.rgb(226, 228, 250), false);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1, -2);
        dp.topMargin = dp(4);
        hero.addView(dateText, dp);

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
        searchParams.topMargin = dp(16);
        root.addView(search, searchParams);

        LinearLayout section = new LinearLayout(this);
        section.setGravity(Gravity.CENTER_VERTICAL);
        TextView sectionTitle = text("YOUR APPS", 12, Color.WHITE, true);
        sectionTitle.setLetterSpacing(0.12f);
        section.addView(sectionTitle, new LinearLayout.LayoutParams(0, dp(42), 1f));
        TextView count = text("TAP TO OPEN", 9, MUTED, true);
        section.addView(count);
        LinearLayout.LayoutParams sectionParams = new LinearLayout.LayoutParams(-1, -2);
        sectionParams.topMargin = dp(4);
        root.addView(section, sectionParams);

        appGrid = new GridView(this);
        appGrid.setNumColumns(4);
        appGrid.setVerticalSpacing(dp(8));
        appGrid.setHorizontalSpacing(dp(4));
        appGrid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        appGrid.setSelector(android.R.color.transparent);
        appGrid.setClipToPadding(false);
        root.addView(appGrid, new LinearLayout.LayoutParams(-1, 0, 1f));

        TextView footer = text("MADE TO STAY LIGHT", 9, Color.rgb(103, 117, 145), true);
        footer.setGravity(Gravity.CENTER);
        footer.setLetterSpacing(0.14f);
        footer.setPadding(0, dp(9), 0, dp(6));
        root.addView(footer);
        setContentView(root);

        adapter = new AppAdapter();
        appGrid.setAdapter(adapter);
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterApps(s == null ? "" : s.toString());
            }
            @Override public void afterTextChanged(Editable e) {}
        });
        loadApps();
        updateClock();
    }

    @Override protected void onResume() {
        super.onResume();
        handler.removeCallbacks(clockUpdater);
        handler.post(clockUpdater);
        if (appGrid != null) loadApps();
    }

    @Override protected void onPause() {
        handler.removeCallbacks(clockUpdater);
        super.onPause();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void updateClock() {
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
