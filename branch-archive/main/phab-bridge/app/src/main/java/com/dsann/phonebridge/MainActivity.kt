package com.dsann.phonebridge

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.TelecomManager
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var body: LinearLayout
    private lateinit var number: EditText
    private var renderedMode = ""
    private var selectedTab = TAB_KEYPAD
    private var pendingDialNumber: String? = null
    private var localResult = ""
    private var showCallKeypad = false

    private var bridgeSummaryView: TextView? = null
    private var phoneAudioDiagnosticsView: TextView? = null
    private var tabletAudioDiagnosticsView: TextView? = null
    private var rootDiagnosticsView: TextView? = null
    private var rootDiagnosticsReport = ""
    @Volatile private var rootDiagnosticsRunning = false
    private var tinymixDiagnosticsView: TextView? = null
    private var tinymixDiagnosticsReport = ""
    private var tinymixComparisonReport = ""
    @Volatile private var tinymixDiagnosticsRunning = false

    private var activeNumberView: TextView? = null
    private var activeStatusView: TextView? = null
    private var activeTimerView: TextView? = null

    private val handler = Handler(Looper.getMainLooper())
    private val refresh = object : Runnable {
        override fun run() {
            val prefs = getSharedPreferences(BridgeService.PREFS, MODE_PRIVATE)
            val state = prefs.getString(BridgeService.KEY_CALL_STATE, "IDLE") ?: "IDLE"
            val mode = when (state) {
                "RINGING" -> MODE_INCOMING
                "DIALING", "CONNECTING", "OFFHOOK" -> MODE_ACTIVE_CALL
                else -> MODE_HOME
            }
            // Keep Bridge diagnostics visible during an active call when requested.
            // Do not rebuild the call screen every 500 ms over the report.
            val keepBridgeVisible = mode == MODE_ACTIVE_CALL &&
                selectedTab == TAB_BRIDGE && renderedMode == MODE_HOME
            if (mode != renderedMode && !keepBridgeVisible) renderScreen(mode)
            if (mode == MODE_ACTIVE_CALL || mode == MODE_INCOMING) updateCallScreen(state)
            if (mode == MODE_HOME && selectedTab == TAB_BRIDGE) refreshBridgeStatus()
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = COLOR_BG
        window.navigationBarColor = COLOR_BG
        buildUi()
        handleDialIntent(intent)
        requestPermissionsIfNeeded()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDialIntent(intent)
        renderedMode = ""
        handler.removeCallbacks(refresh)
        handler.post(refresh)
    }

    override fun onResume() {
        super.onResume()
        renderedMode = ""
        handler.removeCallbacks(refresh)
        handler.post(refresh)
    }

    override fun onPause() {
        handler.removeCallbacks(refresh)
        super.onPause()
    }

    private fun buildUi() {
        renderScreen(MODE_HOME)
    }

    private fun renderScreen(mode: String) {
        renderedMode = mode
        activeNumberView = null
        activeStatusView = null
        activeTimerView = null

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(COLOR_BG)
            setPadding(dp(18), dp(12), dp(18), dp(10))
        }
        setContentView(root)

        if (mode == MODE_HOME) {
            buildHomeHeader()
            body = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(0, dp(10), 0, dp(8))
            }
            root.addView(body, LinearLayout.LayoutParams(-1, 0, 1f))
            buildBottomNavigation()
            showSelectedTab()
        } else {
            buildCallScreen(mode)
        }
    }

    private fun buildHomeHeader() {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2), dp(8), dp(2), dp(14))
        }
        val titleColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        titleColumn.addView(makeText("PhoneBridge", 25f, COLOR_TEXT, true))
        titleColumn.addView(makeText("CELLULAR CALLS", 10f, COLOR_MUTED, true))
        header.addView(titleColumn, LinearLayout.LayoutParams(0, -2, 1f))

        val state = getSharedPreferences(BridgeService.PREFS, MODE_PRIVATE)
            .getString(BridgeService.KEY_CALL_STATE, "IDLE") ?: "IDLE"
        val statusPill = makeText(
            if (isDefaultDialer()) "PHONE READY" else "DIALER SETUP",
            10f,
            if (isDefaultDialer()) COLOR_GREEN else COLOR_AMBER,
            true,
            Gravity.CENTER
        )
        statusPill.background = rounded(COLOR_PANEL, 16)
        statusPill.setPadding(dp(11), dp(8), dp(11), dp(8))
        header.addView(statusPill)
        root.addView(header, LinearLayout.LayoutParams(-1, -2))
        root.addView(View(this).apply { setBackgroundColor(COLOR_LINE) },
            LinearLayout.LayoutParams(-1, dp(1)))
    }

    private fun buildBottomNavigation() {
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(4))
        }
        val tabs = listOf(TAB_KEYPAD to "Keypad", TAB_RECENTS to "Recents", TAB_BRIDGE to "Bridge")
        tabs.forEach { pair ->
            val selected = selectedTab == pair.first
            val tab = makeText(
                pair.second,
                13f,
                if (selected) COLOR_TEXT else COLOR_MUTED,
                selected,
                Gravity.CENTER
            )
            tab.background = rounded(if (selected) COLOR_SELECTED else COLOR_BG, 16)
            tab.setPadding(dp(8), dp(12), dp(8), dp(12))
            tab.setOnClickListener {
                selectedTab = pair.first
                renderScreen(MODE_HOME)
            }
            nav.addView(tab, LinearLayout.LayoutParams(0, -2, 1f).apply {
                leftMargin = dp(4)
                rightMargin = dp(4)
            })
        }
        root.addView(nav, LinearLayout.LayoutParams(-1, -2))
    }

    private fun showSelectedTab() {
        body.removeAllViews()
        when (selectedTab) {
            TAB_RECENTS -> showRecentsTab()
            TAB_BRIDGE -> showBridgeTab()
            else -> showKeypadTab()
        }
    }

    private fun showKeypadTab() {
        body.addView(makeText("Enter a number", 14f, COLOR_MUTED, true),
            LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })

        val numberRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        number = EditText(this).apply {
            hint = "Phone number"
            setHintTextColor(COLOR_MUTED)
            setTextColor(COLOR_TEXT)
            textSize = 22f
            inputType = InputType.TYPE_CLASS_PHONE
            setSingleLine(true)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), 0, dp(12), 0)
            background = rounded(COLOR_PANEL, 18)
            if (Build.VERSION.SDK_INT >= 21) setShowSoftInputOnFocus(false)
            isFocusableInTouchMode = true
            pendingDialNumber?.let {
                setText(it)
                setSelection(text.length)
                pendingDialNumber = null
            }
        }
        numberRow.addView(number, LinearLayout.LayoutParams(0, dp(56), 1f))
        val delete = actionButton("⌫", COLOR_PANEL, COLOR_TEXT, {
            val value = number.text
            if (value.isNotEmpty()) value.delete(value.length - 1, value.length)
        })
        delete.setOnLongClickListener {
            number.setText("")
            true
        }
        numberRow.addView(delete, LinearLayout.LayoutParams(dp(54), dp(56)).apply {
            leftMargin = dp(8)
        })
        body.addView(numberRow, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(12)
        })

        val keypad = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val keys = arrayOf(
            arrayOf("1" to "", "2" to "ABC", "3" to "DEF"),
            arrayOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
            arrayOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
            arrayOf("*" to "", "0" to "+", "#" to "")
        )
        keys.forEach { rowKeys ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            rowKeys.forEach { key ->
                row.addView(dialKey(key.first, key.second) {
                    number.append(key.first)
                }, LinearLayout.LayoutParams(0, dp(61), 1f).apply {
                    leftMargin = dp(4)
                    rightMargin = dp(4)
                    bottomMargin = dp(6)
                })
            }
            keypad.addView(row, LinearLayout.LayoutParams(-1, -2))
        }
        body.addView(keypad, LinearLayout.LayoutParams(-1, 0, 1f))

        val call = actionButton("Call", COLOR_GREEN, COLOR_DARK_TEXT) {
            val result = PhoneCallController.dial(this, number.text.toString())
            localResult = result
            if (result.startsWith("ERROR")) {
                Toast.makeText(this, result, Toast.LENGTH_LONG).show()
            } else if (!isDefaultDialer()) {
                Toast.makeText(
                    this,
                    "Calls use the system phone screen until PhoneBridge is set as the default phone app.",
                    Toast.LENGTH_LONG
                ).show()
            }
            handler.post(refresh)
        }
        call.textSize = 18f
        call.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        body.addView(call, LinearLayout.LayoutParams(-1, dp(54)).apply {
            topMargin = dp(2)
        })
        if (!isDefaultDialer()) {
            body.addView(makeText(
                "Set PhoneBridge as the default phone app to use its own in-call screen.",
                11f, COLOR_MUTED, false, Gravity.CENTER
            ), LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(8)
            })
        }
    }

    private fun dialKey(digit: String, letters: String, onClick: () -> Unit): LinearLayout {
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(COLOR_PANEL, 18)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
        tile.addView(makeText(digit, 24f, COLOR_TEXT, false, Gravity.CENTER))
        if (letters.isNotEmpty()) {
            tile.addView(makeText(letters, 9f, COLOR_MUTED, true, Gravity.CENTER))
        }
        return tile
    }

    private fun showRecentsTab() {
        body.addView(makeText("Recent calls", 20f, COLOR_TEXT, true),
            LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        val prefs = getSharedPreferences(BridgeService.PREFS, MODE_PRIVATE)
        val recent = try {
            JSONArray(prefs.getString(BridgeService.KEY_RECENTS, "[]") ?: "[]")
        } catch (_: Throwable) {
            JSONArray()
        }
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(4))
        }
        if (recent.length() == 0) {
            list.addView(makeText("Your calls will appear here.", 14f, COLOR_MUTED),
                LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(20) })
        } else {
            for (index in 0 until recent.length()) {
                val entry = recent.optJSONObject(index) ?: continue
                val phoneNumber = entry.optString("number", "")
                if (phoneNumber.isBlank()) continue
                val direction = entry.optString("direction", "OUTGOING")
                val timestamp = entry.optLong("timestamp", 0L)
                val duration = entry.optLong("duration", 0L)
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = rounded(COLOR_PANEL, 16)
                    setPadding(dp(12), dp(11), dp(10), dp(11))
                }
                val details = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                details.addView(makeText(phoneNumber, 15f, COLOR_TEXT, true))
                val whenText = if (timestamp > 0) {
                    SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault()).format(Date(timestamp))
                } else {
                    "Recent call"
                }
                val directionText = when (direction) {
                    "INCOMING" -> "Incoming"
                    "MISSED" -> "Missed"
                    else -> "Outgoing"
                }
                val suffix = if (duration > 0) " · " + (duration / 60) + ":" +
                    String.format(Locale.getDefault(), "%02d", duration % 60) else ""
                details.addView(makeText(directionText + " · " + whenText + suffix, 11f, COLOR_MUTED))
                row.addView(details, LinearLayout.LayoutParams(0, -2, 1f))
                val callButton = actionButton("Call", COLOR_GREEN, COLOR_DARK_TEXT) {
                    selectedTab = TAB_KEYPAD
                    pendingDialNumber = phoneNumber
                    renderScreen(MODE_HOME)
                }
                row.addView(callButton, LinearLayout.LayoutParams(dp(64), dp(42)).apply {
                    leftMargin = dp(8)
                })
                list.addView(row, LinearLayout.LayoutParams(-1, -2).apply {
                    bottomMargin = dp(8)
                })
            }
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(list)
        }
        body.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
    }

    private fun showBridgeTab() {
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(12))
        }

        content.addView(makeText("Bridge diagnostics", 20f, COLOR_TEXT, true),
            LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })

        val currentCallState = getSharedPreferences(BridgeService.PREFS, MODE_PRIVATE)
            .getString(BridgeService.KEY_CALL_STATE, "IDLE") ?: "IDLE"
        if (currentCallState in arrayOf("DIALING", "CONNECTING", "OFFHOOK", "ACTIVE", "HOLDING")) {
            content.addView(actionButton("Return to call screen", COLOR_SELECTED, COLOR_TEXT, {
                renderScreen(MODE_ACTIVE_CALL)
            }), LinearLayout.LayoutParams(-1, dp(46)).apply { bottomMargin = dp(10) })
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(COLOR_PANEL, 18)
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }
        card.addView(makeText("LIVE BRIDGE / CALL STATE", 10f, COLOR_MUTED, true))
        bridgeSummaryView = makeText("Reading status…", 12f, COLOR_TEXT, false)
        bridgeSummaryView?.setTextIsSelectable(true)
        card.addView(bridgeSummaryView, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(7) })
        content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })

        content.addView(makeText("PHONE AUDIO METRICS", 11f, COLOR_MUTED, true),
            LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(4) })
        phoneAudioDiagnosticsView = makeText("", 11f, COLOR_TEXT, false)
        phoneAudioDiagnosticsView?.setTextIsSelectable(true)
        content.addView(phoneAudioDiagnosticsView, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })

        content.addView(makeText("TABLET AUDIO METRICS (OVER WI-FI)", 11f, COLOR_MUTED, true),
            LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(4) })
        tabletAudioDiagnosticsView = makeText("Waiting for tablet telemetry. Connect both devices and start Wi-Fi audio.", 11f, COLOR_TEXT, false)
        tabletAudioDiagnosticsView?.setTextIsSelectable(true)
        content.addView(tabletAudioDiagnosticsView, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })

        content.addView(makeText(
            "During calls, tablet microphone packets are measured but not played on the phone speaker. The phone attempts VOICE_DOWNLINK capture only; if Android blocks it, the error is shown rather than silently relaying the phone mic. STREAM_MUSIC is not the cellular uplink.",
            11f, COLOR_MUTED, false
        ), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })

        content.addView(actionButton(
            if (isDefaultDialer()) "PhoneBridge is the default phone app" else "Set as default phone app",
            COLOR_SELECTED, COLOR_TEXT
        ) { requestDefaultDialer() }, LinearLayout.LayoutParams(-1, dp(48)).apply { bottomMargin = dp(7) })

        content.addView(actionButton("Start bridge", COLOR_PANEL, COLOR_TEXT, {
            startBridgeService()
            localResult = "Bridge start requested."
            refreshBridgeStatus()
        }), LinearLayout.LayoutParams(-1, dp(46)).apply { bottomMargin = dp(7) })

        content.addView(actionButton("Stop bridge", COLOR_PANEL, COLOR_TEXT, {
            startService(Intent(this, BridgeService::class.java).apply {
                action = BridgeService.ACTION_STOP
            })
            localResult = "Bridge stop requested."
            refreshBridgeStatus()
        }), LinearLayout.LayoutParams(-1, dp(46)).apply { bottomMargin = dp(7) })

        content.addView(actionButton(
            if (rootDiagnosticsRunning) "Root diagnostics running…" else "Run root audio diagnostics",
            COLOR_SELECTED, COLOR_TEXT
        ) { runRootAudioDiagnostics() }, LinearLayout.LayoutParams(-1, dp(48)).apply { bottomMargin = dp(7) })

        content.addView(makeText("TINYMIX MIXER REPORT (READ-ONLY)", 11f, COLOR_MUTED, true),
            LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(4) })
        content.addView(makeText(
            "Bundled TinyALSA tool. This report lists mixer controls and current values only; PhoneBridge does not change any mixer controls.",
            11f, COLOR_MUTED, false
        ), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(7) })
        content.addView(actionButton(
            if (tinymixDiagnosticsRunning) "Reading mixer controls…" else "Run tinymix mixer report",
            COLOR_PANEL, COLOR_TEXT, { runTinymixDiagnostics() }
        ), LinearLayout.LayoutParams(-1, dp(46)).apply { bottomMargin = dp(7) })
        tinymixDiagnosticsView = makeText(
            if (tinymixDiagnosticsReport.isBlank()) "Not run yet. Run during a call test to inspect the current mixer state." else tinymixDiagnosticsReport,
            10f, COLOR_TEXT, false
        )
        tinymixDiagnosticsView?.setTextIsSelectable(true)
        content.addView(tinymixDiagnosticsView, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })

        content.addView(actionButton(
            "Save idle mixer baseline (read-only)", COLOR_PANEL, COLOR_TEXT, {
                saveIdleMixerBaseline()
            }
        ), LinearLayout.LayoutParams(-1, dp(46)).apply { bottomMargin = dp(7) })
        content.addView(actionButton(
            "Compare mixer during active call", COLOR_PANEL, COLOR_TEXT, {
                compareMixerDuringCall()
            }
        ), LinearLayout.LayoutParams(-1, dp(46)).apply { bottomMargin = dp(10) })

        content.addView(actionButton("Copy diagnostic report", COLOR_PANEL, COLOR_TEXT, {
            copyDiagnosticReport()
        }), LinearLayout.LayoutParams(-1, dp(46)).apply { bottomMargin = dp(10) })

        content.addView(makeText("ROOT AUDIO REPORT", 11f, COLOR_MUTED, true),
            LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(4) })
        rootDiagnosticsView = makeText(
            if (rootDiagnosticsReport.isBlank()) "Not run yet. Run this during a test call, then copy the report." else rootDiagnosticsReport,
            10f, COLOR_TEXT, false
        )
        rootDiagnosticsView?.setTextIsSelectable(true)
        content.addView(rootDiagnosticsView, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })

        if (localResult.isNotBlank()) {
            content.addView(makeText(localResult, 11f, COLOR_MUTED, false),
                LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }

        scroll.addView(content)
        body.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        refreshBridgeStatus()
    }

    private fun refreshBridgeStatus() {
        val prefs = getSharedPreferences(BridgeService.PREFS, MODE_PRIVATE)
        val bridge = prefs.getString(BridgeService.KEY_STATUS, "Starting…") ?: "Starting…"
        val call = prefs.getString(BridgeService.KEY_CALL_STATE, "IDLE") ?: "IDLE"
        val audio = prefs.getString("audio_wifi", "STOPPED") ?: "STOPPED"
        bridgeSummaryView?.text = "Bridge: " + bridge + "\nCall: " + call + "\nWi-Fi audio: " + audio
        phoneAudioDiagnosticsView?.text = AudioBridge.diagnosticSummary()
        tabletAudioDiagnosticsView?.text = prefs.getString(
            "tablet_audio_diag",
            "Waiting for tablet telemetry. Start Wi-Fi audio to receive TX/RX packet counts, mic levels, and capture errors."
        ) ?: "Waiting for tablet telemetry."
    }

    private fun runRootAudioDiagnostics() {
        if (rootDiagnosticsRunning) return
        rootDiagnosticsRunning = true
        rootDiagnosticsView?.text = "Collecting read-only root diagnostics. Keep the screen open and approve the Magisk su prompt if shown."
        Thread({
            val report = collectRootAudioDiagnostics()
            runOnUiThread {
                rootDiagnosticsRunning = false
                rootDiagnosticsReport = report
                rootDiagnosticsView?.text = report
                localResult = "Root audio diagnostics collected."
            }
        }, "PhoneBridgeRootAudioDiagnostics").start()
    }

    private fun runTinymixDiagnostics() {
        if (tinymixDiagnosticsRunning) return
        tinymixDiagnosticsRunning = true
        tinymixDiagnosticsView?.text = "Preparing bundled tinymix. Approve the Magisk su prompt if shown."
        Thread({
            val report = collectTinymixDiagnostics()
            runOnUiThread {
                tinymixDiagnosticsRunning = false
                tinymixDiagnosticsReport = report
                tinymixDiagnosticsView?.text = report
                localResult = "Tinymix mixer report collected."
            }
        }, "PhoneBridgeTinymixDiagnostics").start()
    }

    private fun isActiveCellularCallForDiagnostics(): Boolean {
        return PhoneCallController.currentState in arrayOf("OFFHOOK", "ACTIVE", "HOLDING")
    }

    private fun diagnosticTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date())
    }

    /** Save an idle mixer snapshot; this only reads controls and never changes them. */
    private fun saveIdleMixerBaseline() {
        if (PhoneCallController.currentState != "IDLE") {
            Toast.makeText(this, "Capture the baseline only when no call is ringing or active.", Toast.LENGTH_LONG).show()
            return
        }
        if (tinymixDiagnosticsRunning) return
        tinymixDiagnosticsRunning = true
        tinymixDiagnosticsView?.text = "Collecting idle mixer baseline (read-only)…"
        Thread({
            val report = collectTinymixDiagnostics()
            val controls = extractMixerControlBlocks(report)
            val valid = controls.isNotEmpty()
            val capturedAt = diagnosticTimestamp()
            if (valid) {
                getSharedPreferences(BridgeService.PREFS, MODE_PRIVATE).edit()
                    .putString("tinymix_idle_baseline_report", report)
                    .putString("tinymix_idle_baseline_at", capturedAt)
                    .apply()
            }
            runOnUiThread {
                tinymixDiagnosticsRunning = false
                tinymixDiagnosticsReport = report
                tinymixComparisonReport = if (valid) {
                    "Idle mixer baseline saved at " + capturedAt + ".\n" +
                        "Controls read: " + controls.size + ". No mixer values were changed."
                } else {
                    "Could not save a valid idle baseline. Check the tinymix report and its contents_exit value."
                }
                tinymixDiagnosticsView?.text = tinymixComparisonReport + "\n\n" + report
                localResult = if (valid) "Read-only idle mixer baseline saved." else "Idle mixer baseline failed."
            }
        }, "PhoneBridgeTinymixIdleBaseline").start()
    }

    /** Compare current controls against the saved idle baseline while a cellular call is active. */
    private fun compareMixerDuringCall() {
        if (!isActiveCellularCallForDiagnostics()) {
            Toast.makeText(this, "Start a connected cellular call before comparing mixer state.", Toast.LENGTH_LONG).show()
            return
        }
        if (tinymixDiagnosticsRunning) return
        val prefs = getSharedPreferences(BridgeService.PREFS, MODE_PRIVATE)
        val baseline = prefs.getString("tinymix_idle_baseline_report", null)
        val baselineAt = prefs.getString("tinymix_idle_baseline_at", "unknown") ?: "unknown"
        if (baseline.isNullOrBlank()) {
            Toast.makeText(this, "Save an idle mixer baseline first.", Toast.LENGTH_LONG).show()
            return
        }

        tinymixDiagnosticsRunning = true
        tinymixDiagnosticsView?.text = "Collecting active-call mixer state and comparing with the idle baseline…"
        Thread({
            val current = collectTinymixDiagnostics()
            val valid = extractMixerControlBlocks(current).isNotEmpty()
            val comparison = if (valid) {
                compareMixerReports(baseline, current, baselineAt, diagnosticTimestamp())
            } else {
                "Active-call mixer snapshot failed. Check the report and contents_exit value.\n" + current
            }
            runOnUiThread {
                tinymixDiagnosticsRunning = false
                tinymixDiagnosticsReport = current
                tinymixComparisonReport = comparison
                tinymixDiagnosticsView?.text = comparison + "\n\n=== Full active-call tinymix report ===\n" + current
                localResult = if (valid) "Read-only idle-versus-call mixer comparison completed." else "Active-call mixer snapshot failed."
            }
        }, "PhoneBridgeTinymixCallComparison").start()
    }

    private fun extractMixerControlBlocks(report: String): LinkedHashMap<String, String> {
        val controls = LinkedHashMap<String, String>()
        val marker = "--- Mixer current values ---"
        if (!report.contains(marker)) return controls
        val values = report.substringAfter(marker, "")
        val controlStart = Regex("""^\s*ctl\s+(\d+)\s*:""")
        var currentId: String? = null
        val currentLines = ArrayList<String>()

        fun finishBlock() {
            val id = currentId
            if (id != null && currentLines.isNotEmpty()) {
                controls[id] = currentLines.joinToString("\n")
            }
            currentLines.clear()
        }

        for (line in values.lineSequence()) {
            val match = controlStart.find(line)
            if (match != null) {
                finishBlock()
                currentId = match.groupValues[1]
                currentLines.add(line.trimEnd())
            } else if (currentId != null) {
                currentLines.add(line.trimEnd())
            }
        }
        finishBlock()
        return controls
    }

    private fun compareMixerReports(
        baselineReport: String,
        currentReport: String,
        baselineAt: String,
        currentAt: String
    ): String {
        val baseline = extractMixerControlBlocks(baselineReport)
        val current = extractMixerControlBlocks(currentReport)
        if (baseline.isEmpty() || current.isEmpty()) {
            return "Comparison unavailable: could not parse control blocks from both reports."
        }
        val ids = (baseline.keys + current.keys).distinct().sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }
        val changed = ids.filter { baseline[it] != current[it] }
        val result = StringBuilder()
        result.append("READ-ONLY MIXER COMPARISON\n")
        result.append("Idle baseline captured: ").append(baselineAt).append(" (CallState=IDLE)\n")
        result.append("Active snapshot captured: ").append(currentAt).append(" (CallState=")
            .append(PhoneCallController.currentState).append(")\n")
        result.append("Controls: idle=").append(baseline.size)
            .append(", active=").append(current.size)
            .append(", changed/added/removed=").append(changed.size).append("\n")
        result.append("No mixer control was modified by this comparison.\n\n")

        if (changed.isEmpty()) {
            result.append("No control blocks differ between the two snapshots.")
        } else {
            val maxDetails = 35
            for (id in changed.take(maxDetails)) {
                fun compact(block: String?): String {
                    if (block == null) return "(control missing)"
                    return block.lineSequence().take(4).joinToString(" | ").take(480)
                }
                result.append("Control ").append(id).append(":\n")
                result.append("  idle: ").append(compact(baseline[id])).append("\n")
                result.append("  call: ").append(compact(current[id])).append("\n\n")
            }
            if (changed.size > maxDetails) {
                result.append("Only the first ").append(maxDetails)
                    .append(" changed controls are shown here; ").append(changed.size - maxDetails)
                    .append(" additional changed controls are omitted from this summary.")
            }
        }
        return result.toString()
    }

    private fun prepareBundledTinymix(): Pair<File, String>? {
        val supportedAbis = if (Build.VERSION.SDK_INT >= 21) Build.SUPPORTED_ABIS else arrayOf(Build.CPU_ABI)
        for (abi in supportedAbis) {
            if (abi != "arm64-v8a" && abi != "armeabi-v7a") continue
            val destination = File(filesDir, "tinymix-$abi")
            try {
                assets.open("tinymix/$abi/tinymix").use { input ->
                    FileOutputStream(destination).use { output -> input.copyTo(output) }
                }
                destination.setReadable(true, true)
                destination.setExecutable(true, false)
                if (destination.canExecute()) return Pair(destination, abi)
            } catch (_: Throwable) {
                try { destination.delete() } catch (_: Throwable) { }
            }
        }
        return null
    }

    private fun collectTinymixDiagnostics(): String {
        val prepared = prepareBundledTinymix()
            ?: return "Bundled tinymix binary is unavailable for this device ABI. Reinstall a PhoneBridge APK built with the TinyALSA asset step."
        val binary = prepared.first.absolutePath
        val abi = prepared.second
        val script = """
            set +e
            echo '=== BUNDLED TINYALSA TINYMIX ==='
            echo 'PhoneBridge only invokes read-only mixer queries.'
            echo '--- Tool version ---'
            "$binary" --version 2>&1
            echo '--- Mixer controls ---'
            "$binary" controls 2>&1
            controls_exit=$?
            printf 'controls_exit=%s\n' "${'$'}controls_exit"
            echo '--- Mixer current values ---'
            "$binary" contents 2>&1
            contents_exit=$?
            printf 'contents_exit=%s\n' "${'$'}contents_exit"
        """.trimIndent()
        return try {
            val process = ProcessBuilder("su", "-c", script).redirectErrorStream(true).start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val buffer = StringBuilder()
            while (true) {
                val line = reader.readLine() ?: break
                if (buffer.length < 22000) buffer.append(line).append('\n')
            }
            reader.close()
            val exitStatus = process.waitFor().toString()
            "Tool=TinyALSA tinymix\nABI=$abi\nexit=$exitStatus\n" + buffer.toString()
        } catch (e: Throwable) {
            "Tinymix diagnostics failed: " + e.javaClass.simpleName + ": " + (e.message ?: "")
        }
    }

    private fun collectRootAudioDiagnostics(): String {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val prefs = getSharedPreferences(BridgeService.PREFS, MODE_PRIVATE)
        val tablet = prefs.getString("tablet_audio_diag", "No tablet telemetry received.") ?: "No tablet telemetry received."
        val captureAudioOutputPermission =
            if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission("android.permission.CAPTURE_AUDIO_OUTPUT") == PackageManager.PERMISSION_GRANTED
            ) "GRANTED" else "NOT_GRANTED"
        val capturedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date())
        val header = "PhoneBridge live call-audio snapshot" +
            "\nCapturedAt=" + capturedAt +
            "\nCallState=" + PhoneCallController.currentState +
            "\nAudioManagerMode=" + audioManager.mode +
            "\nSpeakerphoneOn=" + audioManager.isSpeakerphoneOn +
            "\nMicrophoneMuted=" + audioManager.isMicrophoneMute +
            "\nCaptureAudioOutputPermission=" + captureAudioOutputPermission +
            "\n\n=== PhoneBridge runtime metrics ===\n" + AudioBridge.diagnosticSummary() +
            "\n\n=== Last tablet telemetry ===\n" + tablet +
            "\n\n=== Live root audio state ===\n"

        val script = """
            set +e
            echo '=== LIVE VOICE PCM SUBSTREAM STATE ==='
            for d in /proc/asound/card0/pcm2c /proc/asound/card0/pcm17c /proc/asound/card0/pcm12c /proc/asound/card0/pcm9c /proc/asound/card0/pcm13c; do
                if [ -d "${'$'}d" ]; then
                    for f in "${'$'}d"/sub*/status "${'$'}d"/sub*/hw_params; do
                        if [ -f "${'$'}f" ]; then echo "--- ${'$'}f"; cat "${'$'}f" 2>&1; fi
                    done
                fi
            done
            echo '=== LIVE PCM OWNERS ==='
            for fd in /proc/[0-9]*/fd/*; do
                target=${'$'}(readlink "${'$'}fd" 2>/dev/null)
                case "${'$'}target" in
                    *pcmC0D2c*|*pcmC0D17c*|*pcmC0D12c*|*pcmC0D9c*|*pcmC0D13c*|*pcmC0D2p*|*pcmC0D17p*|*pcmC0D10p*|*pcmC0D11p*)
                        rest=${'$'}{fd#/proc/}; pid=${'$'}{rest%%/*}
                        printf 'pid=%s comm=' "${'$'}pid"; cat "/proc/${'$'}pid/comm" 2>/dev/null
                        echo "pcmFd=${'$'}fd device=${'$'}target"
                        ;;
                esac
            done
            echo '=== CURRENT AUDIO ROUTE / RECORDING LINES ==='
            dumpsys audio 2>&1 | grep -iE 'mode:|phone state|speakerphone|sco|communication|stream.*volume|recording|input device|audio focus' | head -n 55
            dumpsys telecom 2>&1 | grep -iE 'CallAudioState|audioRoute|supportedRouteMask|activeBluetoothDevice' | head -n 15
            echo '=== ACTIVE AUDIO FLINGER RECORDING LINES ==='
            dumpsys media.audio_flinger 2>&1 | grep -iE 'RecordThread|Record Track|record thread|Input device|active client|standby|VOICE_CALL|VOICE_DOWNLINK' | head -n 55
        """.trimIndent()

        var output = ""
        var exitStatus = "unknown"
        try {
            val process = ProcessBuilder("su", "-c", script).redirectErrorStream(true).start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val buffer = StringBuilder()
            while (true) {
                val line = reader.readLine() ?: break
                if (buffer.length < 14000) buffer.append(line).append('\n')
            }
            reader.close()
            exitStatus = process.waitFor().toString()
            output = buffer.toString()
        } catch (e: Throwable) {
            output = "Root diagnostics failed: " + e.javaClass.simpleName + ": " + (e.message ?: "")
        }
        return header + "exit=" + exitStatus + "\n" + output.take(14000)
    }

    private fun copyDiagnosticReport() {
        val prefs = getSharedPreferences(BridgeService.PREFS, MODE_PRIVATE)
        val latest = rootDiagnosticsReport +
            "\n\n=== IDLE VS ACTIVE-CALL TINYMIX COMPARISON ===\n" +
            (if (tinymixComparisonReport.isBlank()) "Not run yet." else tinymixComparisonReport) +
            "\n\n=== BUNDLED TINYMIX REPORT ===\n" +
            (if (tinymixDiagnosticsReport.isBlank()) "Not run yet." else tinymixDiagnosticsReport) +
            "\n\n=== LIVE SNAPSHOT ===" +
            "\nCallState=" + PhoneCallController.currentState +
            "\nPhone=" + AudioBridge.diagnosticSummary() +
            "\nTablet=" + (prefs.getString("tablet_audio_diag", "No tablet telemetry received.") ?: "No tablet telemetry received.")
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("PhoneBridge audio diagnostics", latest))
        Toast.makeText(this, "Diagnostic report copied", Toast.LENGTH_SHORT).show()
    }

    private fun buildCallScreen(mode: String) {
        val incoming = mode == MODE_INCOMING
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(4), dp(18), dp(4), dp(10))
        }
        top.addView(makeText("PhoneBridge", 20f, COLOR_TEXT, true, Gravity.CENTER))
        top.addView(makeText(
            if (incoming) "INCOMING CALL" else "CALL IN PROGRESS",
            10f,
            if (incoming) COLOR_GREEN else COLOR_MUTED,
            true,
            Gravity.CENTER
        ), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(5) })
        root.addView(top, LinearLayout.LayoutParams(-1, -2))
        if (!incoming) {
            root.addView(actionButton("Bridge diagnostics", COLOR_PANEL, COLOR_TEXT, {
                selectedTab = TAB_BRIDGE
                renderScreen(MODE_HOME)
            }), LinearLayout.LayoutParams(-1, dp(44)).apply {
                topMargin = dp(8)
                bottomMargin = dp(4)
            })
        }
        root.addView(View(this), LinearLayout.LayoutParams(-1, 0, 0.6f))

        val avatar = makeText("☎", 44f, COLOR_TEXT, false, Gravity.CENTER)
        avatar.background = rounded(COLOR_SELECTED, 80)
        root.addView(avatar, LinearLayout.LayoutParams(dp(112), dp(112)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        })
        activeNumberView = makeText("Unknown caller", 25f, COLOR_TEXT, true, Gravity.CENTER)
        root.addView(activeNumberView, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(20)
        })
        activeStatusView = makeText(
            if (incoming) "Calling you" else "Connecting…",
            14f, COLOR_GREEN, false, Gravity.CENTER
        )
        root.addView(activeStatusView, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(8)
        })
        activeTimerView = makeText("", 13f, COLOR_MUTED, false, Gravity.CENTER)
        root.addView(activeTimerView, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(4)
        })
        root.addView(View(this), LinearLayout.LayoutParams(-1, 0, 0.65f))

        if (incoming) {
            val actions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            actions.addView(actionButton("Decline", COLOR_RED, Color.WHITE, {
                localResult = PhoneCallController.reject()
            }), LinearLayout.LayoutParams(0, dp(58), 1f).apply {
                rightMargin = dp(8)
            })
            actions.addView(actionButton("Answer", COLOR_GREEN, COLOR_DARK_TEXT, {
                localResult = PhoneCallController.answer()
            }), LinearLayout.LayoutParams(0, dp(58), 1f).apply {
                leftMargin = dp(8)
            })
            root.addView(actions, LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(28)
            })
        } else {
            val controls = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            controls.addView(callOption("Mute", PhoneCallController.isMuted(), {
                PhoneCallController.setMuted(!PhoneCallController.isMuted())
                renderScreen(MODE_ACTIVE_CALL)
            }), LinearLayout.LayoutParams(0, dp(78), 1f).apply {
                rightMargin = dp(5)
            })
            controls.addView(callOption("Speaker", PhoneCallController.isSpeakerEnabled(), {
                PhoneCallController.setSpeakerEnabled(!PhoneCallController.isSpeakerEnabled())
                renderScreen(MODE_ACTIVE_CALL)
            }), LinearLayout.LayoutParams(0, dp(78), 1f).apply {
                leftMargin = dp(5)
                rightMargin = dp(5)
            })
            controls.addView(callOption(if (showCallKeypad) "Hide pad" else "Keypad", showCallKeypad, {
                showCallKeypad = !showCallKeypad
                renderScreen(MODE_ACTIVE_CALL)
            }), LinearLayout.LayoutParams(0, dp(78), 1f).apply {
                leftMargin = dp(5)
            })
            root.addView(controls, LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(12)
            })
            if (showCallKeypad) {
                addInCallKeypad()
            }
            root.addView(actionButton("End call", COLOR_RED, Color.WHITE, {
                localResult = PhoneCallController.hangup()
            }), LinearLayout.LayoutParams(-1, dp(56)).apply {
                bottomMargin = dp(16)
            })
        }
        updateCallScreen(
            getSharedPreferences(BridgeService.PREFS, MODE_PRIVATE)
                .getString(BridgeService.KEY_CALL_STATE, "IDLE") ?: "IDLE"
        )
    }

    private fun addInCallKeypad() {
        val rows = arrayOf(
            arrayOf("1", "2", "3"),
            arrayOf("4", "5", "6"),
            arrayOf("7", "8", "9"),
            arrayOf("*", "0", "#")
        )
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(COLOR_PANEL, 16)
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        rows.forEach { rowKeys ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            rowKeys.forEach { digit ->
                val key = actionButton(digit, COLOR_SELECTED, COLOR_TEXT) {
                    PhoneCallController.sendDtmf(digit[0])
                }
                row.addView(key, LinearLayout.LayoutParams(0, dp(37), 1f).apply {
                    leftMargin = dp(3)
                    rightMargin = dp(3)
                    bottomMargin = dp(4)
                })
            }
            panel.addView(row, LinearLayout.LayoutParams(-1, -2))
        }
        root.addView(panel, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(10)
        })
    }

    private fun callOption(title: String, active: Boolean, onClick: () -> Unit): LinearLayout {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(if (active) COLOR_SELECTED else COLOR_PANEL, 18)
            setPadding(dp(4), dp(8), dp(4), dp(8))
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
        val symbol = when (title) {
            "Mute" -> "M"
            "Speaker" -> "S"
            else -> "⌨"
        }
        item.addView(makeText(symbol, 18f, if (active) COLOR_GREEN else COLOR_TEXT, true, Gravity.CENTER))
        item.addView(makeText(title, 11f, COLOR_TEXT, active, Gravity.CENTER),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
        return item
    }

    private fun updateCallScreen(state: String) {
        val prefs = getSharedPreferences(BridgeService.PREFS, MODE_PRIVATE)
        val phoneNumber = prefs.getString(BridgeService.KEY_CALL_NUMBER, "") ?: ""
        activeNumberView?.text = phoneNumber.ifBlank { "Unknown caller" }
        activeStatusView?.text = when (state) {
            "RINGING" -> "Calling you"
            "DIALING" -> "Dialling…"
            "CONNECTING" -> "Connecting…"
            "OFFHOOK" -> "Connected"
            else -> state
        }
        val startedAt = prefs.getLong(BridgeService.KEY_CALL_STARTED_AT, 0L)
        activeTimerView?.text = if (startedAt > 0L && state == "OFFHOOK") {
            val seconds = ((System.currentTimeMillis() - startedAt) / 1000L).coerceAtLeast(0L)
            String.format(
                Locale.getDefault(),
                "%02d:%02d",
                seconds / 60L,
                seconds % 60L
            )
        } else {
            if (state == "RINGING") "Incoming cellular call" else " "
        }
    }

    private fun handleDialIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_DIAL) return
        val uri: Uri? = intent.data
        val dialNumber = uri?.schemeSpecificPart ?: ""
        pendingDialNumber = dialNumber
        selectedTab = TAB_KEYPAD
        if (::number.isInitialized) {
            number.setText(dialNumber)
            number.setSelection(number.text.length)
            pendingDialNumber = null
        }
    }

    private fun requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT < 23) {
            startBridgeService()
            return
        }
        val requested = arrayOf(
            android.Manifest.permission.CALL_PHONE,
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.RECORD_AUDIO
        )
        val missing = requested.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            startBridgeService()
        } else {
            requestPermissions(missing.toTypedArray(), 100)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            startBridgeService()
            val callAllowed =
                checkSelfPermission(android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
            val micAllowed =
                checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            localResult = when {
                !callAllowed -> "Grant Phone permission to place calls."
                !micAllowed -> "Call control is available; microphone permission is needed for Wi-Fi audio tests."
                else -> "Permissions ready."
            }
        }
    }

    private fun requestDefaultDialer() {
        if (Build.VERSION.SDK_INT < 23) return
        try {
            val telecom = getSystemService(TELECOM_SERVICE) as TelecomManager
            if (packageName == telecom.defaultDialerPackage) {
                localResult = "PhoneBridge is already the default phone app."
                Toast.makeText(this, localResult, Toast.LENGTH_SHORT).show()
                return
            }
            startActivity(
                Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                    putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
                }
            )
        } catch (e: Throwable) {
            localResult = "Could not open default-phone-app selection: " + e.javaClass.simpleName
            Toast.makeText(this, localResult, Toast.LENGTH_LONG).show()
        }
    }

    private fun isDefaultDialer(): Boolean {
        return try {
            packageName == (getSystemService(TELECOM_SERVICE) as TelecomManager).defaultDialerPackage
        } catch (_: Throwable) {
            false
        }
    }

    private fun startBridgeService() {
        startService(Intent(this, BridgeService::class.java))
    }

    private fun actionButton(
        label: String,
        backgroundColor: Int,
        textColor: Int,
        onClick: () -> Unit
    ): TextView {
        val view = makeText(label, 14f, textColor, true, Gravity.CENTER)
        view.background = rounded(backgroundColor, 18)
        view.setPadding(dp(12), dp(8), dp(12), dp(8))
        view.isClickable = true
        view.isFocusable = true
        view.setOnClickListener { onClick() }
        return view
    }

    private fun makeText(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false,
        gravityValue: Int = Gravity.START
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = gravityValue
            includeFontPadding = false
            if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
    }

    private fun rounded(color: Int, radiusDp: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp).toFloat()
            setColor(color)
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()

    override fun onDestroy() {
        handler.removeCallbacks(refresh)
        super.onDestroy()
    }

    companion object {
        const val ACTION_SHOW_CALL = "com.dsann.phonebridge.SHOW_CALL"
        private const val MODE_HOME = "HOME"
        private const val MODE_INCOMING = "INCOMING"
        private const val MODE_ACTIVE_CALL = "ACTIVE_CALL"
        private const val TAB_KEYPAD = "KEYPAD"
        private const val TAB_RECENTS = "RECENTS"
        private const val TAB_BRIDGE = "BRIDGE"

        private val COLOR_BG = Color.rgb(12, 15, 22)
        private val COLOR_PANEL = Color.rgb(25, 30, 40)
        private val COLOR_SELECTED = Color.rgb(37, 48, 65)
        private val COLOR_LINE = Color.rgb(43, 50, 64)
        private val COLOR_TEXT = Color.rgb(245, 247, 251)
        private val COLOR_MUTED = Color.rgb(150, 161, 179)
        private val COLOR_GREEN = Color.rgb(90, 226, 157)
        private val COLOR_RED = Color.rgb(235, 83, 99)
        private val COLOR_AMBER = Color.rgb(255, 190, 90)
        private val COLOR_DARK_TEXT = Color.rgb(10, 35, 25)
    }
}
}controls_exit"
            echo '--- Mixer current values ---'
            "$binary" contents 2>&1
            contents_exit=$?
            printf 'contents_exit=%s\n' "${'
        """.trimIndent()
        return try {
            val process = ProcessBuilder("su", "-c", script).redirectErrorStream(true).start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val buffer = StringBuilder()
            while (true) {
                val line = reader.readLine() ?: break
                if (buffer.length < 22000) buffer.append(line).append('\n')
            }
            reader.close()
            val exitStatus = process.waitFor().toString()
            "Tool=TinyALSA tinymix\nABI=$abi\nexit=$exitStatus\n" + buffer.toString()
        } catch (e: Throwable) {
            "Tinymix diagnostics failed: " + e.javaClass.simpleName + ": " + (e.message ?: "")
        }
    }

    private fun collectRootAudioDiagnostics(): String {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val prefs = getSharedPreferences(BridgeService.PREFS, MODE_PRIVATE)
        val tablet = prefs.getString("tablet_audio_diag", "No tablet telemetry received.") ?: "No tablet telemetry received."
        val captureAudioOutputPermission =
            if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission("android.permission.CAPTURE_AUDIO_OUTPUT") == PackageManager.PERMISSION_GRANTED
            ) "GRANTED" else "NOT_GRANTED"
        val capturedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date())
        val header = "PhoneBridge live call-audio snapshot" +
            "\nCapturedAt=" + capturedAt +
            "\nCallState=" + PhoneCallController.currentState +
            "\nAudioManagerMode=" + audioManager.mode +
            "\nSpeakerphoneOn=" + audioManager.isSpeakerphoneOn +
            "\nMicrophoneMuted=" + audioManager.isMicrophoneMute +
            "\nCaptureAudioOutputPermission=" + captureAudioOutputPermission +
            "\n\n=== PhoneBridge runtime metrics ===\n" + AudioBridge.diagnosticSummary() +
            "\n\n=== Last tablet telemetry ===\n" + tablet +
            "\n\n=== Live root audio state ===\n"

        val script = """
            set +e
            echo '=== LIVE VOICE PCM SUBSTREAM STATE ==='
            for d in /proc/asound/card0/pcm2c /proc/asound/card0/pcm17c /proc/asound/card0/pcm12c /proc/asound/card0/pcm9c /proc/asound/card0/pcm13c; do
                if [ -d "${'$'}d" ]; then
                    for f in "${'$'}d"/sub*/status "${'$'}d"/sub*/hw_params; do
                        if [ -f "${'$'}f" ]; then echo "--- ${'$'}f"; cat "${'$'}f" 2>&1; fi
                    done
                fi
            done
            echo '=== LIVE PCM OWNERS ==='
            for fd in /proc/[0-9]*/fd/*; do
                target=${'$'}(readlink "${'$'}fd" 2>/dev/null)
                case "${'$'}target" in
                    *pcmC0D2c*|*pcmC0D17c*|*pcmC0D12c*|*pcmC0D9c*|*pcmC0D13c*|*pcmC0D2p*|*pcmC0D17p*|*pcmC0D10p*|*pcmC0D11p*)
                        rest=${'$'}{fd#/proc/}; pid=${'$'}{rest%%/*}
                        printf 'pid=%s comm=' "${'$'}pid"; cat "/proc/${'$'}pid/comm" 2>/dev/null
                        echo "pcmFd=${'$'}fd device=${'$'}target"
                        ;;
                esac
            done
            echo '=== CURRENT AUDIO ROUTE / RECORDING LINES ==='
            dumpsys audio 2>&1 | grep -iE 'mode:|phone state|speakerphone|sco|communication|stream.*volume|recording|input device|audio focus' | head -n 55
            dumpsys telecom 2>&1 | grep -iE 'CallAudioState|audioRoute|supportedRouteMask|activeBluetoothDevice' | head -n 15
            echo '=== ACTIVE AUDIO FLINGER RECORDING LINES ==='
            dumpsys media.audio_flinger 2>&1 | grep -iE 'RecordThread|Record Track|record thread|Input device|active client|standby|VOICE_CALL|VOICE_DOWNLINK' | head -n 55
        """.trimIndent()

        var output = ""
        var exitStatus = "unknown"
        try {
            val process = ProcessBuilder("su", "-c", script).redirectErrorStream(true).start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val buffer = StringBuilder()
            while (true) {
                val line = reader.readLine() ?: break
                if (buffer.length < 14000) buffer.append(line).append('\n')
            }
            reader.close()
            exitStatus = process.waitFor().toString()
            output = buffer.toString()
        } catch (e: Throwable) {
            output = "Root diagnostics failed: " + e.javaClass.simpleName + ": " + (e.message ?: "")
        }
        return header + "exit=" + exitStatus + "\n" + output.take(14000)
    }

    private fun copyDiagnosticReport() {
        val prefs = getSharedPreferences(BridgeService.PREFS, MODE_PRIVATE)
        val latest = rootDiagnosticsReport +
            "\n\n=== BUNDLED TINYMIX REPORT ===\n" +
            (if (tinymixDiagnosticsReport.isBlank()) "Not run yet." else tinymixDiagnosticsReport) +
            "\n\n=== LIVE SNAPSHOT ===" +
            "\nCallState=" + PhoneCallController.currentState +
            "\nPhone=" + AudioBridge.diagnosticSummary() +
            "\nTablet=" + (prefs.getString("tablet_audio_diag", "No tablet telemetry received.") ?: "No tablet telemetry received.")
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("PhoneBridge audio diagnostics", latest))
        Toast.makeText(this, "Diagnostic report copied", Toast.LENGTH_SHORT).show()
    }

    private fun buildCallScreen(mode: String) {
        val incoming = mode == MODE_INCOMING
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(4), dp(18), dp(4), dp(10))
        }
        top.addView(makeText("PhoneBridge", 20f, COLOR_TEXT, true, Gravity.CENTER))
        top.addView(makeText(
            if (incoming) "INCOMING CALL" else "CALL IN PROGRESS",
            10f,
            if (incoming) COLOR_GREEN else COLOR_MUTED,
            true,
            Gravity.CENTER
        ), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(5) })
        root.addView(top, LinearLayout.LayoutParams(-1, -2))
        if (!incoming) {
            root.addView(actionButton("Bridge diagnostics", COLOR_PANEL, COLOR_TEXT, {
                selectedTab = TAB_BRIDGE
                renderScreen(MODE_HOME)
            }), LinearLayout.LayoutParams(-1, dp(44)).apply {
                topMargin = dp(8)
                bottomMargin = dp(4)
            })
        }
        root.addView(View(this), LinearLayout.LayoutParams(-1, 0, 0.6f))

        val avatar = makeText("☎", 44f, COLOR_TEXT, false, Gravity.CENTER)
        avatar.background = rounded(COLOR_SELECTED, 80)
        root.addView(avatar, LinearLayout.LayoutParams(dp(112), dp(112)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        })
        activeNumberView = makeText("Unknown caller", 25f, COLOR_TEXT, true, Gravity.CENTER)
        root.addView(activeNumberView, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(20)
        })
        activeStatusView = makeText(
            if (incoming) "Calling you" else "Connecting…",
            14f, COLOR_GREEN, false, Gravity.CENTER
        )
        root.addView(activeStatusView, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(8)
        })
        activeTimerView = makeText("", 13f, COLOR_MUTED, false, Gravity.CENTER)
        root.addView(activeTimerView, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(4)
        })
        root.addView(View(this), LinearLayout.LayoutParams(-1, 0, 0.65f))

        if (incoming) {
            val actions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            actions.addView(actionButton("Decline", COLOR_RED, Color.WHITE, {
                localResult = PhoneCallController.reject()
            }), LinearLayout.LayoutParams(0, dp(58), 1f).apply {
                rightMargin = dp(8)
            })
            actions.addView(actionButton("Answer", COLOR_GREEN, COLOR_DARK_TEXT, {
                localResult = PhoneCallController.answer()
            }), LinearLayout.LayoutParams(0, dp(58), 1f).apply {
                leftMargin = dp(8)
            })
            root.addView(actions, LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(28)
            })
        } else {
            val controls = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            controls.addView(callOption("Mute", PhoneCallController.isMuted(), {
                PhoneCallController.setMuted(!PhoneCallController.isMuted())
                renderScreen(MODE_ACTIVE_CALL)
            }), LinearLayout.LayoutParams(0, dp(78), 1f).apply {
                rightMargin = dp(5)
            })
            controls.addView(callOption("Speaker", PhoneCallController.isSpeakerEnabled(), {
                PhoneCallController.setSpeakerEnabled(!PhoneCallController.isSpeakerEnabled())
                renderScreen(MODE_ACTIVE_CALL)
            }), LinearLayout.LayoutParams(0, dp(78), 1f).apply {
                leftMargin = dp(5)
                rightMargin = dp(5)
            })
            controls.addView(callOption(if (showCallKeypad) "Hide pad" else "Keypad", showCallKeypad, {
                showCallKeypad = !showCallKeypad
                renderScreen(MODE_ACTIVE_CALL)
            }), LinearLayout.LayoutParams(0, dp(78), 1f).apply {
                leftMargin = dp(5)
            })
            root.addView(controls, LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(12)
            })
            if (showCallKeypad) {
                addInCallKeypad()
            }
            root.addView(actionButton("End call", COLOR_RED, Color.WHITE, {
                localResult = PhoneCallController.hangup()
            }), LinearLayout.LayoutParams(-1, dp(56)).apply {
                bottomMargin = dp(16)
            })
        }
        updateCallScreen(
            getSharedPreferences(BridgeService.PREFS, MODE_PRIVATE)
                .getString(BridgeService.KEY_CALL_STATE, "IDLE") ?: "IDLE"
        )
    }

    private fun addInCallKeypad() {
        val rows = arrayOf(
            arrayOf("1", "2", "3"),
            arrayOf("4", "5", "6"),
            arrayOf("7", "8", "9"),
            arrayOf("*", "0", "#")
        )
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(COLOR_PANEL, 16)
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        rows.forEach { rowKeys ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            rowKeys.forEach { digit ->
                val key = actionButton(digit, COLOR_SELECTED, COLOR_TEXT) {
                    PhoneCallController.sendDtmf(digit[0])
                }
                row.addView(key, LinearLayout.LayoutParams(0, dp(37), 1f).apply {
                    leftMargin = dp(3)
                    rightMargin = dp(3)
                    bottomMargin = dp(4)
                })
            }
            panel.addView(row, LinearLayout.LayoutParams(-1, -2))
        }
        root.addView(panel, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(10)
        })
    }

    private fun callOption(title: String, active: Boolean, onClick: () -> Unit): LinearLayout {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(if (active) COLOR_SELECTED else COLOR_PANEL, 18)
            setPadding(dp(4), dp(8), dp(4), dp(8))
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
        val symbol = when (title) {
            "Mute" -> "M"
            "Speaker" -> "S"
            else -> "⌨"
        }
        item.addView(makeText(symbol, 18f, if (active) COLOR_GREEN else COLOR_TEXT, true, Gravity.CENTER))
        item.addView(makeText(title, 11f, COLOR_TEXT, active, Gravity.CENTER),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
        return item
    }

    private fun updateCallScreen(state: String) {
        val prefs = getSharedPreferences(BridgeService.PREFS, MODE_PRIVATE)
        val phoneNumber = prefs.getString(BridgeService.KEY_CALL_NUMBER, "") ?: ""
        activeNumberView?.text = phoneNumber.ifBlank { "Unknown caller" }
        activeStatusView?.text = when (state) {
            "RINGING" -> "Calling you"
            "DIALING" -> "Dialling…"
            "CONNECTING" -> "Connecting…"
            "OFFHOOK" -> "Connected"
            else -> state
        }
        val startedAt = prefs.getLong(BridgeService.KEY_CALL_STARTED_AT, 0L)
        activeTimerView?.text = if (startedAt > 0L && state == "OFFHOOK") {
            val seconds = ((System.currentTimeMillis() - startedAt) / 1000L).coerceAtLeast(0L)
            String.format(
                Locale.getDefault(),
                "%02d:%02d",
                seconds / 60L,
                seconds % 60L
            )
        } else {
            if (state == "RINGING") "Incoming cellular call" else " "
        }
    }

    private fun handleDialIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_DIAL) return
        val uri: Uri? = intent.data
        val dialNumber = uri?.schemeSpecificPart ?: ""
        pendingDialNumber = dialNumber
        selectedTab = TAB_KEYPAD
        if (::number.isInitialized) {
            number.setText(dialNumber)
            number.setSelection(number.text.length)
            pendingDialNumber = null
        }
    }

    private fun requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT < 23) {
            startBridgeService()
            return
        }
        val requested = arrayOf(
            android.Manifest.permission.CALL_PHONE,
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.RECORD_AUDIO
        )
        val missing = requested.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            startBridgeService()
        } else {
            requestPermissions(missing.toTypedArray(), 100)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            startBridgeService()
            val callAllowed =
                checkSelfPermission(android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
            val micAllowed =
                checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            localResult = when {
                !callAllowed -> "Grant Phone permission to place calls."
                !micAllowed -> "Call control is available; microphone permission is needed for Wi-Fi audio tests."
                else -> "Permissions ready."
            }
        }
    }

    private fun requestDefaultDialer() {
        if (Build.VERSION.SDK_INT < 23) return
        try {
            val telecom = getSystemService(TELECOM_SERVICE) as TelecomManager
            if (packageName == telecom.defaultDialerPackage) {
                localResult = "PhoneBridge is already the default phone app."
                Toast.makeText(this, localResult, Toast.LENGTH_SHORT).show()
                return
            }
            startActivity(
                Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                    putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
                }
            )
        } catch (e: Throwable) {
            localResult = "Could not open default-phone-app selection: " + e.javaClass.simpleName
            Toast.makeText(this, localResult, Toast.LENGTH_LONG).show()
        }
    }

    private fun isDefaultDialer(): Boolean {
        return try {
            packageName == (getSystemService(TELECOM_SERVICE) as TelecomManager).defaultDialerPackage
        } catch (_: Throwable) {
            false
        }
    }

    private fun startBridgeService() {
        startService(Intent(this, BridgeService::class.java))
    }

    private fun actionButton(
        label: String,
        backgroundColor: Int,
        textColor: Int,
        onClick: () -> Unit
    ): TextView {
        val view = makeText(label, 14f, textColor, true, Gravity.CENTER)
        view.background = rounded(backgroundColor, 18)
        view.setPadding(dp(12), dp(8), dp(12), dp(8))
        view.isClickable = true
        view.isFocusable = true
        view.setOnClickListener { onClick() }
        return view
    }

    private fun makeText(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false,
        gravityValue: Int = Gravity.START
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = gravityValue
            includeFontPadding = false
            if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
    }

    private fun rounded(color: Int, radiusDp: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp).toFloat()
            setColor(color)
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()

    override fun onDestroy() {
        handler.removeCallbacks(refresh)
        super.onDestroy()
    }

    companion object {
        const val ACTION_SHOW_CALL = "com.dsann.phonebridge.SHOW_CALL"
        private const val MODE_HOME = "HOME"
        private const val MODE_INCOMING = "INCOMING"
        private const val MODE_ACTIVE_CALL = "ACTIVE_CALL"
        private const val TAB_KEYPAD = "KEYPAD"
        private const val TAB_RECENTS = "RECENTS"
        private const val TAB_BRIDGE = "BRIDGE"

        private val COLOR_BG = Color.rgb(12, 15, 22)
        private val COLOR_PANEL = Color.rgb(25, 30, 40)
        private val COLOR_SELECTED = Color.rgb(37, 48, 65)
        private val COLOR_LINE = Color.rgb(43, 50, 64)
        private val COLOR_TEXT = Color.rgb(245, 247, 251)
        private val COLOR_MUTED = Color.rgb(150, 161, 179)
        private val COLOR_GREEN = Color.rgb(90, 226, 157)
        private val COLOR_RED = Color.rgb(235, 83, 99)
        private val COLOR_AMBER = Color.rgb(255, 190, 90)
        private val COLOR_DARK_TEXT = Color.rgb(10, 35, 25)
    }
}
}contents_exit"
        """.trimIndent()
        return try {
            val process = ProcessBuilder("su", "-c", script).redirectErrorStream(true).start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val buffer = StringBuilder()
            while (true) {
                val line = reader.readLine() ?: break
                if (buffer.length < 22000) buffer.append(line).append('\n')
            }
            reader.close()
            val exitStatus = process.waitFor().toString()
            "Tool=TinyALSA tinymix\nABI=$abi\nexit=$exitStatus\n" + buffer.toString()
        } catch (e: Throwable) {
            "Tinymix diagnostics failed: " + e.javaClass.simpleName + ": " + (e.message ?: "")
        }
    }

    private fun collectRootAudioDiagnostics(): String {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val prefs = getSharedPreferences(BridgeService.PREFS, MODE_PRIVATE)
        val tablet = prefs.getString("tablet_audio_diag", "No tablet telemetry received.") ?: "No tablet telemetry received."
        val captureAudioOutputPermission =
            if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission("android.permission.CAPTURE_AUDIO_OUTPUT") == PackageManager.PERMISSION_GRANTED
            ) "GRANTED" else "NOT_GRANTED"
        val capturedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date())
        val header = "PhoneBridge live call-audio snapshot" +
            "\nCapturedAt=" + capturedAt +
            "\nCallState=" + PhoneCallController.currentState +
            "\nAudioManagerMode=" + audioManager.mode +
            "\nSpeakerphoneOn=" + audioManager.isSpeakerphoneOn +
            "\nMicrophoneMuted=" + audioManager.isMicrophoneMute +
            "\nCaptureAudioOutputPermission=" + captureAudioOutputPermission +
            "\n\n=== PhoneBridge runtime metrics ===\n" + AudioBridge.diagnosticSummary() +
            "\n\n=== Last tablet telemetry ===\n" + tablet +
            "\n\n=== Live root audio state ===\n"

        val script = """
            set +e
            echo '=== LIVE VOICE PCM SUBSTREAM STATE ==='
            for d in /proc/asound/card0/pcm2c /proc/asound/card0/pcm17c /proc/asound/card0/pcm12c /proc/asound/card0/pcm9c /proc/asound/card0/pcm13c; do
                if [ -d "${'$'}d" ]; then
                    for f in "${'$'}d"/sub*/status "${'$'}d"/sub*/hw_params; do
                        if [ -f "${'$'}f" ]; then echo "--- ${'$'}f"; cat "${'$'}f" 2>&1; fi
                    done
                fi
            done
            echo '=== LIVE PCM OWNERS ==='
            for fd in /proc/[0-9]*/fd/*; do
                target=${'$'}(readlink "${'$'}fd" 2>/dev/null)
                case "${'$'}target" in
                    *pcmC0D2c*|*pcmC0D17c*|*pcmC0D12c*|*pcmC0D9c*|*pcmC0D13c*|*pcmC0D2p*|*pcmC0D17p*|*pcmC0D10p*|*pcmC0D11p*)
                        rest=${'$'}{fd#/proc/}; pid=${'$'}{rest%%/*}
                        printf 'pid=%s comm=' "${'$'}pid"; cat "/proc/${'$'}pid/comm" 2>/dev/null
                        echo "pcmFd=${'$'}fd device=${'$'}target"
                        ;;
                esac
            done
            echo '=== CURRENT AUDIO ROUTE / RECORDING LINES ==='
            dumpsys audio 2>&1 | grep -iE 'mode:|phone state|speakerphone|sco|communication|stream.*volume|recording|input device|audio focus' | head -n 55
            dumpsys telecom 2>&1 | grep -iE 'CallAudioState|audioRoute|supportedRouteMask|activeBluetoothDevice' | head -n 15
            echo '=== ACTIVE AUDIO FLINGER RECORDING LINES ==='
            dumpsys media.audio_flinger 2>&1 | grep -iE 'RecordThread|Record Track|record thread|Input device|active client|standby|VOICE_CALL|VOICE_DOWNLINK' | head -n 55
        """.trimIndent()

        var output = ""
        var exitStatus = "unknown"
        try {
            val process = ProcessBuilder("su", "-c", script).redirectErrorStream(true).start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val buffer = StringBuilder()
            while (true) {
                val line = reader.readLine() ?: break
                if (buffer.length < 14000) buffer.append(line).append('\n')
            }
            reader.close()
            exitStatus = process.waitFor().toString()
            output = buffer.toString()
        } catch (e: Throwable) {
            output = "Root diagnostics failed: " + e.javaClass.simpleName + ": " + (e.message ?: "")
        }
        return header + "exit=" + exitStatus + "\n" + output.take(14000)
    }

    private fun copyDiagnosticReport() {
        val prefs = getSharedPreferences(BridgeService.PREFS, MODE_PRIVATE)
        val latest = rootDiagnosticsReport +
            "\n\n=== BUNDLED TINYMIX REPORT ===\n" +
            (if (tinymixDiagnosticsReport.isBlank()) "Not run yet." else tinymixDiagnosticsReport) +
            "\n\n=== LIVE SNAPSHOT ===" +
            "\nCallState=" + PhoneCallController.currentState +
            "\nPhone=" + AudioBridge.diagnosticSummary() +
            "\nTablet=" + (prefs.getString("tablet_audio_diag", "No tablet telemetry received.") ?: "No tablet telemetry received.")
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("PhoneBridge audio diagnostics", latest))
        Toast.makeText(this, "Diagnostic report copied", Toast.LENGTH_SHORT).show()
    }

    private fun buildCallScreen(mode: String) {
        val incoming = mode == MODE_INCOMING
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(4), dp(18), dp(4), dp(10))
        }
        top.addView(makeText("PhoneBridge", 20f, COLOR_TEXT, true, Gravity.CENTER))
        top.addView(makeText(
            if (incoming) "INCOMING CALL" else "CALL IN PROGRESS",
            10f,
            if (incoming) COLOR_GREEN else COLOR_MUTED,
            true,
            Gravity.CENTER
        ), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(5) })
        root.addView(top, LinearLayout.LayoutParams(-1, -2))
        if (!incoming) {
            root.addView(actionButton("Bridge diagnostics", COLOR_PANEL, COLOR_TEXT, {
                selectedTab = TAB_BRIDGE
                renderScreen(MODE_HOME)
            }), LinearLayout.LayoutParams(-1, dp(44)).apply {
                topMargin = dp(8)
                bottomMargin = dp(4)
            })
        }
        root.addView(View(this), LinearLayout.LayoutParams(-1, 0, 0.6f))

        val avatar = makeText("☎", 44f, COLOR_TEXT, false, Gravity.CENTER)
        avatar.background = rounded(COLOR_SELECTED, 80)
        root.addView(avatar, LinearLayout.LayoutParams(dp(112), dp(112)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        })
        activeNumberView = makeText("Unknown caller", 25f, COLOR_TEXT, true, Gravity.CENTER)
        root.addView(activeNumberView, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(20)
        })
        activeStatusView = makeText(
            if (incoming) "Calling you" else "Connecting…",
            14f, COLOR_GREEN, false, Gravity.CENTER
        )
        root.addView(activeStatusView, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(8)
        })
        activeTimerView = makeText("", 13f, COLOR_MUTED, false, Gravity.CENTER)
        root.addView(activeTimerView, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(4)
        })
        root.addView(View(this), LinearLayout.LayoutParams(-1, 0, 0.65f))

        if (incoming) {
            val actions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            actions.addView(actionButton("Decline", COLOR_RED, Color.WHITE, {
                localResult = PhoneCallController.reject()
            }), LinearLayout.LayoutParams(0, dp(58), 1f).apply {
                rightMargin = dp(8)
            })
            actions.addView(actionButton("Answer", COLOR_GREEN, COLOR_DARK_TEXT, {
                localResult = PhoneCallController.answer()
            }), LinearLayout.LayoutParams(0, dp(58), 1f).apply {
                leftMargin = dp(8)
            })
            root.addView(actions, LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(28)
            })
        } else {
            val controls = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            controls.addView(callOption("Mute", PhoneCallController.isMuted(), {
                PhoneCallController.setMuted(!PhoneCallController.isMuted())
                renderScreen(MODE_ACTIVE_CALL)
            }), LinearLayout.LayoutParams(0, dp(78), 1f).apply {
                rightMargin = dp(5)
            })
            controls.addView(callOption("Speaker", PhoneCallController.isSpeakerEnabled(), {
                PhoneCallController.setSpeakerEnabled(!PhoneCallController.isSpeakerEnabled())
                renderScreen(MODE_ACTIVE_CALL)
            }), LinearLayout.LayoutParams(0, dp(78), 1f).apply {
                leftMargin = dp(5)
                rightMargin = dp(5)
            })
            controls.addView(callOption(if (showCallKeypad) "Hide pad" else "Keypad", showCallKeypad, {
                showCallKeypad = !showCallKeypad
                renderScreen(MODE_ACTIVE_CALL)
            }), LinearLayout.LayoutParams(0, dp(78), 1f).apply {
                leftMargin = dp(5)
            })
            root.addView(controls, LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(12)
            })
            if (showCallKeypad) {
                addInCallKeypad()
            }
            root.addView(actionButton("End call", COLOR_RED, Color.WHITE, {
                localResult = PhoneCallController.hangup()
            }), LinearLayout.LayoutParams(-1, dp(56)).apply {
                bottomMargin = dp(16)
            })
        }
        updateCallScreen(
            getSharedPreferences(BridgeService.PREFS, MODE_PRIVATE)
                .getString(BridgeService.KEY_CALL_STATE, "IDLE") ?: "IDLE"
        )
    }

    private fun addInCallKeypad() {
        val rows = arrayOf(
            arrayOf("1", "2", "3"),
            arrayOf("4", "5", "6"),
            arrayOf("7", "8", "9"),
            arrayOf("*", "0", "#")
        )
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(COLOR_PANEL, 16)
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        rows.forEach { rowKeys ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            rowKeys.forEach { digit ->
                val key = actionButton(digit, COLOR_SELECTED, COLOR_TEXT) {
                    PhoneCallController.sendDtmf(digit[0])
                }
                row.addView(key, LinearLayout.LayoutParams(0, dp(37), 1f).apply {
                    leftMargin = dp(3)
                    rightMargin = dp(3)
                    bottomMargin = dp(4)
                })
            }
            panel.addView(row, LinearLayout.LayoutParams(-1, -2))
        }
        root.addView(panel, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(10)
        })
    }

    private fun callOption(title: String, active: Boolean, onClick: () -> Unit): LinearLayout {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(if (active) COLOR_SELECTED else COLOR_PANEL, 18)
            setPadding(dp(4), dp(8), dp(4), dp(8))
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
        val symbol = when (title) {
            "Mute" -> "M"
            "Speaker" -> "S"
            else -> "⌨"
        }
        item.addView(makeText(symbol, 18f, if (active) COLOR_GREEN else COLOR_TEXT, true, Gravity.CENTER))
        item.addView(makeText(title, 11f, COLOR_TEXT, active, Gravity.CENTER),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
        return item
    }

    private fun updateCallScreen(state: String) {
        val prefs = getSharedPreferences(BridgeService.PREFS, MODE_PRIVATE)
        val phoneNumber = prefs.getString(BridgeService.KEY_CALL_NUMBER, "") ?: ""
        activeNumberView?.text = phoneNumber.ifBlank { "Unknown caller" }
        activeStatusView?.text = when (state) {
            "RINGING" -> "Calling you"
            "DIALING" -> "Dialling…"
            "CONNECTING" -> "Connecting…"
            "OFFHOOK" -> "Connected"
            else -> state
        }
        val startedAt = prefs.getLong(BridgeService.KEY_CALL_STARTED_AT, 0L)
        activeTimerView?.text = if (startedAt > 0L && state == "OFFHOOK") {
            val seconds = ((System.currentTimeMillis() - startedAt) / 1000L).coerceAtLeast(0L)
            String.format(
                Locale.getDefault(),
                "%02d:%02d",
                seconds / 60L,
                seconds % 60L
            )
        } else {
            if (state == "RINGING") "Incoming cellular call" else " "
        }
    }

    private fun handleDialIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_DIAL) return
        val uri: Uri? = intent.data
        val dialNumber = uri?.schemeSpecificPart ?: ""
        pendingDialNumber = dialNumber
        selectedTab = TAB_KEYPAD
        if (::number.isInitialized) {
            number.setText(dialNumber)
            number.setSelection(number.text.length)
            pendingDialNumber = null
        }
    }

    private fun requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT < 23) {
            startBridgeService()
            return
        }
        val requested = arrayOf(
            android.Manifest.permission.CALL_PHONE,
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.RECORD_AUDIO
        )
        val missing = requested.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            startBridgeService()
        } else {
            requestPermissions(missing.toTypedArray(), 100)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            startBridgeService()
            val callAllowed =
                checkSelfPermission(android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
            val micAllowed =
                checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            localResult = when {
                !callAllowed -> "Grant Phone permission to place calls."
                !micAllowed -> "Call control is available; microphone permission is needed for Wi-Fi audio tests."
                else -> "Permissions ready."
            }
        }
    }

    private fun requestDefaultDialer() {
        if (Build.VERSION.SDK_INT < 23) return
        try {
            val telecom = getSystemService(TELECOM_SERVICE) as TelecomManager
            if (packageName == telecom.defaultDialerPackage) {
                localResult = "PhoneBridge is already the default phone app."
                Toast.makeText(this, localResult, Toast.LENGTH_SHORT).show()
                return
            }
            startActivity(
                Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                    putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
                }
            )
        } catch (e: Throwable) {
            localResult = "Could not open default-phone-app selection: " + e.javaClass.simpleName
            Toast.makeText(this, localResult, Toast.LENGTH_LONG).show()
        }
    }

    private fun isDefaultDialer(): Boolean {
        return try {
            packageName == (getSystemService(TELECOM_SERVICE) as TelecomManager).defaultDialerPackage
        } catch (_: Throwable) {
            false
        }
    }

    private fun startBridgeService() {
        startService(Intent(this, BridgeService::class.java))
    }

    private fun actionButton(
        label: String,
        backgroundColor: Int,
        textColor: Int,
        onClick: () -> Unit
    ): TextView {
        val view = makeText(label, 14f, textColor, true, Gravity.CENTER)
        view.background = rounded(backgroundColor, 18)
        view.setPadding(dp(12), dp(8), dp(12), dp(8))
        view.isClickable = true
        view.isFocusable = true
        view.setOnClickListener { onClick() }
        return view
    }

    private fun makeText(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false,
        gravityValue: Int = Gravity.START
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = gravityValue
            includeFontPadding = false
            if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
    }

    private fun rounded(color: Int, radiusDp: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp).toFloat()
            setColor(color)
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()

    override fun onDestroy() {
        handler.removeCallbacks(refresh)
        super.onDestroy()
    }

    companion object {
        const val ACTION_SHOW_CALL = "com.dsann.phonebridge.SHOW_CALL"
        private const val MODE_HOME = "HOME"
        private const val MODE_INCOMING = "INCOMING"
        private const val MODE_ACTIVE_CALL = "ACTIVE_CALL"
        private const val TAB_KEYPAD = "KEYPAD"
        private const val TAB_RECENTS = "RECENTS"
        private const val TAB_BRIDGE = "BRIDGE"

        private val COLOR_BG = Color.rgb(12, 15, 22)
        private val COLOR_PANEL = Color.rgb(25, 30, 40)
        private val COLOR_SELECTED = Color.rgb(37, 48, 65)
        private val COLOR_LINE = Color.rgb(43, 50, 64)
        private val COLOR_TEXT = Color.rgb(245, 247, 251)
        private val COLOR_MUTED = Color.rgb(150, 161, 179)
        private val COLOR_GREEN = Color.rgb(90, 226, 157)
        private val COLOR_RED = Color.rgb(235, 83, 99)
        private val COLOR_AMBER = Color.rgb(255, 190, 90)
        private val COLOR_DARK_TEXT = Color.rgb(10, 35, 25)
    }
}
