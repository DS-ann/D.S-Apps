package com.dsann.phonebridge

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.SystemClock
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.sqrt

/** Wi-Fi PCM transport. Media playback is not cellular call-uplink injection. */
object AudioBridge {
    private const val SAMPLE_RATE = 8000
    private const val FRAME_SAMPLES = 160
    private const val FRAME_BYTES = FRAME_SAMPLES * 2
    private const val AUDIO_PORT = 45822

    private val running = AtomicBoolean(false)
    private val executor = Executors.newFixedThreadPool(2)
    private var socket: DatagramSocket? = null

    private val phoneToPadPackets = AtomicLong(0)
    private val padToPhonePackets = AtomicLong(0)
    private val downlinkRms = AtomicInteger(0)
    private val downlinkPeak = AtomicInteger(0)
    private val padMicRms = AtomicInteger(0)
    private val padMicPeak = AtomicInteger(0)
    private val lastPhoneTxAt = AtomicLong(0)
    private val lastPadRxAt = AtomicLong(0)
    private val captureSource = AtomicReference("STOPPED")
    private val captureState = AtomicReference("STOPPED")
    private val speakerState = AtomicReference("STOPPED")

    @Synchronized
    fun start(context: Context, address: InetAddress, remotePort: Int, localPort: Int = AUDIO_PORT): Boolean {
        if (!running.compareAndSet(false, true)) return false
        val activeSocket = try {
            DatagramSocket(localPort).apply { reuseAddress = true }
        } catch (e: Throwable) {
            running.set(false)
            captureState.set("ERROR_UDP_" + shortError(e))
            publishStatus(context)
            return false
        }
        socket = activeSocket

        phoneToPadPackets.set(0)
        padToPhonePackets.set(0)
        downlinkRms.set(0)
        downlinkPeak.set(0)
        padMicRms.set(0)
        padMicPeak.set(0)
        lastPhoneTxAt.set(0)
        lastPadRxAt.set(0)
        captureSource.set("WAITING_FOR_CALL")
        captureState.set("WAITING_FOR_CALL")
        speakerState.set("IDLE_ONLY_STREAM_MUSIC")
        publishStatus(context)

        // Phone -> tablet: request call DOWNLINK only while the cellular call is active.
        // This prevents re-transmitting the Phab's physical microphone to the tablet.
        executor.execute {
            var record: AudioRecord? = null
            try {
                while (running.get()) {
                    if (!isCellularCallActive()) {
                        if (record != null) {
                            try { record?.stop() } catch (_: Throwable) { }
                            try { record?.release() } catch (_: Throwable) { }
                            record = null
                        }
                        captureSource.set("DISABLED_OUTSIDE_CALL")
                        captureState.set("WAITING_FOR_ACTIVE_CALL")
                        publishStatus(context)
                        try { Thread.sleep(150L) } catch (_: InterruptedException) { }
                        continue
                    }

                    if (record == null) {
                        captureSource.set("VOICE_DOWNLINK")
                        captureState.set("OPENING_VOICE_DOWNLINK")
                        publishStatus(context)
                        try {
                            record = openRecord(MediaRecorder.AudioSource.VOICE_DOWNLINK)
                            captureState.set("RECORDING_VOICE_DOWNLINK")
                            publishStatus(context)
                        } catch (e: Throwable) {
                            captureState.set("ERROR_DOWNLINK_CAPTURE_" + shortError(e))
                            publishStatus(context)
                            try { Thread.sleep(1000L) } catch (_: InterruptedException) { }
                            continue
                        }
                    }

                    val currentRecord = record ?: continue
                    val pcm = ShortArray(FRAME_SAMPLES)
                    val n = currentRecord.read(pcm, 0, pcm.size)
                    if (n < 0) {
                        captureState.set("ERROR_AUDIO_READ_" + n)
                        publishStatus(context)
                        try { currentRecord.release() } catch (_: Throwable) { }
                        record = null
                        continue
                    }
                    if (n > 0 && running.get() && isCellularCallActive()) {
                        downlinkRms.set(rms(pcm, n))
                        downlinkPeak.set(peak(pcm, n))
                        val bytes = ByteArray(n * 2)
                        var p = 0
                        for (i in 0 until n) {
                            val value = pcm[i].toInt()
                            bytes[p++] = (value and 0xff).toByte()
                            bytes[p++] = ((value ushr 8) and 0xff).toByte()
                        }
                        activeSocket.send(DatagramPacket(bytes, bytes.size, address, remotePort))
                        phoneToPadPackets.incrementAndGet()
                        lastPhoneTxAt.set(SystemClock.elapsedRealtime())
                        if (phoneToPadPackets.get() % 25L == 0L) publishStatus(context)
                    }
                }
            } catch (e: Throwable) {
                if (running.get()) {
                    captureState.set("ERROR_" + shortError(e))
                    publishStatus(context)
                }
            } finally {
                try { record?.stop() } catch (_: Throwable) { }
                try { record?.release() } catch (_: Throwable) { }
            }
        }

        // Tablet -> phone: always receive and measure packets. Play to the speaker
        // only outside calls; inside a call discard here, because STREAM_MUSIC is
        // not the SIM transmit/uplink path and caused the misleading echo behaviour.
        executor.execute {
            var track: AudioTrack? = null
            val packet = ByteArray(2048)
            val datagram = DatagramPacket(packet, packet.size)
            try {
                while (running.get()) {
                    datagram.length = packet.size
                    activeSocket.receive(datagram)
                    val length = datagram.length
                    if (length <= 0 || !running.get()) continue
                    val samples = length / 2
                    val pcm = ShortArray(samples)
                    for (i in 0 until samples) {
                        val lo = packet[i * 2].toInt() and 0xff
                        val hi = packet[i * 2 + 1].toInt()
                        pcm[i] = ((hi shl 8) or lo).toShort()
                    }
                    padMicRms.set(rms(pcm, samples))
                    padMicPeak.set(peak(pcm, samples))
                    padToPhonePackets.incrementAndGet()
                    lastPadRxAt.set(SystemClock.elapsedRealtime())

                    if (isCellularCallActive()) {
                        if (track != null) {
                            try { track?.pause() } catch (_: Throwable) { }
                            try { track?.flush() } catch (_: Throwable) { }
                            try { track?.stop() } catch (_: Throwable) { }
                            try { track?.release() } catch (_: Throwable) { }
                            track = null
                        }
                        speakerState.set("DISABLED_DURING_CALL_NOT_CELLULAR_UPLINK")
                    } else {
                        if (track == null) {
                            val min = AudioTrack.getMinBufferSize(
                                SAMPLE_RATE,
                                AudioFormat.CHANNEL_OUT_MONO,
                                AudioFormat.ENCODING_PCM_16BIT
                            )
                            require(min > 0) { "AUDIO_OUTPUT_UNAVAILABLE" }
                            val created = AudioTrack(
                                AudioManager.STREAM_MUSIC,
                                SAMPLE_RATE,
                                AudioFormat.CHANNEL_OUT_MONO,
                                AudioFormat.ENCODING_PCM_16BIT,
                                maxOf(min, FRAME_BYTES * 8),
                                AudioTrack.MODE_STREAM
                            )
                            check(created.state == AudioTrack.STATE_INITIALIZED) { "AUDIO_OUTPUT_NOT_INITIALIZED" }
                            created.play()
                            track = created
                            speakerState.set("PLAYING_STREAM_MUSIC_IDLE_ONLY")
                        }
                        track?.write(pcm, 0, pcm.size)
                    }
                    if (padToPhonePackets.get() % 25L == 0L) publishStatus(context)
                }
            } catch (e: SocketException) {
                if (running.get()) {
                    speakerState.set("ERROR_SOCKET_" + shortError(e))
                    publishStatus(context)
                }
            } catch (e: Throwable) {
                if (running.get()) {
                    speakerState.set("ERROR_" + shortError(e))
                    publishStatus(context)
                }
            } finally {
                try { track?.stop() } catch (_: Throwable) { }
                try { track?.release() } catch (_: Throwable) { }
            }
        }
        publishStatus(context)
        return true
    }

    private fun openRecord(source: Int): AudioRecord {
        val min = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        require(min > 0) { "AUDIO_INPUT_UNAVAILABLE" }
        val record = AudioRecord(
            source,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(min, FRAME_BYTES * 4)
        )
        try {
            check(record.state == AudioRecord.STATE_INITIALIZED) { "AUDIO_RECORD_NOT_INITIALIZED_SOURCE_" + source }
            record.startRecording()
            check(record.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "AUDIO_RECORDING_NOT_STARTED_SOURCE_" + source }
            return record
        } catch (e: Throwable) {
            try { record.release() } catch (_: Throwable) { }
            throw e
        }
    }

    private fun isCellularCallActive(): Boolean {
        val state = PhoneCallController.currentState
        return state == "OFFHOOK" || state == "ACTIVE" || state == "HOLDING"
    }

    private fun rms(samples: ShortArray, count: Int): Int {
        if (count <= 0) return 0
        var sum = 0.0
        for (i in 0 until count) {
            val v = samples[i].toDouble()
            sum += v * v
        }
        return sqrt(sum / count).toInt().coerceIn(0, 32767)
    }

    private fun peak(samples: ShortArray, count: Int): Int {
        var value = 0
        for (i in 0 until count) {
            val v = kotlin.math.abs(samples[i].toInt())
            if (v > value) value = v
        }
        return value.coerceIn(0, 32767)
    }

    private fun shortError(error: Throwable): String {
        return (error.javaClass.simpleName + "_" + (error.message ?: "NO_MESSAGE"))
            .replace(' ', '_').replace(';', '_').replace('|', '_').take(150)
    }

    private fun age(timestamp: Long): String {
        if (timestamp <= 0L) return "never"
        return (SystemClock.elapsedRealtime() - timestamp).coerceAtLeast(0L).toString()
    }

    fun diagnosticSummary(): String {
        return "running=" + (if (running.get()) "1" else "0") +
            ";call=" + (if (isCellularCallActive()) "1" else "0") +
            ";capture=" + captureSource.get() +
            ";captureState=" + captureState.get() +
            ";phoneToPadPackets=" + phoneToPadPackets.get() +
            ";downlinkRms=" + downlinkRms.get() +
            ";downlinkPeak=" + downlinkPeak.get() +
            ";lastPhoneTxMsAgo=" + age(lastPhoneTxAt.get()) +
            ";padToPhonePackets=" + padToPhonePackets.get() +
            ";padMicRms=" + padMicRms.get() +
            ";padMicPeak=" + padMicPeak.get() +
            ";lastPadRxMsAgo=" + age(lastPadRxAt.get()) +
            ";speaker=" + speakerState.get()
    }

    private fun publishStatus(context: Context) {
        val summary = diagnosticSummary()
        context.getSharedPreferences(BridgeService.PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString("audio_wifi", summary)
            .putString("audio_phone_diag", summary)
            .apply()
    }

    @Synchronized
    fun stop(context: Context) {
        running.set(false)
        try { socket?.close() } catch (_: Throwable) { }
        socket = null
        captureState.set("STOPPED")
        speakerState.set("STOPPED")
        captureSource.set("STOPPED")
        publishStatus(context)
    }

    fun isRunning(): Boolean = running.get()
}
