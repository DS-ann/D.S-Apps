package com.dsann.batterymonitor;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
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

public class MainActivity extends Activity {
    private static final String PREFS="settings";
    private static final String KEY_INTERVAL_MS="sampling_interval_ms";
    private static final long MIN_RECORDING_INTERVAL_MS=100L;
    private static final long MAX_RECORDING_INTERVAL_MS=2000L;
    private static final long RECORDING_INTERVAL_STEP_MS=10L;
    private static final long DEFAULT_RECORDING_INTERVAL_MS=1000L;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private TextView voltage,current;
    private TextView calculatedPercent,calculatedMah;
    private EditText customCapacityInput,startingChargeInput;
    private Switch notificationSwitch;
    private SeekBar samplingBar;
    private TextView samplingLabel;
    private LinearLayout monitorView,myBatteryView,historyView,historyList;
    private View monitorContainer;
    private ScrollView myBatteryScroll;
    private TextView monitorTab,myBatteryTab,historyTab,healthTab;
    private LinearLayout healthView;
    private ScrollView healthScroll;
    private TextView healthLevel,healthStatus,healthTemp,healthVoltage,healthCurrent,healthSource,healthFullCharge,healthRemaining,healthDesign,healthRatio,healthRaw,healthFile,healthFullEnergy,healthDesignEnergy,healthSessionEstimate,healthSessionHealth;
    private final Runnable updater=new Runnable(){public void run(){updateValues();handler.postDelayed(this,currentRecordingIntervalMs());}};
    
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0F1115);
        
        LinearLayout header=new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(20),dp(18),dp(20),dp(8));
        TextView appTitle=text("BatteryPulse",25);
        appTitle.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        header.addView(appTitle,full());
        TextView subtitle=text("Battery monitor • sessions • health",13);
        subtitle.setTextColor(0xFF9AA0AA);
        header.addView(subtitle,top(2));
        root.addView(header);
        
        LinearLayout tabs=new LinearLayout(this);
        tabs.setPadding(dp(16),dp(4),dp(16),dp(10));
        monitorTab=tab("Monitor");
        myBatteryTab=tab("My Battery");
        historyTab=tab("History");
        healthTab=tab("Health");
        tabs.addView(monitorTab,new LinearLayout.LayoutParams(0,dp(42),1));
        tabs.addView(myBatteryTab,new LinearLayout.LayoutParams(0,dp(42),1));
        tabs.addView(historyTab,new LinearLayout.LayoutParams(0,dp(42),1));
        tabs.addView(healthTab,new LinearLayout.LayoutParams(0,dp(42),1));
        root.addView(tabs);
        
        monitorView=buildMonitor();
        monitorContainer=new ScrollView(this);
        ((ScrollView)monitorContainer).setFillViewport(true);
        ((ScrollView)monitorContainer).addView(monitorView);
        myBatteryView=buildMyBattery();
        myBatteryScroll=new ScrollView(this);
        myBatteryScroll.setFillViewport(true);
        myBatteryScroll.setClipToPadding(false);
        myBatteryScroll.setPadding(0,0,0,dp(8));
        myBatteryScroll.addView(myBatteryView);
        historyView=buildHistory();
        LinearLayout healthContent=buildHealth();
        healthScroll=new ScrollView(this);
        healthScroll.setFillViewport(true);
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
        if (notificationSwitch.isChecked()) startNotificationService();
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
        
        enableDoze.setOnClickListener(v->{DozeController.Result result=DozeController.forceDoze();dozeStatus.setText(result.message);});
        disableDoze.setOnClickListener(v->{DozeController.Result result=DozeController.disableForceDoze();dozeStatus.setText(result.message);});
        
        LinearLayout percentBox=toolCard("Battery percentage override","Temporarily change Android's displayed battery level.");
        LinearLayout percentRow=new LinearLayout(this);
        percentRow.setGravity(Gravity.CENTER_VERTICAL);
        EditText percentInput=new EditText(this);
        percentInput.setHint("0–100");
        percentInput.setSingleLine(true);
        percentInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        percentRow.addView(percentInput,new LinearLayout.LayoutParams(0,-2,1));
        Button setPercent=actionButton("Set");
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
            try{
                BatteryPercentOverride.Result result=BatteryPercentOverride.set(Integer.parseInt(value));
                percentStatus.setText(result.message);
            }catch(NumberFormatException e){percentStatus.setText("Enter a value from 0 to 100.");}
        });
        resetPercent.setOnClickListener(v->{BatteryPercentOverride.Result result=BatteryPercentOverride.reset();percentStatus.setText(result.message);});
        
        notificationSwitch.setOnCheckedChangeListener((b,on)->{
            getSharedPreferences(PREFS,0).edit().putBoolean("notification_enabled",on).apply();
            if(on){
                startNotificationService();
            }else { SessionStore.finish(this); stopService(new Intent(this,BatteryNotificationService.class)); }
        });
        return r;
    }
    
    private LinearLayout buildMyBattery(){
        LinearLayout r=new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(dp(16),dp(4),dp(16),dp(20));

        TextView title=text("My Battery",22);
        title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        r.addView(title,full());
        LinearLayout calculator=card(0xFF17271F,0xFF63E6A8);
        TextView calculatorTitle=text("Current estimate",16);
        calculatorTitle.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        calculator.addView(calculatorTitle,full());
        calculatedPercent=text("--%",38);
        calculatedPercent.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        calculatedPercent.setTextColor(0xFF63E6A8);
        calculator.addView(calculatedPercent,top(4));
        calculatedMah=text("Remaining charge: enter capacity and starting mAh",13);
        calculatedMah.setTextColor(0xFFD2D8E0);
        calculator.addView(calculatedMah,top(2));
        TextView capacityLabel=text("Full usable battery capacity (mAh)",13);
        capacityLabel.setTextColor(0xFFD2D8E0);
        calculator.addView(capacityLabel,top(12));
        customCapacityInput=new EditText(this);
        customCapacityInput.setSingleLine(true);
        customCapacityInput.setHint("e.g. 4050");
        customCapacityInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        double savedCapacity=BatteryPercentCalculator.getCustomCapacityMah(this);
        if(!Double.isNaN(savedCapacity)&&!Double.isInfinite(savedCapacity)&&savedCapacity>0){
            customCapacityInput.setText(String.format(java.util.Locale.US,"%.2f",savedCapacity)
                    .replaceAll("\\.?0+$",""));
        }
        calculator.addView(customCapacityInput,top(3));

        TextView startingLabel=text("Starting remaining charge (mAh)",13);
        startingLabel.setTextColor(0xFFD2D8E0);
        calculator.addView(startingLabel,top(10));
        startingChargeInput=new EditText(this);
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
        Button saveStart=actionButton("Save & start");
        Button stopMeasurement=actionButton("Stop");
        actions.addView(saveStart,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout.LayoutParams stopParams=new LinearLayout.LayoutParams(0,-2,1);
        stopParams.leftMargin=dp(8);
        actions.addView(stopMeasurement,stopParams);
        calculator.addView(actions,top(10));
        Button clearAll=actionButton("Clear all");
        calculator.addView(clearAll,top(6));
        saveStart.setOnClickListener(v->startCalculatorFromInputs());
        stopMeasurement.setOnClickListener(v->stopMeasurement());
        clearAll.setOnClickListener(v->clearAllMeasurementData());
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
        if(index==2){refreshHistory();handler.postDelayed(historyUpdater,10000);}
        else if(index==3) refreshHealth();
    }
    private void refreshHealth(){
        try{
            android.content.Intent b=registerReceiver(null,new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED));
            if(b!=null){
                int level=b.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL,-1), scale=b.getIntExtra(android.os.BatteryManager.EXTRA_SCALE,100); int pct=(level>=0&&scale>0)?Math.round(level*100f/scale):-1;
                healthLevel.setText(pct>=0?pct+"%":"--"); int status=b.getIntExtra(android.os.BatteryManager.EXTRA_STATUS,-1);
                healthStatus.setText("Status: "+(status==android.os.BatteryManager.BATTERY_STATUS_CHARGING?"Charging":status==android.os.BatteryManager.BATTERY_STATUS_FULL?"Full":"Discharging / idle"));
                int mv=b.getIntExtra(android.os.BatteryManager.EXTRA_VOLTAGE,0); healthVoltage.setText(mv>0?String.format(java.util.Locale.US,"%.3f V",mv/1000.0):"N/A");
                int temp=b.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE,0); healthTemp.setText(temp!=0?String.format(java.util.Locale.US,"%.1f °C",temp/10.0):"N/A");
            }
            BatteryReader.Reading rr=BatteryReader.read(this); healthCurrent.setText(rr.currentText()); healthSource.setText(rr.currentUa!=Long.MIN_VALUE?"Fuel gauge":"Android");
            BatteryHealthReader.Data d=BatteryHealthReader.read(this);
            healthFullCharge.setText(d.charge(d.fullChargeUaH)); healthRemaining.setText(d.charge(d.remainingChargeUaH)); healthDesign.setText(d.charge(d.designChargeUaH));
            healthRatio.setText(Double.isNaN(d.ratio)?"Unavailable":String.format(java.util.Locale.US,"%.1f %%",d.ratio));
            healthRaw.setText(d.rawHealth==null?"Unavailable":d.rawHealth); healthFile.setText(d.sourceFile);
            healthFullEnergy.setText(d.energy(d.fullEnergyUWh)); healthDesignEnergy.setText(d.energy(d.designEnergyUWh));
            SessionEstimate est=estimateFromSessions();
            healthSessionEstimate.setText(est.capacityMah>0?String.format(java.util.Locale.US,"%.0f mAh",est.capacityMah):"Unavailable");
            healthSessionHealth.setText(est.health>0?String.format(java.util.Locale.US,"%.1f %%  •  %d qualifying sessions",est.health,est.count):"Unavailable");
        }catch(Throwable ignored){
            healthLevel.setText("--"); healthStatus.setText("Status: unavailable"); healthVoltage.setText("N/A"); healthTemp.setText("N/A"); healthCurrent.setText("N/A"); healthSource.setText("Unavailable");
            healthFullCharge.setText("Unavailable"); healthRemaining.setText("Unavailable"); healthDesign.setText("Unavailable"); healthRatio.setText("Unavailable"); healthRaw.setText("Unavailable"); healthFile.setText("Unavailable"); healthFullEnergy.setText("Unavailable"); healthDesignEnergy.setText("Unavailable"); healthSessionEstimate.setText("Unavailable"); healthSessionHealth.setText("Unavailable");
        }
    }
    private SessionEstimate estimateFromSessions(){
        ArrayList<SessionStore.Record> records=SessionStore.getRecords(this);
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
        Button clear=actionButton("Clear history");
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
        handler.removeCallbacks(updater);
        handler.post(updater);
        handler.removeCallbacks(historyUpdater);
        if(historyView!=null&&historyView.getVisibility()==View.VISIBLE){refreshHistory();handler.postDelayed(historyUpdater,10000);}
        if(healthView!=null&&healthView.getVisibility()==View.VISIBLE)refreshHealth();
    }
    
    @Override protected void onPause(){
        handler.removeCallbacks(updater);
        handler.removeCallbacks(historyUpdater);
        super.onPause();
    }
    
    private void updateValues(){
        try {
            BatteryReader.Reading r=BatteryReader.read(this);
            voltage.setText(r.voltageText());
            current.setText(r.currentText());
            boolean backgroundRecording=getSharedPreferences(PREFS,0).getBoolean("notification_enabled",false);
            updateCalculatedLevel(backgroundRecording
                    ?BatteryPercentCalculator.current(this)
                    :BatteryPercentCalculator.sample(this,r));
        } catch (Throwable ignored) {
            voltage.setText("N/A");
            current.setText("N/A");
            updateCalculatedLevel(BatteryPercentCalculator.current(this));
        }
    }

    private void updateCalculatedLevel(BatteryPercentCalculator.Estimate estimate){
        if(calculatedPercent==null||calculatedMah==null)return;
        if(estimate==null||!estimate.isAvailable()){
            calculatedPercent.setText("--");
            calculatedMah.setText(estimate!=null&&estimate.capacityMah==0.0
                    ? "Remaining charge: 0 mAh" : "Remaining charge: -- mAh");
            return;
        }
        calculatedPercent.setText(estimate.percent+"%");
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
            updateCalculatedLevel(BatteryPercentCalculator.startEstimate(
                    this,capacity,starting,BatteryReader.read(this)));
        }catch(NumberFormatException e){
            Toast.makeText(this,"Enter valid numeric mAh values.",Toast.LENGTH_SHORT).show();
        }
    }

    private void stopMeasurement(){
        BatteryPercentCalculator.stopMeasurement(this);
        boolean wasRecording=notificationSwitch!=null&&notificationSwitch.isChecked();
        if(wasRecording){
            notificationSwitch.setChecked(false);
        }else{
            getSharedPreferences(PREFS,0).edit().putBoolean("notification_enabled",false).apply();
            SessionStore.finish(this);
            stopService(new Intent(this,BatteryNotificationService.class));
        }
        updateCalculatedLevel(BatteryPercentCalculator.current(this));
    }

    private void clearAllMeasurementData(){
        stopMeasurement();
        SessionStore.clear(this);
        BatteryPercentCalculator.clearAll(this);
        if(customCapacityInput!=null)customCapacityInput.setText("0");
        if(startingChargeInput!=null)startingChargeInput.setText("0");
        updateCalculatedLevel(BatteryPercentCalculator.current(this));
        if(historyView!=null&&historyView.getVisibility()==View.VISIBLE)refreshHistory();
    }

    private long currentRecordingIntervalMs(){
        long value=getSharedPreferences(PREFS,0).getLong(KEY_INTERVAL_MS,DEFAULT_RECORDING_INTERVAL_MS);
        return value<MIN_RECORDING_INTERVAL_MS||value>MAX_RECORDING_INTERVAL_MS
                ?DEFAULT_RECORDING_INTERVAL_MS:value;
    }

    private String formatRecordingInterval(long ms){
        return String.format(java.util.Locale.US,"Current recording interval  •  %.2f s",ms/1000.0);
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
        c.setPadding(dp(14),dp(13),dp(14),dp(13));
        GradientDrawable bg=new GradientDrawable();
        bg.setColor(color == Color.WHITE ? 0xFF171A20 : color);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1),0xFF303744);
        c.setBackground(bg);
        if(Build.VERSION.SDK_INT>=21)c.setElevation(dp(1));
        return c;
    }

    private LinearLayout card(int color,int strokeColor){
        LinearLayout c=card(color);
        GradientDrawable bg=(GradientDrawable)c.getBackground();
        bg.setStroke(dp(1),strokeColor);
        return c;
    }
    
    private Button actionButton(String label){
        Button b=new Button(this);
        b.setText(label);
        b.setTextSize(13);
        b.setAllCaps(false);
        return b;
    }
    
    private TextView tab(String label){
        TextView v=text(label,14);
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
            tabs[i].setTextColor(active?activeText[i]:0xFF8E96A3);
            tabs[i].setBackground(round(active?activeBg[i]:0x00000000,12));
            tabs[i].setMinHeight(dp(48));
            tabs[i].setContentDescription(tabs[i].getText()+" tab"+(active?", selected":""));
        }
    }
    
    private GradientDrawable round(int color,int radius){
        GradientDrawable d=new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }
    
    private String formatSigned(double value){return String.format(java.util.Locale.US,"%+.1f",value);}
    private int indexForInterval(long ms){int best=0;long diff=Math.abs(INTERVALS_MS[0]-ms);for(int i=1;i<INTERVALS_MS.length;i++){long d=Math.abs(INTERVALS_MS[i]-ms);if(d<diff){diff=d;best=i;}}return best;}
    private void startNotificationService(){Intent i=new Intent(this,BatteryNotificationService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}
    private TextView text(String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(0xFFE8EBF0);return v;}
    private LinearLayout.LayoutParams full(){return new LinearLayout.LayoutParams(-1,-2);}
    private LinearLayout.LayoutParams top(int n){LinearLayout.LayoutParams p=full();p.topMargin=dp(n);return p;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
