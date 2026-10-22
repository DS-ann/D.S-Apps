package com.dsann.batterymonitor;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity {
    private static final String PREFS="settings";
    private static final String KEY_INTERVAL_MS="sampling_interval_ms";
    private static final String KEY_MY_BATTERY_INTERVAL_MS="my_battery_sampling_interval_ms";
    private static final String KEY_USE_ANDROID_PERCENT="my_battery_use_android_percent";
    private static final String KEY_ANDROID_PERCENT_RESET_PENDING="my_battery_android_percent_reset_pending";
    private static final String KEY_LAST_ANDROID_PERCENT="my_battery_last_android_percent";
    private static final String KEY_LAST_ANDROID_POWER_STATUS="my_battery_last_android_power_status";
    private static final String KEY_LAST_ANDROID_POWER_SOURCE="my_battery_last_android_power_source";
    private static final long MIN_MY_BATTERY_INTERVAL_MS=50L;
    private static final long MAX_MY_BATTERY_INTERVAL_MS=1500L;
    private static final long MY_BATTERY_INTERVAL_STEP_MS=10L;
    private static final long DEFAULT_MY_BATTERY_INTERVAL_MS=500L;
    private static final long MIN_RECORDING_INTERVAL_MS=100L;
    private static final long MAX_RECORDING_INTERVAL_MS=2000L;
    private static final long RECORDING_INTERVAL_STEP_MS=10L;
    private static final long DEFAULT_RECORDING_INTERVAL_MS=1000L;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService sensorExecutor=Executors.newSingleThreadExecutor();
    private final AtomicBoolean monitorReadInFlight=new AtomicBoolean(false);
    private final AtomicBoolean myBatteryReadInFlight=new AtomicBoolean(false);
    private final AtomicBoolean healthReadInFlight=new AtomicBoolean(false);
    private final AtomicBoolean calculatorStartInFlight=new AtomicBoolean(false);
    private TextView voltage,current;
    private TextView calculatedPercent,calculatedMah,myBatteryCurrentLabel,myBatteryRateLabel,androidPercentStatus;
    private EditText customCapacityInput,startingChargeInput;
    private Button useAndroidPercentButton;
    private Switch notificationSwitch;
    private Switch fakeChargingSwitch;
    private TextView fakeChargingStatus;
    private SeekBar samplingBar,myBatterySamplingBar;
    private TextView samplingLabel,myBatterySamplingLabel;
    private LinearLayout monitorView,myBatteryView,historyView,historyList;
    private View monitorContainer;
    private ScrollView myBatteryScroll;
    private TextView monitorTab,myBatteryTab,historyTab,healthTab;
    private LinearLayout healthView;
    private ScrollView healthScroll;
    private TextView healthLevel,healthStatus,healthTemp,healthVoltage,healthCurrent,healthSource,healthFullCharge,healthRemaining,healthDesign,healthRatio,healthRaw,healthFile,healthFullEnergy,healthDesignEnergy,healthSessionEstimate,healthSessionHealth;
    private final Runnable updater=new Runnable(){public void run(){if(monitorContainer==null||monitorContainer.getVisibility()==View.VISIBLE)updateValues();handler.postDelayed(this,currentRecordingIntervalMs());}};
    private final Runnable myBatteryUpdater=new Runnable(){public void run(){if(myBatteryScroll==null||myBatteryScroll.getVisibility()==View.VISIBLE)updateMyBatteryEstimate();handler.postDelayed(this,currentMyBatteryIntervalMs());}};
    
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(0xFF0D1117);
        getWindow().setNavigationBarColor(0xFF0D1117);
        
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0D1117);
        
        LinearLayout header=new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(18),dp(16),dp(18),dp(15));
        header.setBackground(round(0xFF151D27,18));
        TextView appTitle=text("BatteryPulse",25);
        appTitle.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        appTitle.setLetterSpacing(0.015f);
        header.addView(appTitle,full());
        TextView subtitle=text("Battery monitor • sessions • health",13);
        subtitle.setTextColor(0xFF9AA0AA);
        header.addView(subtitle,top(2));
        LinearLayout.LayoutParams headerParams=top(10);
        headerParams.leftMargin=dp(16);
        headerParams.rightMargin=dp(16);
        root.addView(header,headerParams);
        
        LinearLayout tabs=new LinearLayout(this);
        tabs.setPadding(dp(4),dp(4),dp(4),dp(4));
        tabs.setBackground(round(0xFF151B24,15));
        monitorTab=tab("Monitor");
        myBatteryTab=tab("My Battery");
        historyTab=tab("History");
        healthTab=tab("Health");
        addTabToBar(tabs,monitorTab);
        addTabToBar(tabs,myBatteryTab);
        addTabToBar(tabs,historyTab);
        addTabToBar(tabs,healthTab);
        LinearLayout.LayoutParams tabsParams=top(10);
        tabsParams.leftMargin=dp(16);
        tabsParams.rightMargin=dp(16);
        root.addView(tabs,tabsParams);
        
        monitorView=buildMonitor();
        monitorContainer=new ScrollView(this);
        ((ScrollView)monitorContainer).setFillViewport(true);
        ((ScrollView)monitorContainer).setVerticalScrollBarEnabled(false);
        ((ScrollView)monitorContainer).addView(monitorView);
        myBatteryView=buildMyBattery();
        myBatteryScroll=new ScrollView(this);
        myBatteryScroll.setFillViewport(true);
        myBatteryScroll.setVerticalScrollBarEnabled(false);
        myBatteryScroll.setClipToPadding(false);
        myBatteryScroll.setPadding(0,0,0,dp(8));
        myBatteryScroll.addView(myBatteryView);
        historyView=buildHistory();
        LinearLayout healthContent=buildHealth();
        healthScroll=new ScrollView(this);
        healthScroll.setFillViewport(true);
        healthScroll.setVerticalScrollBarEnabled(false);
        healthScroll.setClipToPadding(false);
        healthScroll.setPadding(0,0,0,dp(8));
        healthScroll.addView(healthContent);
        healthView=healthContent;
        
        root.addView(monitorContainer,new LinearLayout.LayoutParams(-1,0,1));
        root.addView(myBatteryScroll,new LinearLayout.LayoutParams(-1,0,1));
        root.addView(historyView,new LinearLayout.LayoutParams(-1,0,1));
        root.addView(healthScroll,new LinearLayout.LayoutParams(-1,0,1));
        myBatteryScroll.setVisibility(View.GONE);
        historyView.setVisibility(View.GONE);
        healthScroll.setVisibility(View.GONE);
        selectTab(0);
        monitorTab.setOnClickListener(v->showTab(0));
        myBatteryTab.setOnClickListener(v->showTab(1));
        historyTab.setOnClickListener(v->showTab(2));
        healthTab.setOnClickListener(v->showTab(3));
        
        setContentView(root);
        android.content.SharedPreferences initialPrefs=getSharedPreferences(PREFS,0);
        if (notificationSwitch.isChecked()
                || initialPrefs.getBoolean(KEY_USE_ANDROID_PERCENT,false)
                || initialPrefs.getBoolean(KEY_ANDROID_PERCENT_RESET_PENDING,false)
                || BatteryPercentCalculator.isRunning(this)) {
            startNotificationService();
        }
        if (BatteryChargingOverride.isEnabled(this)) {
            try {
                sensorExecutor.execute(() -> {
                    BatteryChargingOverride.Result result =
                            BatteryChargingOverride.reapplyIfEnabled(MainActivity.this);
                    runOnUiThread(() -> {
                        if (!isFinishing() && !isDestroyed() && fakeChargingStatus != null) {
                            fakeChargingStatus.setText(result.message);
                            fakeChargingStatus.setTextColor(result.success
                                    ? 0xFF63E6A8 : 0xFFFF7F9A);
                        }
                    });
                });
            } catch (RejectedExecutionException ignored) { }
        }
        root.setFocusableInTouchMode(true);
        root.requestFocus();
    }
    
    private LinearLayout buildMonitor(){
        LinearLayout r=new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(dp(16),dp(4),dp(16),dp(20));
        
        LinearLayout metrics=new LinearLayout(this);
        metrics.setOrientation(LinearLayout.HORIZONTAL);
        
        LinearLayout voltageCard=metricCard("VOLTAGE","-- V",0xFF172334,0xFF63B3FF);
        voltage=(TextView)voltageCard.getTag();
        LinearLayout currentCard=metricCard("CURRENT","-- mA",0xFF17271F,0xFF63E6A8);
        current=(TextView)currentCard.getTag();
        metrics.addView(voltageCard,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout.LayoutParams curLp=new LinearLayout.LayoutParams(0,-2,1);
        curLp.leftMargin=dp(8);
        metrics.addView(currentCard,curLp);
        r.addView(metrics);
        

        
        LinearLayout recording=card();
        LinearLayout recordingTop=new LinearLayout(this);
        recordingTop.setGravity(Gravity.CENTER_VERTICAL);
        TextView recordingTitle=text("Background recording",17);
        recordingTitle.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        recordingTop.addView(recordingTitle,new LinearLayout.LayoutParams(0,-2,1));
        notificationSwitch=new Switch(this);
        recordingTop.addView(notificationSwitch,new LinearLayout.LayoutParams(-2,-2));
        recording.addView(recordingTop,full());
        TextView recordingSub=text("Keeps the session updated while the app is closed.",13);
        recordingSub.setTextColor(0xFF9AA0AA);
        recording.addView(recordingSub,top(2));
        
        LinearLayout sampling=new LinearLayout(this);
        sampling.setOrientation(LinearLayout.VERTICAL);
        samplingLabel=text("Current recording interval  •  1.00 s",13);
        samplingLabel.setTextColor(0xFF9AA0AA);
        sampling.addView(samplingLabel,full());
        samplingBar=new SeekBar(this);
        samplingBar.setMax((int)((MAX_RECORDING_INTERVAL_MS-MIN_RECORDING_INTERVAL_MS)/RECORDING_INTERVAL_STEP_MS));
        sampling.addView(samplingBar,new LinearLayout.LayoutParams(-1,-2));
        recording.addView(sampling,top(10));
        r.addView(recording,top(12));
        
        android.content.SharedPreferences prefs=getSharedPreferences(PREFS,0);
        long savedInterval=prefs.getLong(KEY_INTERVAL_MS,DEFAULT_RECORDING_INTERVAL_MS);
        if(savedInterval<MIN_RECORDING_INTERVAL_MS||savedInterval>MAX_RECORDING_INTERVAL_MS)
            savedInterval=DEFAULT_RECORDING_INTERVAL_MS;
        samplingBar.setProgress((int)((savedInterval-MIN_RECORDING_INTERVAL_MS)/RECORDING_INTERVAL_STEP_MS));
        samplingLabel.setText(formatRecordingInterval(savedInterval));
        notificationSwitch.setChecked(prefs.getBoolean("notification_enabled",false));
        samplingBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int progress,boolean fromUser){
                long interval=MIN_RECORDING_INTERVAL_MS+progress*RECORDING_INTERVAL_STEP_MS;
                samplingLabel.setText(formatRecordingInterval(interval));
                if(fromUser)getSharedPreferences(PREFS,0).edit().putLong(KEY_INTERVAL_MS,interval).apply();
            }
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){}
        });
        
        LinearLayout dozeBox=toolCard("Force Doze","Put Android into Doze for power testing.");
        LinearLayout dozeRow=new LinearLayout(this);
        dozeRow.setGravity(Gravity.CENTER_VERTICAL);
        Button enableDoze=actionButton("Enable");
        Button disableDoze=actionButton("Disable");
        dozeRow.addView(enableDoze,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout.LayoutParams dlp=new LinearLayout.LayoutParams(0,-2,1);
        dlp.leftMargin=dp(8);
        dozeRow.addView(disableDoze,dlp);
        dozeBox.addView(dozeRow,top(10));
        TextView dozeStatus=text("Normal idle behavior",12);
        dozeStatus.setTextColor(0xFF858D99);
        dozeBox.addView(dozeStatus,top(4));
        r.addView(dozeBox,top(12));
        
        enableDoze.setOnClickListener(v->{
            try{sensorExecutor.execute(()->{
                DozeController.Result result=DozeController.forceDoze();
                runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())dozeStatus.setText(result.message);});
            });}catch(RejectedExecutionException ignored){}
        });
        disableDoze.setOnClickListener(v->{
            try{sensorExecutor.execute(()->{
                DozeController.Result result=DozeController.disableForceDoze();
                runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())dozeStatus.setText(result.message);});
            });}catch(RejectedExecutionException ignored){}
        });
        
        LinearLayout fakeChargeBox=toolCard("Fake Android charging",
                "Simulates Android's charging indication only; it does not charge the battery.");
        LinearLayout fakeChargeRow=new LinearLayout(this);
        fakeChargeRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView fakeChargeLabel=text("Reported charging state",14);
        fakeChargeRow.addView(fakeChargeLabel,new LinearLayout.LayoutParams(0,-2,1));
        fakeChargingSwitch=new Switch(this);
        fakeChargingSwitch.setChecked(BatteryChargingOverride.isEnabled(this));
        fakeChargeRow.addView(fakeChargingSwitch,new LinearLayout.LayoutParams(-2,-2));
        fakeChargeBox.addView(fakeChargeRow,top(8));
        fakeChargingStatus=text(BatteryChargingOverride.isEnabled(this)
                ? "Fake Android charging is active."
                : "Android uses the reported battery state.",12);
        fakeChargingStatus.setTextColor(0xFF858D99);
        fakeChargeBox.addView(fakeChargingStatus,top(4));
        r.addView(fakeChargeBox,top(12));
        fakeChargingSwitch.setOnCheckedChangeListener((button,on)->handleFakeChargingSwitch(on));

        LinearLayout fgDaemonBox=toolCard("Fuel-gauge daemon",
                "Root control for /sys/devices/platform/battery_meter/FG_daemon_disable.");
        LinearLayout fgDaemonRow=new LinearLayout(this);
        fgDaemonRow.setGravity(Gravity.CENTER_VERTICAL);
        Button disableFgDaemon=actionButton("Disable daemon");
        Button enableFgDaemon=actionButton("Enable daemon");
        fgDaemonRow.addView(disableFgDaemon,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout.LayoutParams enableFgParams=new LinearLayout.LayoutParams(0,-2,1);
        enableFgParams.leftMargin=dp(8);
        fgDaemonRow.addView(enableFgDaemon,enableFgParams);
        fgDaemonBox.addView(fgDaemonRow,top(10));
        TextView fgDaemonStatus=text(
                "Disable writes 1; Enable writes 0. Re-enable after testing.",12);
        fgDaemonStatus.setTextColor(0xFF858D99);
        fgDaemonBox.addView(fgDaemonStatus,top(4));
        r.addView(fgDaemonBox,top(12));
        disableFgDaemon.setOnClickListener(v->runFuelGaugeDaemonCommand(
                true,disableFgDaemon,enableFgDaemon,fgDaemonStatus));
        enableFgDaemon.setOnClickListener(v->runFuelGaugeDaemonCommand(
                false,disableFgDaemon,enableFgDaemon,fgDaemonStatus));

        LinearLayout percentBox=toolCard("Battery percentage override","Temporarily change Android's displayed battery level.");
        LinearLayout percentRow=new LinearLayout(this);
        percentRow.setGravity(Gravity.CENTER_VERTICAL);
        EditText percentInput=new EditText(this);
        styleInput(percentInput);
        percentInput.setHint("0–100");
        percentInput.setSingleLine(true);
        percentInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        percentRow.addView(percentInput,new LinearLayout.LayoutParams(0,-2,1));
        Button setPercent=primaryButton("Set");
        Button resetPercent=actionButton("Reset");
        LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(-2,-2);
        slp.leftMargin=dp(6);
        percentRow.addView(setPercent,slp);
        LinearLayout.LayoutParams rlp=new LinearLayout.LayoutParams(-2,-2);
        rlp.leftMargin=dp(6);
        percentRow.addView(resetPercent,rlp);
        percentBox.addView(percentRow,top(10));
        TextView percentStatus=text("Reset restores the real battery value.",12);
        percentStatus.setTextColor(0xFF858D99);
        percentBox.addView(percentStatus,top(4));
        r.addView(percentBox,top(12));
        
        setPercent.setOnClickListener(v->{
            String value=percentInput.getText().toString().trim();
            if(value.isEmpty()){percentStatus.setText("Enter a value from 0 to 100.");return;}
            final int requested;
            try{requested=Integer.parseInt(value);}
            catch(NumberFormatException e){percentStatus.setText("Enter a value from 0 to 100.");return;}
            setPercent.setEnabled(false);
            try{
                sensorExecutor.execute(()->{
                    BatteryPercentOverride.Result result;
                    try{result=BatteryPercentOverride.set(requested,
                            BatteryChargingOverride.powerInfoForAndroid(MainActivity.this,
                                    BatteryReader.getPowerInfo(MainActivity.this)));}
                    catch(Throwable error){result=new BatteryPercentOverride.Result(false,
                            error.getMessage()==null?"Root command failed.":error.getMessage());}
                    final BatteryPercentOverride.Result finalResult=result;
                    runOnUiThread(()->{
                        if (finalResult.success) {
                            getSharedPreferences(PREFS,0).edit()
                                    .putBoolean(BatteryChargingOverride.KEY_MANUAL_PERCENT_ENABLED,true)
                                    .putInt(BatteryChargingOverride.KEY_MANUAL_PERCENT,requested)
                                    .apply();
                        }
                        if(!isFinishing()&&!isDestroyed())percentStatus.setText(finalResult.message);
                        setPercent.setEnabled(true);
                    });
                });
            }catch(RejectedExecutionException ignored){setPercent.setEnabled(true);}
        });
        resetPercent.setOnClickListener(v->{
            resetPercent.setEnabled(false);
            try{
                sensorExecutor.execute(()->{
                    BatteryPercentOverride.Result result=BatteryChargingOverride.resetAndroidBattery(MainActivity.this);
                    runOnUiThread(()->{
                        if (result.success) {
                            getSharedPreferences(PREFS,0).edit()
                                    .putBoolean(BatteryChargingOverride.KEY_MANUAL_PERCENT_ENABLED,false)
                                    .remove(BatteryChargingOverride.KEY_MANUAL_PERCENT)
                                    .apply();
                        }
                        if(!isFinishing()&&!isDestroyed())percentStatus.setText(result.message);
                        resetPercent.setEnabled(true);
                    });
                });
            }catch(RejectedExecutionException ignored){resetPercent.setEnabled(true);}
        });
        
        notificationSwitch.setOnCheckedChangeListener((b,on)->{
            getSharedPreferences(PREFS,0).edit().putBoolean("notification_enabled",on).apply();
            if(on){
                startNotificationService();
                requestBatteryOptimizationExemption();
            }else{
                SessionStore.finish(this);
                android.content.SharedPreferences recordingPrefs=getSharedPreferences(PREFS,0);
                if(recordingPrefs.getBoolean(KEY_USE_ANDROID_PERCENT,false)
                        ||recordingPrefs.getBoolean(KEY_ANDROID_PERCENT_RESET_PENDING,false)
                        ||BatteryPercentCalculator.isRunning(this)){
                    startNotificationService();
                }else{
                    stopService(new Intent(this,BatteryNotificationService.class));
                }
            }
        });
        return r;
    }
    
    private void handleFakeChargingSwitch(boolean enabled){
        if(fakeChargingSwitch==null)return;
        fakeChargingSwitch.setEnabled(false);
        try{
            sensorExecutor.execute(()->{
                BatteryChargingOverride.Result result=
                        BatteryChargingOverride.setEnabled(MainActivity.this,enabled);
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    fakeChargingSwitch.setOnCheckedChangeListener(null);
                    fakeChargingSwitch.setChecked(result.enabled);
                    fakeChargingSwitch.setOnCheckedChangeListener(
                            (button,on)->handleFakeChargingSwitch(on));
                    fakeChargingStatus.setText(result.message);
                    fakeChargingStatus.setTextColor(result.success
                            ? 0xFF63E6A8 : 0xFFFF7F9A);
                    fakeChargingSwitch.setEnabled(true);
                });
            });
        }catch(RejectedExecutionException ignored){
            fakeChargingSwitch.setEnabled(true);
            fakeChargingSwitch.setOnCheckedChangeListener(null);
            fakeChargingSwitch.setChecked(BatteryChargingOverride.isEnabled(this));
            fakeChargingSwitch.setOnCheckedChangeListener(
                    (button,on)->handleFakeChargingSwitch(on));
        }
    }

    private void runFuelGaugeDaemonCommand(boolean disable,Button disableButton,
                                           Button enableButton,TextView status){
        disableButton.setEnabled(false);
        enableButton.setEnabled(false);
        try{
            sensorExecutor.execute(()->{
                BatteryChargingOverride.Result result=
                        BatteryChargingOverride.setFuelGaugeDaemonDisabled(disable);
                runOnUiThread(()->{
                    if(!isFinishing()&&!isDestroyed()){
                        status.setText(result.message);
                        status.setTextColor(result.success?0xFF63E6A8:0xFFFF7F9A);
                    }
                    disableButton.setEnabled(true);
                    enableButton.setEnabled(true);
                });
            });
        }catch(RejectedExecutionException ignored){
            disableButton.setEnabled(true);
            enableButton.setEnabled(true);
            status.setText("Root command could not be queued.");
        }
    }

    private LinearLayout buildMyBattery(){
        LinearLayout r=new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(dp(16),dp(4),dp(16),dp(20));

        TextView title=text("My Battery",22);
        title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        r.addView(title,full());

        LinearLayout intervalCard=card();
        myBatterySamplingLabel=text("My Battery calculation interval  •  0.50 s",13);
        myBatterySamplingLabel.setTextColor(0xFF9AA0AA);
        intervalCard.addView(myBatterySamplingLabel,full());
        myBatterySamplingBar=new SeekBar(this);
        myBatterySamplingBar.setMax((int)((MAX_MY_BATTERY_INTERVAL_MS-MIN_MY_BATTERY_INTERVAL_MS)/MY_BATTERY_INTERVAL_STEP_MS));
        intervalCard.addView(myBatterySamplingBar,new LinearLayout.LayoutParams(-1,-2));
        android.content.SharedPreferences batteryPrefs=getSharedPreferences(PREFS,0);
        long savedBatteryInterval=batteryPrefs.getLong(KEY_MY_BATTERY_INTERVAL_MS,DEFAULT_MY_BATTERY_INTERVAL_MS);
        if(savedBatteryInterval<MIN_MY_BATTERY_INTERVAL_MS||savedBatteryInterval>MAX_MY_BATTERY_INTERVAL_MS)
            savedBatteryInterval=DEFAULT_MY_BATTERY_INTERVAL_MS;
        myBatterySamplingBar.setProgress((int)((savedBatteryInterval-MIN_MY_BATTERY_INTERVAL_MS)/MY_BATTERY_INTERVAL_STEP_MS));
        myBatterySamplingLabel.setText(formatMyBatteryInterval(savedBatteryInterval));
        myBatterySamplingBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int progress,boolean fromUser){
                long interval=MIN_MY_BATTERY_INTERVAL_MS+progress*MY_BATTERY_INTERVAL_STEP_MS;
                myBatterySamplingLabel.setText(formatMyBatteryInterval(interval));
                if(fromUser)getSharedPreferences(PREFS,0).edit().putLong(KEY_MY_BATTERY_INTERVAL_MS,interval).apply();
            }
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){}
        });
        r.addView(intervalCard,top(10));

        LinearLayout calculator=card(0xFF17271F,0xFF63E6A8);
        TextView calculatorTitle=text("Current estimate",16);
        calculatorTitle.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        calculator.addView(calculatorTitle,full());
        calculatedPercent=text("--",34);
        calculatedPercent.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        calculatedPercent.setTextColor(0xFF63E6A8);
        calculator.addView(calculatedPercent,top(4));
        myBatteryCurrentLabel=text("Current: N/A",12);
        myBatteryCurrentLabel.setTextColor(0xFF9AA0AA);
        calculator.addView(myBatteryCurrentLabel,top(1));
        myBatteryRateLabel=text("Charging rate: --%/h\nDischarging rate: --%/h",12);
        myBatteryRateLabel.setTextColor(0xFF9AA0AA);
        calculator.addView(myBatteryRateLabel,top(3));
        TextView rateAverageHint=text("1-minute average • updated every minute",11);
        rateAverageHint.setTextColor(0xFF7F8B9B);
        calculator.addView(rateAverageHint,top(2));
        calculatedMah=text("Remaining charge: enter capacity and starting mAh",13);
        calculatedMah.setTextColor(0xFFD2D8E0);
        calculator.addView(calculatedMah,top(2));
        TextView capacityLabel=text("Full usable battery capacity (mAh)",13);
        capacityLabel.setTextColor(0xFFD2D8E0);
        calculator.addView(capacityLabel,top(12));
        customCapacityInput=new EditText(this);
        styleInput(customCapacityInput);
        customCapacityInput.setSingleLine(true);
        customCapacityInput.setHint("e.g. 4050");
        customCapacityInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        double savedCapacity=BatteryPercentCalculator.getCustomCapacityMah(this);
        if(!Double.isNaN(savedCapacity)&&!Double.isInfinite(savedCapacity)&&savedCapacity>=0){
            customCapacityInput.setText(String.format(java.util.Locale.US,"%.2f",savedCapacity)
                    .replaceAll("\\.?0+$",""));
        }
        calculator.addView(customCapacityInput,top(3));

        TextView startingLabel=text("Starting remaining charge (mAh)",13);
        startingLabel.setTextColor(0xFFD2D8E0);
        calculator.addView(startingLabel,top(10));
        startingChargeInput=new EditText(this);
        styleInput(startingChargeInput);
        startingChargeInput.setSingleLine(true);
        startingChargeInput.setHint("Your estimate, e.g. 1800");
        startingChargeInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        double savedStarting=BatteryPercentCalculator.getStartingChargeMah(this);
        if(!Double.isNaN(savedStarting)&&!Double.isInfinite(savedStarting)&&savedStarting>=0){
            startingChargeInput.setText(String.format(java.util.Locale.US,"%.2f",savedStarting)
                    .replaceAll("\\.?0+$",""));
        }
        calculator.addView(startingChargeInput,top(3));

        LinearLayout actions=new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        Button saveStart=primaryButton("Save & start");
        Button stopMeasurement=dangerButton("Stop");
        actions.addView(saveStart,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout.LayoutParams stopParams=new LinearLayout.LayoutParams(0,-2,1);
        stopParams.leftMargin=dp(8);
        actions.addView(stopMeasurement,stopParams);
        calculator.addView(actions,top(10));
        Button clearAll=dangerButton("Clear all");
        calculator.addView(clearAll,top(6));
        useAndroidPercentButton=primaryButton("Use this as Android %");
        calculator.addView(useAndroidPercentButton,top(6));
        androidPercentStatus=text("",12);
        androidPercentStatus.setTextColor(0xFF9AA0AA);
        calculator.addView(androidPercentStatus,top(3));
        boolean usingAppPercent=getSharedPreferences(PREFS,0).getBoolean(KEY_USE_ANDROID_PERCENT,false);
        useAndroidPercentButton.setText(usingAppPercent?"Stop using app percentage":"Use this as Android %");
        if(usingAppPercent)androidPercentStatus.setText("Syncing Android battery level with My Battery.");
        saveStart.setOnClickListener(v->startCalculatorFromInputs());
        stopMeasurement.setOnClickListener(v->stopMeasurement());
        clearAll.setOnClickListener(v->clearAllMeasurementData());
        useAndroidPercentButton.setOnClickListener(v->toggleAndroidPercentage());
        r.addView(calculator,top(12));
        return r;
    }

    private LinearLayout buildHealth(){
        LinearLayout r=new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(dp(16),dp(4),dp(16),dp(20));
        TextView title=text("Battery health",22); title.setTypeface(Typeface.DEFAULT,Typeface.BOLD); r.addView(title,full());
        TextView sub=text("Standard Android battery properties and Linux fuel-gauge data.",13); sub.setTextColor(0xFF707070); r.addView(sub,top(2));
        LinearLayout overview=card(0xFF201B2B,0xFFB58CFF);
        healthLevel=text("--%",30); healthLevel.setTypeface(Typeface.DEFAULT,Typeface.BOLD); overview.addView(healthLevel,full());
        healthStatus=text("Status: --",14); healthStatus.setTextColor(0xFF9AA0AA); overview.addView(healthStatus,top(4));
        r.addView(overview,top(12));
        addHealthField(r,"Full charge capacity","healthFullCharge");
        addHealthField(r,"Remaining charge counter","healthRemaining");
        addHealthField(r,"Design capacity","healthDesign");
        addHealthField(r,"Capacity ratio","healthRatio");
        addHealthField(r,"Raw health source","healthRaw");
        addHealthField(r,"Source file","healthFile");
        addHealthField(r,"Full energy","healthFullEnergy");
        addHealthField(r,"Design energy","healthDesignEnergy");
        addHealthField(r,"Session-based capacity estimate (≥40% charge)","healthSessionEstimate");
        addHealthField(r,"Session-based health estimate","healthSessionHealth");
        LinearLayout row1=new LinearLayout(this);
        LinearLayout voltageCard=infoCard("VOLTAGE","--"); healthVoltage=(TextView)voltageCard.getTag();
        LinearLayout tempCard=infoCard("TEMPERATURE","--"); healthTemp=(TextView)tempCard.getTag();
        row1.addView(voltageCard,new LinearLayout.LayoutParams(0,-2,1)); LinearLayout.LayoutParams x=new LinearLayout.LayoutParams(0,-2,1); x.leftMargin=dp(8); row1.addView(tempCard,x);
        r.addView(row1,top(12));
        LinearLayout row2=new LinearLayout(this);
        LinearLayout currentCard=infoCard("CURRENT","--"); healthCurrent=(TextView)currentCard.getTag();
        LinearLayout sourceCard=infoCard("LIVE SOURCE","--"); healthSource=(TextView)sourceCard.getTag();
        row2.addView(currentCard,new LinearLayout.LayoutParams(0,-2,1)); x=new LinearLayout.LayoutParams(0,-2,1); x.leftMargin=dp(8); row2.addView(sourceCard,x);
        r.addView(row2,top(8));
        TextView note=text("BatteryPulse uses standard Android battery APIs and Linux power_supply data, with root access used to fill protected kernel values when available.",12); note.setTextColor(0xFF858D99); r.addView(note,top(12));
        return r;
    }
    private void addHealthField(LinearLayout parent,String label,String key){
        int bgColor=0xFF171A20;
        int strokeColor=0xFF303744;
        if("healthFullCharge".equals(key)) { bgColor=0xFF172334; strokeColor=0xFF63B3FF; }
        else if("healthRemaining".equals(key)) { bgColor=0xFF17271F; strokeColor=0xFF63E6A8; }
        else if("healthDesign".equals(key)) { bgColor=0xFF211E18; strokeColor=0xFFFFC86B; }
        else if("healthRatio".equals(key)) { bgColor=0xFF201B2B; strokeColor=0xFFB58CFF; }
        else if("healthRaw".equals(key)) { bgColor=0xFF261A20; strokeColor=0xFFFF7F9A; }
        else if("healthFile".equals(key)) { bgColor=0xFF18242A; strokeColor=0xFF69C7D8; }
        else if("healthFullEnergy".equals(key)||"healthDesignEnergy".equals(key)) { bgColor=0xFF182334; strokeColor=0xFF7CC7FF; }
        else if("healthSessionEstimate".equals(key)||"healthSessionHealth".equals(key)) { bgColor=0xFF17271F; strokeColor=0xFF63E6A8; }
        LinearLayout c=card(bgColor,strokeColor);
        TextView l=text(label,11); l.setTypeface(Typeface.DEFAULT,Typeface.BOLD); l.setTextColor(0xFF9AA0AA); c.addView(l,full());
        TextView v=text("Unavailable",16); v.setTypeface(Typeface.DEFAULT,Typeface.BOLD); c.addView(v,top(4));
        if("healthFullCharge".equals(key)) healthFullCharge=v;
        else if("healthRemaining".equals(key)) healthRemaining=v;
        else if("healthDesign".equals(key)) healthDesign=v;
        else if("healthRatio".equals(key)) healthRatio=v;
        else if("healthRaw".equals(key)) healthRaw=v;
        else if("healthFile".equals(key)) healthFile=v;
        else if("healthFullEnergy".equals(key)) healthFullEnergy=v;
        else if("healthDesignEnergy".equals(key)) healthDesignEnergy=v;
        else if("healthSessionEstimate".equals(key)) healthSessionEstimate=v;
        else if("healthSessionHealth".equals(key)) healthSessionHealth=v;
        parent.addView(c,top(7));
    }
    private LinearLayout infoCard(String label,String value){
        LinearLayout c=card(0xFF171A20,0xFF303744); TextView l=text(label,10); l.setTypeface(Typeface.DEFAULT,Typeface.BOLD); l.setTextColor(0xFF8E96A3); c.addView(l,full());
        TextView v=text(value,17); v.setTypeface(Typeface.DEFAULT,Typeface.BOLD); c.setTag(v); c.addView(v,top(5)); return c;
    }
    private void showTab(int index){
        monitorContainer.setVisibility(index==0?View.VISIBLE:View.GONE);
        myBatteryScroll.setVisibility(index==1?View.VISIBLE:View.GONE);
        historyView.setVisibility(index==2?View.VISIBLE:View.GONE);
        healthScroll.setVisibility(index==3?View.VISIBLE:View.GONE);
        selectTab(index);
        handler.removeCallbacks(historyUpdater);
        if(index==1) updateMyBatteryEstimate();
        if(index==2){refreshHistory();handler.postDelayed(historyUpdater,10000);}
        else if(index==3) refreshHealth();
    }
    private static final class HealthSnapshot {
        String level="--";
        String status="Status: unavailable";
        String voltage="N/A";
        String temperature="N/A";
        String current="N/A";
        String source="Unavailable";
        String fullCharge="Unavailable";
        String remaining="Unavailable";
        String design="Unavailable";
        String ratio="Unavailable";
        String raw="Unavailable";
        String file="Unavailable";
        String fullEnergy="Unavailable";
        String designEnergy="Unavailable";
        String sessionEstimate="Unavailable";
        String sessionHealth="Unavailable";
    }

    private void refreshHealth(){
        if(!healthReadInFlight.compareAndSet(false,true))return;
        try{
            sensorExecutor.execute(()->{
                HealthSnapshot snapshot=new HealthSnapshot();
                try{
                    android.content.Intent b=registerReceiver(null,
                            new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED));
                    if(b!=null){
                        int level=b.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL,-1);
                        int scale=b.getIntExtra(android.os.BatteryManager.EXTRA_SCALE,100);
                        int pct=(level>=0&&scale>0)?Math.round(level*100f/scale):-1;
                        snapshot.level=pct>=0?pct+"%":"--";
                        int status=b.getIntExtra(android.os.BatteryManager.EXTRA_STATUS,-1);
                        snapshot.status="Status: "+(status==android.os.BatteryManager.BATTERY_STATUS_CHARGING
                                ?"Charging":status==android.os.BatteryManager.BATTERY_STATUS_FULL
                                ?"Full":"Discharging / idle");
                        int mv=b.getIntExtra(android.os.BatteryManager.EXTRA_VOLTAGE,0);
                        snapshot.voltage=mv>0?String.format(java.util.Locale.US,"%.3f V",mv/1000.0):"N/A";
                        int temp=b.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE,0);
                        snapshot.temperature=temp!=0?String.format(java.util.Locale.US,"%.1f °C",temp/10.0):"N/A";
                    }
                    BatteryReader.Reading reading=BatteryReader.read(MainActivity.this);
                    snapshot.current=reading.currentText();
                    snapshot.source=reading.currentUa!=Long.MIN_VALUE?"Fuel gauge":"Android";
                    BatteryHealthReader.Data data=BatteryHealthReader.read(MainActivity.this);
                    snapshot.fullCharge=data.charge(data.fullChargeUaH);
                    snapshot.remaining=data.charge(data.remainingChargeUaH);
                    snapshot.design=data.charge(data.designChargeUaH);
                    snapshot.ratio=Double.isNaN(data.ratio)?"Unavailable"
                            :String.format(java.util.Locale.US,"%.1f %%",data.ratio);
                    snapshot.raw=data.rawHealth==null?"Unavailable":data.rawHealth;
                    snapshot.file=data.sourceFile;
                    snapshot.fullEnergy=data.energy(data.fullEnergyUWh);
                    snapshot.designEnergy=data.energy(data.designEnergyUWh);
                    SessionEstimate estimate=estimateFromSessions();
                    snapshot.sessionEstimate=estimate.capacityMah>0
                            ?String.format(java.util.Locale.US,"%.0f mAh",estimate.capacityMah)
                            :"Unavailable";
                    snapshot.sessionHealth=estimate.health>0
                            ?String.format(java.util.Locale.US,"%.1f %%  •  %d qualifying sessions",
                                    estimate.health,estimate.count)
                            :"Unavailable";
                }catch(Throwable ignored){
                    // Return whatever was read; a later refresh retries missing fields.
                }
                final HealthSnapshot result=snapshot;
                runOnUiThread(()->{
                    try{
                        if(isFinishing()||isDestroyed())return;
                        healthLevel.setText(result.level);
                        healthStatus.setText(result.status);
                        healthVoltage.setText(result.voltage);
                        healthTemp.setText(result.temperature);
                        healthCurrent.setText(result.current);
                        healthSource.setText(result.source);
                        healthFullCharge.setText(result.fullCharge);
                        healthRemaining.setText(result.remaining);
                        healthDesign.setText(result.design);
                        healthRatio.setText(result.ratio);
                        healthRaw.setText(result.raw);
                        healthFile.setText(result.file);
                        healthFullEnergy.setText(result.fullEnergy);
                        healthDesignEnergy.setText(result.designEnergy);
                        healthSessionEstimate.setText(result.sessionEstimate);
                        healthSessionHealth.setText(result.sessionHealth);
                    }finally{
                        healthReadInFlight.set(false);
                    }
                });
            });
        }catch(RejectedExecutionException ignored){
            healthReadInFlight.set(false);
        }
    }

    private SessionEstimate estimateFromSessions(){
        ArrayList<SessionStore.Record> records=SessionStore.getCapacityRecords(this);
        double weightedCapacity=0;
        double totalWeight=0;
        int count=0;
        for(SessionStore.Record rec:records){
            if(!rec.charging || rec.startPercent<0 || rec.endPercent<0) continue;
            int delta=rec.endPercent-rec.startPercent;
            if(delta<40 || rec.mah<=0) continue;
            double capacity=rec.mah*100.0/delta;
            if(capacity<1500 || capacity>6000) continue;
            double weight=Math.min(delta,100);
            weightedCapacity+=capacity*weight;
            totalWeight+=weight;
            count++;
        }
        if(totalWeight<=0) return new SessionEstimate(-1, -1, 0);
        double cap=weightedCapacity/totalWeight;
        return new SessionEstimate(cap,cap*100.0/4050.0,count);
    }

    private static final class SessionEstimate{
        final double capacityMah,health;
        final int count;
        SessionEstimate(double c,double h,int n){capacityMah=c;health=h;count=n;}
    }

    private LinearLayout buildHistory(){
        LinearLayout r=new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(dp(16),dp(4),dp(16),dp(16));
        TextView title=text("Session history",22);
        title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        r.addView(title,full());
        TextView sub=text("Measured energy use across recording sessions.",13);
        sub.setTextColor(0xFF707070);
        r.addView(sub,top(2));
        Button clear=dangerButton("Clear history");
        r.addView(clear,top(10));
        clear.setOnClickListener(v->{SessionStore.clear(this);refreshHistory();});
        ScrollView s=new ScrollView(this);
        historyList=new LinearLayout(this);
        historyList.setOrientation(LinearLayout.VERTICAL);
        s.addView(historyList);
        r.addView(s,new LinearLayout.LayoutParams(-1,0,1));
        return r;
    }
    
    private final Runnable historyUpdater=new Runnable(){public void run(){if(historyView!=null&&historyView.getVisibility()==View.VISIBLE){refreshHistory();handler.postDelayed(this,10000);}}};
    
    @Override protected void onResume(){
        super.onResume();
        updateValues();
        updateMyBatteryEstimate();
        handler.removeCallbacks(updater);
        handler.post(updater);
        handler.removeCallbacks(myBatteryUpdater);
        handler.post(myBatteryUpdater);
        handler.removeCallbacks(historyUpdater);
        if(historyView!=null&&historyView.getVisibility()==View.VISIBLE){refreshHistory();handler.postDelayed(historyUpdater,10000);}
        if(healthView!=null&&healthView.getVisibility()==View.VISIBLE)refreshHealth();
    }
    
    @Override protected void onPause(){
        handler.removeCallbacks(updater);
        handler.removeCallbacks(myBatteryUpdater);
        handler.removeCallbacks(historyUpdater);
        super.onPause();
    }

    @Override protected void onDestroy(){
        handler.removeCallbacks(updater);
        handler.removeCallbacks(myBatteryUpdater);
        handler.removeCallbacks(historyUpdater);
        sensorExecutor.shutdownNow();
        super.onDestroy();
    }
    
    private void updateValues(){
        if(monitorContainer!=null&&monitorContainer.getVisibility()!=View.VISIBLE)return;
        if(!monitorReadInFlight.compareAndSet(false,true))return;
        try{
            sensorExecutor.execute(()->{
                BatteryReader.Reading reading=null;
                try{reading=BatteryReader.read(MainActivity.this);}catch(Throwable ignored){}
                final BatteryReader.Reading result=reading;
                try{
                    runOnUiThread(()->{
                        if(isFinishing()||isDestroyed()||voltage==null||current==null)return;
                        if(result==null){
                            voltage.setText("N/A");
                            current.setText("N/A");
                        }else{
                            voltage.setText(result.voltageText());
                            current.setText(result.currentText());
                        }
                    });
                }finally{
                    monitorReadInFlight.set(false);
                }
            });
        }catch(RejectedExecutionException ignored){
            monitorReadInFlight.set(false);
        }
    }

    private void updateMyBatteryEstimate(){
        if(myBatteryScroll!=null&&myBatteryScroll.getVisibility()!=View.VISIBLE)return;
        if(!myBatteryReadInFlight.compareAndSet(false,true))return;
        try{
            sensorExecutor.execute(()->{
                BatteryReader.Reading reading=null;
                BatteryReader.PowerState powerState=BatteryReader.PowerState.UNKNOWN;
                BatteryPercentCalculator.Estimate estimate=BatteryPercentCalculator.current(MainActivity.this);
                try{
                    reading=BatteryReader.read(MainActivity.this);
                    powerState=BatteryReader.getPowerState(MainActivity.this);
                    android.content.SharedPreferences prefs=getSharedPreferences(PREFS,0);
                    boolean serviceActive=prefs.getBoolean("notification_enabled",false)
                            ||prefs.getBoolean(KEY_USE_ANDROID_PERCENT,false)
                            ||prefs.getBoolean(KEY_ANDROID_PERCENT_RESET_PENDING,false)
                            ||BatteryPercentCalculator.isRunning(MainActivity.this);
                    estimate=serviceActive
                            ?BatteryPercentCalculator.current(MainActivity.this)
                            :BatteryPercentCalculator.sample(MainActivity.this,reading);
                }catch(Throwable ignored){
                    estimate=BatteryPercentCalculator.current(MainActivity.this);
                }
                final BatteryReader.Reading readingResult=reading;
                final BatteryReader.PowerState powerStateResult=powerState;
                final BatteryPercentCalculator.Estimate estimateResult=estimate;
                try{
                    runOnUiThread(()->{
                        if(isFinishing()||isDestroyed())return;
                        if(myBatteryCurrentLabel!=null){
                            myBatteryCurrentLabel.setText("Current: "
                                    +(readingResult==null?"N/A":readingResult.currentText()));
                        }
                        updateBatteryRates(readingResult,estimateResult,powerStateResult);
                        updateCalculatedLevel(estimateResult);
                    });
                }finally{
                    myBatteryReadInFlight.set(false);
                }
            });
        }catch(RejectedExecutionException ignored){
            myBatteryReadInFlight.set(false);
        }
    }

    private void updateBatteryRates(BatteryReader.Reading reading,
                                    BatteryPercentCalculator.Estimate estimate,
                                    BatteryReader.PowerState state){
        if(myBatteryRateLabel==null)return;
        String chargingRate="--";
        String dischargingRate="--";
        double capacityMah=estimate==null?Double.NaN:estimate.capacityMah;
        if(Double.isNaN(capacityMah)||Double.isInfinite(capacityMah)||capacityMah<100.0){
            capacityMah=BatteryPercentCalculator.getCustomCapacityMah(this);
        }
        // The signed rate follows actual current, not Android's charger-connected state.
        double rate=BatteryRateAverager.ratePerHour(reading,capacityMah);
        boolean validRate=!Double.isNaN(rate)&&!Double.isInfinite(rate);
        if(state==BatteryReader.PowerState.CHARGING){
            if(validRate)chargingRate=String.format(java.util.Locale.US,"%+.2f",rate);
        }else if(state==BatteryReader.PowerState.DISCHARGING
                ||state==BatteryReader.PowerState.NOT_CHARGING){
            if(validRate)dischargingRate=String.format(java.util.Locale.US,"%+.2f",rate);
        }else if(state==BatteryReader.PowerState.FULL){
            if(validRate)chargingRate=String.format(java.util.Locale.US,"%+.2f",rate);
        }else if(validRate){
            if(rate>=0.0)chargingRate=String.format(java.util.Locale.US,"%+.2f",rate);
            else dischargingRate=String.format(java.util.Locale.US,"%+.2f",rate);
        }
        myBatteryRateLabel.setText("Charging rate: "+chargingRate+"%/h\n"
                +"Discharging rate: "+dischargingRate+"%/h");
    }

    private void updateCalculatedLevel(BatteryPercentCalculator.Estimate estimate){
        if(calculatedPercent==null||calculatedMah==null)return;
        if(estimate==null||!estimate.isAvailable()){
            calculatedPercent.setText("--");
            calculatedMah.setText(estimate!=null&&estimate.capacityMah==0.0
                    ? "Remaining charge: 0 mAh" : "Remaining charge: -- mAh");
            return;
        }
        calculatedPercent.setText(String.format(java.util.Locale.US,"%.2f%%",estimate.percentExact));
        calculatedMah.setText(String.format(java.util.Locale.US,
                "Remaining charge: %.1f / %.0f mAh",estimate.remainingMah,estimate.capacityMah));
    }

    private void startCalculatorFromInputs(){
        if(customCapacityInput==null||startingChargeInput==null)return;
        String capacityText=customCapacityInput.getText().toString().trim();
        String startingText=startingChargeInput.getText().toString().trim();
        if(capacityText.isEmpty()){
            Toast.makeText(this,"Enter full usable capacity in mAh.",Toast.LENGTH_SHORT).show();
            return;
        }
        if(startingText.isEmpty()){
            Toast.makeText(this,"Enter starting remaining charge in mAh.",Toast.LENGTH_SHORT).show();
            return;
        }
        try{
            double capacity=Double.parseDouble(capacityText);
            double starting=Double.parseDouble(startingText);
            if(Double.isNaN(capacity)||Double.isInfinite(capacity)||capacity<100.0||capacity>100000.0){
                Toast.makeText(this,"Capacity must be between 100 and 100000 mAh.",Toast.LENGTH_SHORT).show();
                return;
            }
            if(Double.isNaN(starting)||Double.isInfinite(starting)||starting<0.0||starting>capacity){
                Toast.makeText(this,"Starting charge must be between 0 and the capacity.",Toast.LENGTH_SHORT).show();
                return;
            }
            if(!calculatorStartInFlight.compareAndSet(false,true))return;
            try{
                sensorExecutor.execute(()->{
                    BatteryPercentCalculator.Estimate estimate;
                    try{
                        estimate=BatteryPercentCalculator.startEstimate(MainActivity.this,capacity,starting,
                                BatteryReader.read(MainActivity.this));
                    }catch(Throwable ignored){
                        estimate=BatteryPercentCalculator.current(MainActivity.this);
                    }
                    final BatteryPercentCalculator.Estimate result=estimate;
                    runOnUiThread(()->{
                        try{
                            if(isFinishing()||isDestroyed())return;
                            updateCalculatedLevel(result);
                            if(result.isAvailable()&&result.running){
                                startNotificationService();
                                requestBatteryOptimizationExemption();
                            }
                        }finally{
                            calculatorStartInFlight.set(false);
                        }
                    });
                });
            }catch(RejectedExecutionException ignored){
                calculatorStartInFlight.set(false);
            }
        }catch(NumberFormatException e){
            Toast.makeText(this,"Enter valid numeric mAh values.",Toast.LENGTH_SHORT).show();
        }
    }

    private void stopMeasurement(){
        BatteryPercentCalculator.stopMeasurement(this);
        updateCalculatedLevel(BatteryPercentCalculator.current(this));
        disableAndroidPercentage("Measurement stopped; restoring Android battery level.");
    }

    private void clearAllMeasurementData(){
        // My Battery owns only its estimator values. Session history and background recording
        // are intentionally not touched by this button.
        BatteryPercentCalculator.clearAll(this);
        if(customCapacityInput!=null)customCapacityInput.setText("0");
        if(startingChargeInput!=null)startingChargeInput.setText("0");
        updateCalculatedLevel(BatteryPercentCalculator.current(this));
        disableAndroidPercentage("My Battery values cleared; restoring Android battery level.");
    }

    private void toggleAndroidPercentage(){
        android.content.SharedPreferences prefs=getSharedPreferences(PREFS,0);
        if(prefs.getBoolean(KEY_USE_ANDROID_PERCENT,false)){
            disableAndroidPercentage("Restoring the real Android battery level.");
            return;
        }
        BatteryPercentCalculator.Estimate estimate=BatteryPercentCalculator.current(this);
        if(estimate==null||!estimate.isAvailable()||!estimate.running){
            Toast.makeText(this,"Start My Battery measurement first.",Toast.LENGTH_SHORT).show();
            return;
        }
        if(useAndroidPercentButton!=null)useAndroidPercentButton.setEnabled(false);
        if(androidPercentStatus!=null)androidPercentStatus.setText("Applying calculated percentage with root…");
        final int percent=estimate.percent;
        new Thread(()->{
            final BatteryReader.PowerInfo powerInfo=BatteryChargingOverride.powerInfoForAndroid(
                    MainActivity.this,BatteryReader.getPowerInfo(MainActivity.this));
            BatteryPercentOverride.Result result=BatteryPercentOverride.set(percent,powerInfo);
            runOnUiThread(()->{
                if(result.success){
                    getSharedPreferences(PREFS,0).edit()
                            .putBoolean(KEY_USE_ANDROID_PERCENT,true)
                            .putBoolean(KEY_ANDROID_PERCENT_RESET_PENDING,false)
                            .putInt(KEY_LAST_ANDROID_PERCENT,percent)
                            .putInt(KEY_LAST_ANDROID_POWER_STATUS,powerInfo.androidStatus)
                            .putInt(KEY_LAST_ANDROID_POWER_SOURCE,powerInfo.sourceMask())
                            .putBoolean(BatteryChargingOverride.KEY_MANUAL_PERCENT_ENABLED,false)
                            .remove(BatteryChargingOverride.KEY_MANUAL_PERCENT)
                            .apply();
                    if(useAndroidPercentButton!=null)useAndroidPercentButton.setText("Stop using app percentage");
                    if(androidPercentStatus!=null)androidPercentStatus.setText("Android battery level: "+percent+"%");
                    startNotificationService();
                    requestBatteryOptimizationExemption();
                }else if(androidPercentStatus!=null){
                    androidPercentStatus.setText(result.message);
                }
                if(useAndroidPercentButton!=null)useAndroidPercentButton.setEnabled(true);
            });
        }).start();
    }

    private void disableAndroidPercentage(String message){
        android.content.SharedPreferences prefs=getSharedPreferences(PREFS,0);
        boolean hadOverride=prefs.getBoolean(KEY_USE_ANDROID_PERCENT,false)
                ||prefs.getBoolean(KEY_ANDROID_PERCENT_RESET_PENDING,false)
                ||prefs.contains(KEY_LAST_ANDROID_PERCENT);
        if(!hadOverride){
            if(useAndroidPercentButton!=null)useAndroidPercentButton.setText("Use this as Android %");
            if(androidPercentStatus!=null)androidPercentStatus.setText("");
            return;
        }
        prefs.edit().putBoolean(KEY_USE_ANDROID_PERCENT,false)
                .putBoolean(KEY_ANDROID_PERCENT_RESET_PENDING,true).apply();
        if(useAndroidPercentButton!=null)useAndroidPercentButton.setText("Use this as Android %");
        if(androidPercentStatus!=null)androidPercentStatus.setText(message);
        // The foreground service performs the root reset off the UI thread and retries failures.
        startNotificationService();
    }

    private void requestBatteryOptimizationExemption(){
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.M)return;
        try{
            PowerManager manager=(PowerManager)getSystemService(POWER_SERVICE);
            if(manager!=null&&!manager.isIgnoringBatteryOptimizations(getPackageName())){
                Intent request=new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:"+getPackageName()));
                startActivity(request);
            }
        }catch(Exception ignored){
            try{startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));}
            catch(Exception ignoredAgain){}
        }
    }

    private long currentRecordingIntervalMs(){
        long value=getSharedPreferences(PREFS,0).getLong(KEY_INTERVAL_MS,DEFAULT_RECORDING_INTERVAL_MS);
        return value<MIN_RECORDING_INTERVAL_MS||value>MAX_RECORDING_INTERVAL_MS
                ?DEFAULT_RECORDING_INTERVAL_MS:value;
    }

    private String formatRecordingInterval(long ms){
        return String.format(java.util.Locale.US,"Current recording interval  •  %.2f s",ms/1000.0);
    }

    private long currentMyBatteryIntervalMs(){
        long value=getSharedPreferences(PREFS,0).getLong(KEY_MY_BATTERY_INTERVAL_MS,DEFAULT_MY_BATTERY_INTERVAL_MS);
        return value<MIN_MY_BATTERY_INTERVAL_MS||value>MAX_MY_BATTERY_INTERVAL_MS
                ?DEFAULT_MY_BATTERY_INTERVAL_MS:value;
    }

    private String formatMyBatteryInterval(long ms){
        return String.format(java.util.Locale.US,"My Battery calculation interval  •  %.2f s",ms/1000.0);
    }

    private void refreshHistory(){
        historyList.removeAllViews();
        ArrayList<SessionStore.Record> rs=SessionStore.getRecords(this);
        if(rs.isEmpty()){
            TextView empty=text("No sessions yet",16);
            empty.setTextColor(0xFF8E96A3);
            historyList.addView(empty,top(24));
            return;
        }
        for(SessionStore.Record rec:rs){
            LinearLayout card=card();
            TextView title=text((rec.charging?"CHARGED":"USED")+"  "+String.format(java.util.Locale.US,"%.1f mAh",rec.mah),18);
            title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
            card.addView(title,full());
            TextView details=text(rec.date()+"  •  "+rec.duration()+"  •  Screen ON: "+rec.screenOnDuration(),13);
            details.setTextColor(0xFF9AA0AA);
            card.addView(details,top(3));
            String currentRange=Double.isNaN(rec.minCurrent)?"Current: --":"Current: min "+formatSigned(rec.minCurrent)+" mA  •  max "+formatSigned(rec.maxCurrent)+" mA";
            String percentRange=(rec.startPercent>=0&&rec.endPercent>=0)?"Battery: "+rec.startPercent+"% → "+rec.endPercent+"%  ("+String.format(java.util.Locale.US,"%+d%%",rec.endPercent-rec.startPercent)+")":"Battery: --";
            TextView overall=text("Overall",12);
            overall.setTextColor(0xFF8E96A3);
            card.addView(overall,top(12));
            card.addView(text(currentRange,13),top(3));
            card.addView(text(percentRange,13),top(3));
            
            LinearLayout expanded=new LinearLayout(this);
            expanded.setOrientation(LinearLayout.VERTICAL);
            expanded.setVisibility(View.GONE);
            addScreenDetails(expanded,"Screen ON",rec.onMs,rec.onMah,rec.onMin,rec.onMax,rec.onStartPercent,rec.onEndPercent);
            addScreenDetails(expanded,"Screen OFF",rec.offMs,rec.offMah,rec.offMin,rec.offMax,rec.offStartPercent,rec.offEndPercent);
            card.addView(expanded,top(10));
            TextView hint=text("Tap for screen details",11);
            hint.setTextColor(0xFF858D99);
            card.addView(hint,top(8));
            card.setOnClickListener(v->{
                boolean show=expanded.getVisibility()!=View.VISIBLE;
                expanded.setVisibility(show?View.VISIBLE:View.GONE);
                hint.setText(show?"Tap to collapse":"Tap for screen details");
            });
            LinearLayout.LayoutParams cp=full();
            cp.topMargin=dp(10);
            historyList.addView(card,cp);
        }
    }
    
    private void addScreenDetails(LinearLayout parent,String label,long ms,double mah,double min,double max,int startPercent,int endPercent){
        TextView heading=text(label,14);
        heading.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        parent.addView(heading,top(4));
        if(ms<=0&&mah<=0){
            TextView none=text("No samples",12);
            none.setTextColor(0xFF858D99);
            parent.addView(none,top(2));
            return;
        }
        long minutes=Math.max(1,ms/60000L);
        String duration=minutes>=60?(minutes/60)+"h "+(minutes%60)+"m":minutes+"m";
        parent.addView(text("Time: "+duration+"  •  mAh: "+String.format(java.util.Locale.US,"%.1f",mah),12),top(2));
        if(Double.isNaN(min)||Double.isNaN(max))parent.addView(text("Current: --",12),top(2));
        else parent.addView(text("Current: min "+formatSigned(min)+" mA  •  max "+formatSigned(max)+" mA",12),top(2));
        if(startPercent>=0&&endPercent>=0)parent.addView(text("Battery: "+startPercent+"% → "+endPercent+"%  ("+String.format(java.util.Locale.US,"%+d%%",endPercent-startPercent)+")",12),top(2));
    }
    
    private LinearLayout metricCard(String label,String value,int color,int strokeColor){
        LinearLayout c=card(color,strokeColor);
        TextView l=text(label,11);
        l.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        l.setTextColor(0xFF777777);
        c.addView(l,full());
        TextView v=text(value,25);
        v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        c.setTag(v);
        c.addView(v,top(5));
        return c;
    }
    
    private LinearLayout toolCard(String title,String subtitle){
        LinearLayout c=card(0xFF211E18,0xFFFFC86B);
        TextView t=text(title,16);
        t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        c.addView(t,full());
        TextView s=text(subtitle,12);
        s.setTextColor(0xFF9AA0AA);
        c.addView(s,top(2));
        return c;
    }
    
    private LinearLayout card(){return card(Color.WHITE);}

    private LinearLayout card(int color){
        LinearLayout c=new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16),dp(15),dp(16),dp(15));
        GradientDrawable bg=new GradientDrawable();
        bg.setColor(color == Color.WHITE ? 0xFF161D27 : color);
        bg.setCornerRadius(dp(17));
        bg.setStroke(dp(1),0xFF2B3746);
        c.setBackground(bg);
        return c;
    }

    private LinearLayout card(int color,int strokeColor){
        LinearLayout c=card(color);
        GradientDrawable bg=(GradientDrawable)c.getBackground();
        bg.setStroke(dp(1),strokeColor);
        return c;
    }
    
    private Button actionButton(String label){
        return styledButton(label,0xFF1B2634,0xFF35465B,0xFFE5EDF7);
    }

    private Button primaryButton(String label){
        return styledButton(label,0xFF20496C,0xFF3979A8,Color.WHITE);
    }

    private Button dangerButton(String label){
        return styledButton(label,0xFF382329,0xFF75404B,0xFFFFDCE1);
    }

    private Button styledButton(String label,int fill,int stroke,int textColor){
        Button b=new Button(this);
        b.setText(label);
        b.setTextSize(13);
        b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setAllCaps(false);
        b.setTextColor(textColor);
        b.setMinHeight(dp(44));
        b.setMinimumHeight(dp(44));
        b.setPadding(dp(12),dp(8),dp(12),dp(8));
        GradientDrawable background=new GradientDrawable();
        background.setColor(fill);
        background.setCornerRadius(dp(12));
        background.setStroke(dp(1),stroke);
        b.setBackground(background);
        return b;
    }

    private void styleInput(EditText input){
        input.setTextColor(0xFFE8EDF4);
        input.setHintTextColor(0xFF7F8B9B);
        input.setTextSize(15);
        input.setPadding(dp(13),dp(11),dp(13),dp(11));
        input.setMinHeight(dp(48));
        input.setBackground(roundStroke(0xFF111720,12,0xFF344255));
    }

    private void addTabToBar(LinearLayout tabs,TextView tab){
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(44),1);
        lp.leftMargin=dp(2);
        lp.rightMargin=dp(2);
        tabs.addView(tab,lp);
    }

    private TextView tab(String label){
        TextView v=text(label,12);
        v.setGravity(Gravity.CENTER);
        v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return v;
    }
    
    private void selectTab(int selected){
        TextView[] tabs={monitorTab,myBatteryTab,historyTab,healthTab};
        int[] activeText={0xFF7CC7FF,0xFF63E6A8,0xFFB58CFF,0xFF63E6A8};
        int[] activeBg={0xFF172334,0xFF17271F,0xFF211B2D,0xFF17271F};
        for(int i=0;i<tabs.length;i++){
            boolean active=i==selected;
            tabs[i].setTextColor(active?activeText[i]:0xFFA2ADBC);
            tabs[i].setBackground(roundStroke(active?activeBg[i]:0xFF171F2A,
                    11,active?activeText[i]:0xFF252F3E));
            tabs[i].setMinHeight(dp(44));
            tabs[i].setContentDescription(tabs[i].getText()+" tab"+(active?", selected":""));
        }
    }
    
    private GradientDrawable round(int color,int radius){
        GradientDrawable d=new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private GradientDrawable roundStroke(int color,int radius,int strokeColor){
        GradientDrawable d=round(color,radius);
        d.setStroke(dp(1),strokeColor);
        return d;
    }
    
    private String formatSigned(double value){return String.format(java.util.Locale.US,"%+.1f",value);}
    private void startNotificationService(){Intent i=new Intent(this,BatteryNotificationService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}
    private TextView text(String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(0xFFE8EBF0);return v;}
    private LinearLayout.LayoutParams full(){return new LinearLayout.LayoutParams(-1,-2);}
    private LinearLayout.LayoutParams top(int n){LinearLayout.LayoutParams p=full();p.topMargin=dp(n);return p;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
