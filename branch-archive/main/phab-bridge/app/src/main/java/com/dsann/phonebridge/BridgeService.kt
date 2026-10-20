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
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import org.json.JSONArray
import org.json.JSONObject

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
        const val KEY_CALL_NUMBER = "call_number"
        const val KEY_CALL_DIRECTION = "call_direction"
        const val KEY_CALL_STARTED_AT = "call_started_at"
        const val KEY_RECENTS = "recent_calls"
        const val KEY_CALL_MUTED = "call_muted"
        const val KEY_CALL_SPEAKER = "call_speaker"
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

            override fun onDetailsChanged(changedCall: Call, details: Call.Details) {
                PhoneCallController.onCallDetailsChanged(changedCall, details)
            }
        }
        callbacks[call] = callback
        call.registerCallback(callback, mainHandler)
        PhoneCallController.onCallAdded(call)

        // Open the app's own call screen. Telecom still owns the call lifecycle.
        try {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    action = MainActivity.ACTION_SHOW_CALL
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
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
    @Volatile private var currentNumber = ""
    @Volatile private var currentDirection = "OUTGOING"
    @Volatile private var callStartedAt = 0L
    @Volatile private var previousNonSpeakerRoute = CallAudioState.ROUTE_EARPIECE

    @Volatile var currentState: String = "IDLE"
        private set

    fun attachServer(instance: BridgeServer, context: Context) {
        server = instance
        appContext = context.applicationContext
        val prefs = appContext?.getSharedPreferences(BridgeService.PREFS, Context.MODE_PRIVATE)
        prefs?.edit()?.putString(BridgeService.KEY_CALL_STATE, currentState)?.apply()
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
            currentNumber = ""
            callStartedAt = 0L
            saveCallMetadata()
            publishState("IDLE")
        }
    }

    fun onCallAdded(call: Call) {
        activeCall = call
        val callNumber = try { call.details?.handle?.schemeSpecificPart ?: "" } catch (_: Throwable) { "" }
        if (callNumber.isNotBlank()) currentNumber = callNumber
        currentDirection = if (call.state == Call.STATE_RINGING) "INCOMING" else "OUTGOING"
        callStartedAt = if (call.state == Call.STATE_ACTIVE || call.state == Call.STATE_HOLDING) {
            System.currentTimeMillis()
        } else {
            0L
        }
        saveCallMetadata()
        publishState(labelFor(call.state))
    }

    fun onCallDetailsChanged(call: Call, details: Call.Details) {
        if (activeCall !== call) return
        val callNumber = try { details.handle?.schemeSpecificPart ?: "" } catch (_: Throwable) { "" }
        if (callNumber.isNotBlank() && callNumber != currentNumber) {
            currentNumber = callNumber
            saveCallMetadata()
        }
    }

    fun onCallRemoved(call: Call) {
        if (activeCall === call) {
            recordRecentCall()
            activeCall = null
            currentNumber = ""
            currentDirection = "OUTGOING"
            callStartedAt = 0L
            saveCallMetadata()
            publishState("IDLE")
        }
    }

    fun onCallStateChanged(call: Call, state: Int) {
        if (activeCall !== call) return
        if ((state == Call.STATE_ACTIVE || state == Call.STATE_HOLDING) && callStartedAt == 0L) {
            callStartedAt = System.currentTimeMillis()
            saveCallMetadata()
        }
        publishState(labelFor(state))
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
            appContext = context.applicationContext
            val telecom = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            val uri = Uri.fromParts("tel", number, null)
            currentNumber = number
            currentDirection = "OUTGOING"
            callStartedAt = 0L
            saveCallMetadata()
            if (context.packageName == telecom.defaultDialerPackage) {
                publishState("DIALING")
                telecom.placeCall(uri, Bundle())
            } else {
                context.startActivity(
                    Intent(Intent.ACTION_CALL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            "OK:DIAL"
        } catch (e: SecurityException) {
            publishState("IDLE")
            "ERROR:CALL_PERMISSION:" + (e.message ?: "permission denied")
        } catch (e: Throwable) {
            publishState("IDLE")
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

    fun isMuted(): Boolean {
        return appContext?.getSharedPreferences(BridgeService.PREFS, Context.MODE_PRIVATE)
            ?.getBoolean(BridgeService.KEY_CALL_MUTED, false) ?: false
    }

    fun setMuted(muted: Boolean): String {
        val service = inCallService ?: return "ERROR:NO_IN_CALL_SERVICE"
        return try {
            service.setMuted(muted)
            appContext?.getSharedPreferences(BridgeService.PREFS, Context.MODE_PRIVATE)
                ?.edit()?.putBoolean(BridgeService.KEY_CALL_MUTED, muted)?.apply()
            "OK:MUTE:" + muted
        } catch (e: Throwable) {
            "ERROR:MUTE:" + e.javaClass.simpleName
        }
    }

    fun isSpeakerEnabled(): Boolean {
        val route = try { inCallService?.callAudioState?.route } catch (_: Throwable) { null }
        if (route != null) return route == CallAudioState.ROUTE_SPEAKER
        return appContext?.getSharedPreferences(BridgeService.PREFS, Context.MODE_PRIVATE)
            ?.getBoolean(BridgeService.KEY_CALL_SPEAKER, false) ?: false
    }

    fun setSpeakerEnabled(enabled: Boolean): String {
        val service = inCallService ?: return "ERROR:NO_IN_CALL_SERVICE"
        return try {
            val audioState = service.callAudioState ?: return "ERROR:CALL_AUDIO_UNAVAILABLE"
            val supported = audioState.supportedRouteMask
            val targetRoute: Int
            if (enabled) {
                if (audioState.route != CallAudioState.ROUTE_SPEAKER) {
                    previousNonSpeakerRoute = audioState.route
                }
                targetRoute = CallAudioState.ROUTE_SPEAKER
            } else {
                targetRoute = if ((supported and previousNonSpeakerRoute) != 0) {
                    previousNonSpeakerRoute
                } else if ((supported and CallAudioState.ROUTE_EARPIECE) != 0) {
                    CallAudioState.ROUTE_EARPIECE
                } else if ((supported and CallAudioState.ROUTE_BLUETOOTH) != 0) {
                    CallAudioState.ROUTE_BLUETOOTH
                } else if ((supported and CallAudioState.ROUTE_WIRED_HEADSET) != 0) {
                    CallAudioState.ROUTE_WIRED_HEADSET
                } else {
                    return "ERROR:NO_SUPPORTED_AUDIO_ROUTE"
                }
            }
            if ((supported and targetRoute) == 0) return "ERROR:UNSUPPORTED_AUDIO_ROUTE"
            service.setAudioRoute(targetRoute)
            appContext?.getSharedPreferences(BridgeService.PREFS, Context.MODE_PRIVATE)
                ?.edit()?.putBoolean(BridgeService.KEY_CALL_SPEAKER, enabled)?.apply()
            "OK:SPEAKER:" + enabled
        } catch (e: Throwable) {
            "ERROR:SPEAKER:" + e.javaClass.simpleName
        }
    }

    fun sendDtmf(digit: Char): String {
        if (!(digit in '0'..'9' || digit == '*' || digit == '#')) return "ERROR:INVALID_DTMF"
        val call = activeCall ?: return "ERROR:NO_ACTIVE_CALL"
        return try {
            call.playDtmfTone(digit)
            Handler(Looper.getMainLooper()).postDelayed({
                try { call.stopDtmfTone() } catch (_: Throwable) { }
            }, 160L)
            "OK:DTMF"
        } catch (e: Throwable) {
            "ERROR:DTMF:" + e.javaClass.simpleName
        }
    }

    private fun recordRecentCall() {
        val context = appContext ?: return
        val number = currentNumber
        if (number.isBlank()) return
        val endedAt = System.currentTimeMillis()
        val duration = if (callStartedAt > 0L) {
            ((endedAt - callStartedAt) / 1000L).coerceAtLeast(0L)
        } else {
            0L
        }
        val direction = if (currentDirection == "INCOMING" && callStartedAt == 0L) {
            "MISSED"
        } else {
            currentDirection
        }
        val prefs = context.getSharedPreferences(BridgeService.PREFS, Context.MODE_PRIVATE)
        val old = try {
            JSONArray(prefs.getString(BridgeService.KEY_RECENTS, "[]") ?: "[]")
        } catch (_: Throwable) {
            JSONArray()
        }
        val updated = JSONArray()
        updated.put(
            JSONObject()
                .put("number", number)
                .put("direction", direction)
                .put("timestamp", endedAt)
                .put("duration", duration)
        )
        for (i in 0 until old.length().coerceAtMost(39)) {
            val item = old.optJSONObject(i) ?: continue
            updated.put(item)
        }
        prefs.edit().putString(BridgeService.KEY_RECENTS, updated.toString()).apply()
    }

    private fun saveCallMetadata() {
        val prefs = appContext?.getSharedPreferences(BridgeService.PREFS, Context.MODE_PRIVATE) ?: return
        prefs.edit()
            .putString(BridgeService.KEY_CALL_NUMBER, currentNumber)
            .putString(BridgeService.KEY_CALL_DIRECTION, currentDirection)
            .putLong(BridgeService.KEY_CALL_STARTED_AT, callStartedAt)
            .apply()
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
