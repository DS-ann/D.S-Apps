package com.dsann.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {
    private GridView appGrid;
    private List<ResolveInfo> apps;
    private PackageManager packageManager;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        packageManager = getPackageManager();
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(18, 22, 32));
        root.setPadding(dp(16), dp(22), dp(16), dp(8));

        TextView title = new TextView(this);
        title.setText("My Launcher"); title.setTextColor(Color.WHITE); title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD); root.addView(title);
        TextView subtitle = new TextView(this);
        subtitle.setText("Your apps, one place"); subtitle.setTextColor(Color.rgb(176,190,210)); subtitle.setTextSize(14);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2); sp.bottomMargin = dp(18); root.addView(subtitle, sp);
        appGrid = new GridView(this); appGrid.setNumColumns(4); appGrid.setVerticalSpacing(dp(10));
        appGrid.setHorizontalSpacing(dp(4)); appGrid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        appGrid.setSelector(android.R.color.transparent); root.addView(appGrid, new LinearLayout.LayoutParams(-1, 0, 1f));
        TextView footer = new TextView(this); footer.setText("Tap an app to open it"); footer.setTextColor(Color.rgb(130,145,166));
        footer.setTextSize(12); footer.setGravity(Gravity.CENTER); footer.setPadding(0, dp(10), 0, dp(8)); root.addView(footer);
        setContentView(root); loadApps();
    }

    @Override protected void onResume() { super.onResume(); if (appGrid != null) loadApps(); }

    private void loadApps() {
        Intent query = new Intent(Intent.ACTION_MAIN, null); query.addCategory(Intent.CATEGORY_LAUNCHER);
        apps = packageManager.queryIntentActivities(query, 0);
        Collections.sort(apps, new Comparator<ResolveInfo>() {
            @Override public int compare(ResolveInfo a, ResolveInfo b) {
                return a.loadLabel(packageManager).toString().compareToIgnoreCase(b.loadLabel(packageManager).toString());
            }
        });
        if (appGrid != null) appGrid.setAdapter(new AppAdapter());
    }

    private int dp(float value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }

    private class AppAdapter extends BaseAdapter {
        @Override public int getCount() { return apps == null ? 0 : apps.size(); }
        @Override public Object getItem(int position) { return apps.get(position); }
        @Override public long getItemId(int position) { return position; }
        @Override public View getView(int position, View recycled, android.view.ViewGroup parent) {
            LinearLayout cell; ImageView icon; TextView label;
            if (recycled instanceof LinearLayout) {
                cell = (LinearLayout)recycled; icon = (ImageView)cell.getChildAt(0); label = (TextView)cell.getChildAt(1);
            } else {
                cell = new LinearLayout(MainActivity.this); cell.setOrientation(LinearLayout.VERTICAL); cell.setGravity(Gravity.CENTER);
                cell.setPadding(dp(3), dp(8), dp(3), dp(8)); icon = new ImageView(MainActivity.this);
                LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(48), dp(48)); ip.gravity = Gravity.CENTER; cell.addView(icon, ip);
                label = new TextView(MainActivity.this); label.setTextColor(Color.WHITE); label.setTextSize(11); label.setGravity(Gravity.CENTER);
                label.setMaxLines(2); label.setEllipsize(android.text.TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(36)); lp.topMargin = dp(5); cell.addView(label, lp);
                cell.setBackgroundResource(android.R.drawable.list_selector_background);
            }
            final ResolveInfo info = apps.get(position); Drawable drawable = info.loadIcon(packageManager);
            icon.setImageDrawable(drawable); label.setText(info.loadLabel(packageManager)); cell.setContentDescription(info.loadLabel(packageManager));
            cell.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
                Intent launch = packageManager.getLaunchIntentForPackage(info.activityInfo.packageName);
                if (launch != null) { try { startActivity(launch); } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Unable to open app", Toast.LENGTH_SHORT).show();
                }}
            }});
            return cell;
        }
    }
}
