package com.dsann.phonebridge

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
            if (mode != renderedMode) renderScreen(mode)
            if (mode == MODE_ACTIVE_CALL || mode == MODE_INCOMING) updateCallScreen(state)
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
        body.addView(makeText("Bridge status", 20f, COLOR_TEXT, true),
            LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        val prefs = getSharedPreferences(BridgeService.PREFS, MODE_PRIVATE)
        val bridge = prefs.getString(BridgeService.KEY_STATUS, "Starting…") ?: "Starting…"
        val call = prefs.getString(BridgeService.KEY_CALL_STATE, "IDLE") ?: "IDLE"
        val audio = prefs.getString("audio_wifi", "STOPPED") ?: "STOPPED"
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(COLOR_PANEL, 18)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        card.addView(makeText("BRIDGE", 10f, COLOR_MUTED, true))
        card.addView(makeText(bridge, 15f, COLOR_TEXT, true),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(5) })
        card.addView(makeText("Call: " + call + "    Wi-Fi audio: " + audio, 12f, COLOR_MUTED),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        body.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })

        body.addView(actionButton(
            if (isDefaultDialer()) "PhoneBridge is the default phone app" else "Set as default phone app",
            COLOR_SELECTED, COLOR_TEXT
        ) { requestDefaultDialer() }, LinearLayout.LayoutParams(-1, dp(50)).apply {
            bottomMargin = dp(8)
        })
        body.addView(actionButton("Start bridge", COLOR_PANEL, COLOR_TEXT, {
            startBridgeService()
            localResult = "Bridge start requested."
            renderScreen(MODE_HOME)
        }), LinearLayout.LayoutParams(-1, dp(48)).apply { bottomMargin = dp(8) })
        body.addView(actionButton("Stop bridge", COLOR_PANEL, COLOR_TEXT, {
            startService(Intent(this, BridgeService::class.java).apply {
                action = BridgeService.ACTION_STOP
            })
            localResult = "Bridge stop requested."
            renderScreen(MODE_HOME)
        }), LinearLayout.LayoutParams(-1, dp(48)).apply { bottomMargin = dp(8) })

        if (localResult.isNotBlank()) {
            body.addView(makeText(localResult, 12f, COLOR_MUTED),
                LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        }
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
