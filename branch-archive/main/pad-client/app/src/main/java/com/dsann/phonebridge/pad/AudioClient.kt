package com.dsann.phonebridge.pad

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.SystemClock
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.sqrt

object AudioClient {
    private const val SAMPLE_RATE = 8000
    private const val FRAME_SAMPLES = 160
    private const val FRAME_BYTES = FRAME_SAMPLES * 2
    private val executor = Executors.newFixedThreadPool(2)
    private val running = AtomicBoolean(false)
    private val txFrames = AtomicLong(0)
    private val rxFrames = AtomicLong(0)
    private val localPortValue = AtomicInteger(0)
    private val micRms = AtomicInteger(0)
    private val micPeak = AtomicInteger(0)
    private val speakerRms = AtomicInteger(0)
    private val speakerPeak = AtomicInteger(0)
    private val lastTxAt = AtomicLong(0)
    private val lastRxAt = AtomicLong(0)
    private val micState = AtomicReference("STOPPED")
    private val speakerState = AtomicReference("STOPPED")
    private var socket: DatagramSocket? = null

    fun isRunning(): Boolean = running.get()

    @Synchronized
    fun start(context: Context, phabAddress: InetAddress): Int {
        if (running.get()) return -1
        val localPort = findPort()
        val activeSocket = DatagramSocket(localPort).apply { reuseAddress = true }
        socket = activeSocket
        running.set(true)
        txFrames.set(0)
        rxFrames.set(0)
        localPortValue.set(localPort)
        micRms.set(0)
        micPeak.set(0)
        speakerRms.set(0)
        speakerPeak.set(0)
        lastTxAt.set(0)
        lastRxAt.set(0)
        micState.set("OPENING")
        speakerState.set("OPENING")
        updateStatus(context)

        executor.execute {
            var record: AudioRecord? = null
            try {
                val min = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
                )
                if (min <= 0) throw IllegalStateException("AUDIO_INPUT_UNAVAILABLE")
                record = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    maxOf(min, FRAME_BYTES * 4)
                )
                if (record.state != AudioRecord.STATE_INITIALIZED) throw IllegalStateException("AUDIO_INPUT_NOT_INITIALIZED")
                record.startRecording()
                if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) throw IllegalStateException("AUDIO_RECORDING_NOT_STARTED")
                micState.set("RECORDING_VOICE_COMMUNICATION")
                updateStatus(context)
                val pcm = ShortArray(FRAME_SAMPLES)
                while (running.get()) {
                    val n = record.read(pcm, 0, pcm.size)
                    if (n < 0) throw IllegalStateException("AUDIO_READ_" + n)
                    if (n > 0 && running.get()) {
                        micRms.set(rms(pcm, n))
                        micPeak.set(peak(pcm, n))
                        val bytes = ByteArray(n * 2)
                        var p = 0
                        for (i in 0 until n) {
                            val value = pcm[i].toInt()
                            bytes[p++] = (value and 0xff).toByte()
                            bytes[p++] = ((value ushr 8) and 0xff).toByte()
                        }
                        activeSocket.send(DatagramPacket(bytes, bytes.size, phabAddress, 45822))
                        txFrames.incrementAndGet()
                        lastTxAt.set(SystemClock.elapsedRealtime())
                        if (txFrames.get() % 25L == 0L) updateStatus(context)
                    }
                }
            } catch (e: Throwable) {
                micState.set("ERROR_" + shortError(e))
                updateStatus(context)
            } finally {
                try { record?.stop() } catch (_: Throwable) { }
                try { record?.release() } catch (_: Throwable) { }
            }
        }

        executor.execute {
            var track: AudioTrack? = null
            try {
                val min = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
                )
                if (min <= 0) throw IllegalStateException("AUDIO_OUTPUT_UNAVAILABLE")
                track = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    maxOf(min, FRAME_BYTES * 8),
                    AudioTrack.MODE_STREAM
                )
                if (track.state != AudioTrack.STATE_INITIALIZED) throw IllegalStateException("AUDIO_OUTPUT_NOT_INITIALIZED")
                track.setStereoVolume(1.0f, 1.0f)
                track.play()
                speakerState.set("PLAYING_STREAM_MUSIC")
                updateStatus(context)
                val packet = ByteArray(2048)
                val datagram = DatagramPacket(packet, packet.size)
                while (running.get()) {
                    datagram.length = packet.size
                    activeSocket.receive(datagram)
                    if (datagram.length <= 0 || !running.get()) continue
                    val samples = datagram.length / 2
                    val pcm = ShortArray(samples)
                    for (i in 0 until samples) {
                        val lo = packet[i * 2].toInt() and 0xff
                        val hi = packet[i * 2 + 1].toInt()
                        pcm[i] = ((hi shl 8) or lo).toShort()
                    }
                    speakerRms.set(rms(pcm, samples))
                    speakerPeak.set(peak(pcm, samples))
                    track.write(pcm, 0, pcm.size)
                    rxFrames.incrementAndGet()
                    lastRxAt.set(SystemClock.elapsedRealtime())
                    if (rxFrames.get() % 25L == 0L) updateStatus(context)
                }
            } catch (e: Throwable) {
                if (running.get()) {
                    speakerState.set("ERROR_" + shortError(e))
                    updateStatus(context)
                }
            } finally {
                try { track?.stop() } catch (_: Throwable) { }
                try { track?.release() } catch (_: Throwable) { }
            }
        }
        updateStatus(context)
        return localPort
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
            .replace(' ', '_').replace(';', '_').replace('|', '_').take(140)
    }

    private fun age(timestamp: Long): String {
        if (timestamp <= 0L) return "never"
        return (SystemClock.elapsedRealtime() - timestamp).coerceAtLeast(0L).toString()
    }

    private fun updateStatus(context: Context) {
        context.getSharedPreferences("audio", Context.MODE_PRIVATE)
            .edit().putString("status", statusLine()).apply()
    }

    private fun statusLine(): String {
        return (if (running.get()) "RUNNING:" else "STOPPED:") + localPortValue.get() +
            " TX:" + txFrames.get() + " RX:" + rxFrames.get() +
            " MIC_RMS:" + micRms.get() + " MIC_PEAK:" + micPeak.get() +
            " RX_RMS:" + speakerRms.get() + " RX_PEAK:" + speakerPeak.get()
    }

    fun diagnosticSummary(context: Context): String {
        return "running=" + (if (running.get()) "1" else "0") +
            ";localPort=" + localPortValue.get() +
            ";micState=" + micState.get() +
            ";txPackets=" + txFrames.get() +
            ";micRms=" + micRms.get() +
            ";micPeak=" + micPeak.get() +
            ";lastTxMsAgo=" + age(lastTxAt.get()) +
            ";speakerState=" + speakerState.get() +
            ";rxPackets=" + rxFrames.get() +
            ";rxRms=" + speakerRms.get() +
            ";rxPeak=" + speakerPeak.get() +
            ";lastRxMsAgo=" + age(lastRxAt.get()) +
            ";status=" + statusLine()
    }

    private fun findPort(): Int {
        var port = 46000
        while (port < 46100) {
            try { DatagramSocket(port).use { return port } } catch (_: Exception) { port++ }
        }
        throw IllegalStateException("NO_UDP_PORT")
    }

    @Synchronized
    fun stop(context: Context) {
        running.set(false)
        try { socket?.close() } catch (_: Throwable) { }
        socket = null
        micState.set("STOPPED")
        speakerState.set("STOPPED")
        updateStatus(context)
    }

    fun status(context: Context): String =
        context.getSharedPreferences("audio", Context.MODE_PRIVATE)
            .getString("status", "STOPPED") ?: "STOPPED"
}
