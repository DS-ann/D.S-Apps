package com.dsann.phonebridge

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.TelecomManager
import android.telecom.VideoProfile

class BridgeService : Service() {
    private var server: BridgeServer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            server?.stop()
            server = null
            stopSelf()
            return START_NOT_STICKY
        }
        ensureServer()
        return START_STICKY
    }

    private fun ensureServer() {
        if (server == null) server = BridgeServer(this).also { it.start() }
    }

    override fun onDestroy() {
        server?.stop()
        server = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "com.dsann.phonebridge.STOP"
        const val PREFS = "bridge"
        const val KEY_STATUS = "status"
        const val KEY_CALL_STATE = "call_state"
    }
}

/**
 * Android Telecom supplies active Call objects only when it binds this
 * InCallService, normally after PhoneBridge is selected as the default phone app.
 */
class PhoneInCallService : InCallService() {
    private val callbacks = mutableMapOf<Call, Call.Callback>()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        PhoneCallController.attachInCallService(this)
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        val callback = object : Call.Callback() {
            override fun onStateChanged(changedCall: Call, state: Int) {
                PhoneCallController.onCallStateChanged(changedCall, state)
            }
        }
        callbacks[call] = callback
        call.registerCallback(callback, mainHandler)
        PhoneCallController.onCallAdded(call)

        // Keep a minimal in-call screen available on the phone.
        try {
            startActivity(
                Intent(this, MainActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            )
        } catch (_: Exception) { }
    }

    override fun onCallRemoved(call: Call) {
        callbacks.remove(call)?.let {
            try { call.unregisterCallback(it) } catch (_: Exception) { }
        }
        PhoneCallController.onCallRemoved(call)
        super.onCallRemoved(call)
    }

    override fun onDestroy() {
        callbacks.forEach { (call, callback) ->
            try { call.unregisterCallback(callback) } catch (_: Exception) { }
        }
        callbacks.clear()
        PhoneCallController.detachInCallService(this)
        super.onDestroy()
    }
}

/** Shared call controller used by the TCP server and Android Telecom. */
object PhoneCallController {
    @Volatile private var activeCall: Call? = null
    @Volatile private var inCallService: PhoneInCallService? = null
    @Volatile private var server: BridgeServer? = null
    @Volatile private var appContext: Context? = null

    @Volatile var currentState: String = "IDLE"
        private set

    fun attachServer(instance: BridgeServer, context: Context) {
        server = instance
        appContext = context.applicationContext
        appContext?.getSharedPreferences(BridgeService.PREFS, Context.MODE_PRIVATE)
            ?.edit()?.putString(BridgeService.KEY_CALL_STATE, currentState)?.apply()
    }

    fun detachServer(instance: BridgeServer) {
        if (server === instance) server = null
    }

    fun attachInCallService(instance: PhoneInCallService) {
        inCallService = instance
    }

    fun detachInCallService(instance: PhoneInCallService) {
        if (inCallService === instance) {
            inCallService = null
            activeCall = null
            publishState("IDLE")
        }
    }

    fun onCallAdded(call: Call) {
        activeCall = call
        publishState(labelFor(call.state))
    }

    fun onCallRemoved(call: Call) {
        if (activeCall === call) {
            activeCall = null
            publishState("IDLE")
        }
    }

    fun onCallStateChanged(call: Call, state: Int) {
        if (activeCall === call) publishState(labelFor(state))
    }

    fun dial(context: Context, rawNumber: String): String {
        val number = rawNumber.trim()
        if (number.isEmpty() || !number.matches(Regex("[+0-9*#(),; .-]+"))) {
            return "ERROR:INVALID_NUMBER"
        }
        if (Build.VERSION.SDK_INT >= 23 &&
            context.checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED
        ) {
            return "ERROR:CALL_PHONE_PERMISSION"
        }

        return try {
            val telecom = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            val uri = Uri.fromParts("tel", number, null)
            if (context.packageName == telecom.defaultDialerPackage) {
                telecom.placeCall(uri, Bundle())
            } else {
                context.startActivity(
                    Intent(Intent.ACTION_CALL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            "OK:DIAL"
        } catch (e: SecurityException) {
            "ERROR:CALL_PERMISSION:" + (e.message ?: "permission denied")
        } catch (e: Throwable) {
            "ERROR:DIAL:" + e.javaClass.simpleName + ":" + (e.message ?: "call failed")
        }
    }

    fun answer(): String {
        val call = activeCall ?: return "ERROR:NO_ACTIVE_CALL:SET_DEFAULT_DIALER"
        return try {
            call.answer(VideoProfile.STATE_AUDIO_ONLY)
            "OK:ANSWER"
        } catch (e: Throwable) {
            "ERROR:ANSWER:" + e.javaClass.simpleName + ":" + (e.message ?: "answer failed")
        }
    }

    fun reject(): String {
        val call = activeCall ?: return "ERROR:NO_ACTIVE_CALL:SET_DEFAULT_DIALER"
        return try {
            call.reject(false, null)
            "OK:REJECT"
        } catch (e: Throwable) {
            "ERROR:REJECT:" + e.javaClass.simpleName + ":" + (e.message ?: "reject failed")
        }
    }

    fun hangup(): String {
        val call = activeCall ?: return "ERROR:NO_ACTIVE_CALL:SET_DEFAULT_DIALER"
        return try {
            call.disconnect()
            "OK:HANGUP"
        } catch (e: Throwable) {
            "ERROR:HANGUP:" + e.javaClass.simpleName + ":" + (e.message ?: "hangup failed")
        }
    }

    private fun publishState(state: String) {
        currentState = state
        appContext?.getSharedPreferences(BridgeService.PREFS, Context.MODE_PRIVATE)
            ?.edit()?.putString(BridgeService.KEY_CALL_STATE, state)?.apply()
        server?.broadcastCallState(state)
    }

    private fun labelFor(state: Int): String {
        return when (state) {
            Call.STATE_RINGING -> "RINGING"
            Call.STATE_DIALING -> "DIALING"
            Call.STATE_CONNECTING -> "CONNECTING"
            Call.STATE_ACTIVE, Call.STATE_HOLDING -> "OFFHOOK"
            Call.STATE_DISCONNECTED -> "IDLE"
            else -> "IDLE"
        }
    }
}
