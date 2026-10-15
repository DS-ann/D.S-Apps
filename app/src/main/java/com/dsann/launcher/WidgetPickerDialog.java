package com.dsann.launcher;

import android.app.Activity;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Scrollable widget picker with per-widget previews. */
final class WidgetPickerDialog {
    interface Listener { void onWidgetSelected(AppWidgetProviderInfo info); }
    private final Activity activity;
    private final AppWidgetManager widgetManager;
    private final PackageManager packageManager;
    private final Listener onWidgetSelected;

    WidgetPickerDialog(Activity activity, AppWidgetManager widgetManager, PackageManager packageManager, Listener onWidgetSelected) {
        this.activity = activity;
        this.widgetManager = widgetManager;
        this.packageManager = packageManager;
        this.onWidgetSelected = onWidgetSelected;
    }

    void show() {
        List<AppWidgetProviderInfo> providers = new ArrayList<>(widgetManager.getInstalledProviders());
        Collections.sort(providers, new Comparator<AppWidgetProviderInfo>() {
            @Override public int compare(AppWidgetProviderInfo a, AppWidgetProviderInfo b) {
                String left = a == null ? "" : String.valueOf(a.loadLabel(packageManager));
                String right = b == null ? "" : String.valueOf(b.loadLabel(packageManager));
                return left.compareToIgnoreCase(right);
            }
        });
        List<AppWidgetProviderInfo> available = new ArrayList<>();
        for (AppWidgetProviderInfo info : providers) if (info != null && info.provider != null) available.add(info);
        if (available.isEmpty()) {
            Toast.makeText(activity, "No installed app widgets are available", Toast.LENGTH_LONG).show();
            return;
        }
        Dialog dialog = new Dialog(activity);
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(LauncherUiUtils.dp(activity, 16), LauncherUiUtils.dp(activity, 14), LauncherUiUtils.dp(activity, 16), LauncherUiUtils.dp(activity, 12));
        panel.setBackground(LauncherUiUtils.roundRect(activity, new int[]{Color.rgb(19,24,42),Color.rgb(12,16,30)}, 20));
        TextView title = LauncherUiUtils.text(activity, "Choose a widget", 20, Color.WHITE, true);
        title.setGravity(Gravity.CENTER_VERTICAL);
        panel.addView(title, new LinearLayout.LayoutParams(-1, LauncherUiUtils.dp(activity,42)));
        TextView subtitle = LauncherUiUtils.text(activity, "Preview each widget before adding it", 12, Color.rgb(165,177,207), false);
        subtitle.setGravity(Gravity.CENTER_VERTICAL);
        panel.addView(subtitle, new LinearLayout.LayoutParams(-1, LauncherUiUtils.dp(activity,28)));
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(true);
        scroll.setClipToPadding(false);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, LauncherUiUtils.dp(activity,440));
        scrollParams.topMargin = LauncherUiUtils.dp(activity,8);
        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        for (int i=0; i<available.size(); i++) {
            final int index=i;
            AppWidgetProviderInfo info=available.get(i);
            LinearLayout row=new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(LauncherUiUtils.dp(activity,10),LauncherUiUtils.dp(activity,10),LauncherUiUtils.dp(activity,10),LauncherUiUtils.dp(activity,10));
            row.setBackground(LauncherUiUtils.roundRect(activity,new int[]{Color.rgb(34,42,67),Color.rgb(27,33,54)},14));
            ImageView preview=new ImageView(activity);
            preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
            preview.setBackground(LauncherUiUtils.roundRect(activity,new int[]{Color.rgb(12,16,28),Color.rgb(20,26,43)},10));
            preview.setPadding(LauncherUiUtils.dp(activity,4),LauncherUiUtils.dp(activity,4),LauncherUiUtils.dp(activity,4),LauncherUiUtils.dp(activity,4));
            android.graphics.drawable.Drawable image=null;
            try { if(info.previewImage!=0) image=info.loadPreviewImage(activity,activity.getResources().getDisplayMetrics().densityDpi); } catch(Exception ignored) {}
            if(image==null) try { image=info.loadIcon(activity,activity.getResources().getDisplayMetrics().densityDpi); } catch(Exception ignored) {}
            if(image!=null) preview.setImageDrawable(image);
            row.addView(preview,new LinearLayout.LayoutParams(LauncherUiUtils.dp(activity,100),LauncherUiUtils.dp(activity,86)));
            LinearLayout details=new LinearLayout(activity);
            details.setOrientation(LinearLayout.VERTICAL);
            details.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams detailsParams=new LinearLayout.LayoutParams(0,-2,1f);
            detailsParams.leftMargin=LauncherUiUtils.dp(activity,12);
            TextView name=LauncherUiUtils.text(activity,String.valueOf(info.loadLabel(packageManager)),15,Color.rgb(246,248,255),true);
            name.setMaxLines(2);
            TextView provider=LauncherUiUtils.text(activity,info.provider.getPackageName(),11,Color.rgb(158,171,202),false);
            provider.setMaxLines(1); provider.setSingleLine(true); provider.setEllipsize(android.text.TextUtils.TruncateAt.END);
            details.addView(name,new LinearLayout.LayoutParams(-1,-2));
            LinearLayout.LayoutParams providerParams=new LinearLayout.LayoutParams(-1,-2);
            providerParams.topMargin=LauncherUiUtils.dp(activity,4);
            details.addView(provider,providerParams);
            row.addView(details,detailsParams);
            TextView add=LauncherUiUtils.text(activity,"＋",25,Color.rgb(178,198,255),true);
            add.setGravity(Gravity.CENTER);
            row.addView(add,new LinearLayout.LayoutParams(LauncherUiUtils.dp(activity,32),LauncherUiUtils.dp(activity,48)));
            LinearLayout.LayoutParams rowParams=new LinearLayout.LayoutParams(-1,-2);
            rowParams.bottomMargin=LauncherUiUtils.dp(activity,8);
            list.addView(row,rowParams);
            row.setOnClickListener(v->{dialog.dismiss();onWidgetSelected.onWidgetSelected(available.get(index));});
        }
        scroll.addView(list,new ScrollView.LayoutParams(-1,-2));
        panel.addView(scroll,scrollParams);
        TextView cancel=LauncherUiUtils.text(activity,"Cancel",14,Color.rgb(190,201,230),true);
        cancel.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams cancelParams=new LinearLayout.LayoutParams(-1,LauncherUiUtils.dp(activity,44));
        cancelParams.topMargin=LauncherUiUtils.dp(activity,4);
        panel.addView(cancel,cancelParams);
        cancel.setOnClickListener(v->dialog.dismiss());
        dialog.setContentView(panel);
        if(dialog.getWindow()!=null){dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);android.view.WindowManager.LayoutParams p=dialog.getWindow().getAttributes();p.dimAmount=0.58f;dialog.getWindow().setAttributes(p);}
        dialog.show();
        if(dialog.getWindow()!=null){int h=activity.getResources().getDisplayMetrics().heightPixels;dialog.getWindow().setLayout(activity.getResources().getDisplayMetrics().widthPixels-LauncherUiUtils.dp(activity,28),Math.min(h-LauncherUiUtils.dp(activity,48),LauncherUiUtils.dp(activity,570)));}
        panel.setAlpha(0f);panel.setScaleX(0.96f);panel.setScaleY(0.96f);panel.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(180).start();
    }
}
