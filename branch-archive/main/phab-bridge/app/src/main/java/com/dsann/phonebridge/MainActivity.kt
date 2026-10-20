package com.dsann.phonebridge

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.TelecomManager
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var number: EditText
    private val handler = Handler(Looper.getMainLooper())
    private var localResult = ""

    private val refresh = object : Runnable {
        override fun run() {
            val prefs = getSharedPreferences(BridgeService.PREFS, MODE_PRIVATE)
            val bridge = prefs.getString(BridgeService.KEY_STATUS, "Starting…") ?: "Starting…"
            val call = prefs.getString(BridgeService.KEY_CALL_STATE, "IDLE") ?: "IDLE"
            val audio = prefs.getString("audio_wifi", "STOPPED") ?: "STOPPED"
            val defaultDialer = try {
                val telecom = getSystemService(TELECOM_SERVICE) as TelecomManager
                packageName == telecom.defaultDialerPackage
            } catch (_: Throwable) { false }
            status.text = "PhoneBridge\n\nBridge: " + bridge +
                "\nCall state: " + call +
                "\nDefault phone app: " + if (defaultDialer) "Yes" else "No" +
                "\nWi-Fi audio: " + audio +
                if (localResult.isNotEmpty()) "\n\n" + localResult else ""
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        handleDialIntent(intent)
        requestPermissionsIfNeeded()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDialIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(refresh)
        handler.post(refresh)
    }

    override fun onPause() {
        handler.removeCallbacks(refresh)
        super.onPause()
    }

    private fun buildUi() {
        status = TextView(this).apply {
            text = "PhoneBridge\nStarting…"
            textSize = 17f
            gravity = Gravity.CENTER
            setPadding(12, 20, 12, 20)
        }
        number = EditText(this).apply {
            hint = "Phone number"
            inputType = InputType.TYPE_CLASS_PHONE
            setSingleLine(true)
            textSize = 20f
            gravity = Gravity.CENTER
        }

        val dial = Button(this).apply {
            text = "Dial number"
            setOnClickListener {
                localResult = PhoneCallController.dial(this@MainActivity, number.text.toString())
            }
        }
        val defaultDialer = Button(this).apply {
            text = "Enable call controls"
            setOnClickListener { requestDefaultDialer() }
        }
        val answer = Button(this).apply {
            text = "Answer"
            setOnClickListener { localResult = PhoneCallController.answer() }
        }
        val reject = Button(this).apply {
            text = "Reject"
            setOnClickListener { localResult = PhoneCallController.reject() }
        }
        val hangup = Button(this).apply {
            text = "End call"
            setOnClickListener { localResult = PhoneCallController.hangup() }
        }
        val start = Button(this).apply {
            text = "Start bridge"
            setOnClickListener { startBridgeService() }
        }
        val stop = Button(this).apply {
            text = "Stop bridge"
            setOnClickListener {
                startService(Intent(this@MainActivity, BridgeService::class.java).apply {
                    action = BridgeService.ACTION_STOP
                })
            }
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 24, 24, 24)
            addView(status, LinearLayout.LayoutParams(-1, -2))
            addView(number, LinearLayout.LayoutParams(-1, -2))
            addView(dial, LinearLayout.LayoutParams(-1, -2))
            addView(defaultDialer, LinearLayout.LayoutParams(-1, -2))
            addView(answer, LinearLayout.LayoutParams(-1, -2))
            addView(reject, LinearLayout.LayoutParams(-1, -2))
            addView(hangup, LinearLayout.LayoutParams(-1, -2))
            addView(start, LinearLayout.LayoutParams(-1, -2))
            addView(stop, LinearLayout.LayoutParams(-1, -2))
        }
        setContentView(ScrollView(this).apply { addView(content) })
    }

    private fun handleDialIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_DIAL) return
        val uri: Uri? = intent.data
        if (uri != null) number.setText(uri.schemeSpecificPart ?: "")
    }

    private fun requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT < 23) {
            startBridgeService()
            return
        }
        val requested = arrayOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECORD_AUDIO
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
                checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
            val micAllowed =
                checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
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
                return
            }
            startActivity(
                Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                    putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
                }
            )
        } catch (e: Throwable) {
            localResult = "Could not open default-phone-app selection: " + e.javaClass.simpleName
        }
    }

    private fun startBridgeService() {
        startService(Intent(this, BridgeService::class.java))
    }

    override fun onDestroy() {
        handler.removeCallbacks(refresh)
        super.onDestroy()
    }
}
