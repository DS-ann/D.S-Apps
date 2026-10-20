package com.dsann.phonebridge

import android.content.Context
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections
import java.util.concurrent.Executors

/** TCP control channel for tablet commands and phone call-state updates. */
class BridgeServer(private val context: Context) {
    @Volatile private var serverSocket: ServerSocket? = null
    private val executor = Executors.newCachedThreadPool()
    private val clients = Collections.synchronizedSet(mutableSetOf<Socket>())
    private val writers = Collections.synchronizedMap(mutableMapOf<Socket, BufferedWriter>())

    fun start() {
        PhoneCallController.attachServer(this, context)
        executor.execute {
            try {
                val server = ServerSocket(BridgeProtocol.PORT)
                serverSocket = server
                updateStatus("Listening on " + BridgeProtocol.PORT)
                while (!server.isClosed) {
                    handleClient(server.accept())
                }
            } catch (e: Exception) {
                if (serverSocket?.isClosed != true) {
                    updateStatus("Server error: " + (e.message ?: e.javaClass.simpleName))
                }
            }
        }
    }

    fun stop() {
        AudioBridge.stop(context)
        PhoneCallController.detachServer(this)
        try { serverSocket?.close() } catch (_: Exception) { }
        synchronized(clients) {
            clients.forEach { try { it.close() } catch (_: Exception) { } }
            clients.clear()
        }
        synchronized(writers) { writers.clear() }
        executor.shutdownNow()
        updateStatus("Stopped")
    }

    private fun handleClient(socket: Socket) {
        clients.add(socket)
        executor.execute {
            socket.use { client ->
                try {
                    val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
                    val writer = BufferedWriter(OutputStreamWriter(client.getOutputStream(), Charsets.UTF_8))
                    synchronized(writers) { writers[client] = writer }
                    writeLine(writer, "READY")
                    writeLine(writer, "CALL_STATE:" + PhoneCallController.currentState)

                    while (!client.isClosed) {
                        val command = reader.readLine()?.trim() ?: break
                        val result = processCommand(command, client)
                        writeLine(writer, result)
                    }
                } catch (_: Exception) {
                    // A disconnected client should not stop the listener or other clients.
                } finally {
                    synchronized(writers) { writers.remove(client) }
                    clients.remove(client)
                }
            }
        }
    }

    private fun processCommand(command: String, socket: Socket): String {
        return try {
            when {
                command == BridgeProtocol.PING -> "PONG"
                command.startsWith("DIAL:") ->
                    PhoneCallController.dial(context, command.substringAfter(':').trim())
                command == "ANSWER" -> PhoneCallController.answer()
                command == "REJECT" -> PhoneCallController.reject()
                command == "HANGUP" -> PhoneCallController.hangup()
                command == "CALL_STATE?" -> "CALL_STATE:" + PhoneCallController.currentState
                command.startsWith("AUDIO_START:") ->
                    startAudio(command.substringAfter(':').trim(), socket)
                command == "AUDIO_STOP" -> {
                    AudioBridge.stop(context)
                    "OK:AUDIO_STOP"
                }
                else -> "ERROR:UNKNOWN_COMMAND"
            }
        } catch (e: Throwable) {
            "ERROR:" + e.javaClass.simpleName + ":" + (e.message ?: "command failed").replace('\n', ' ')
        }
    }

    private fun startAudio(portText: String, socket: Socket): String {
        val port = portText.toIntOrNull()
            ?: return "ERROR:INVALID_AUDIO_PORT"
        if (port !in 1024..65535) return "ERROR:INVALID_AUDIO_PORT"
        val address = socket.inetAddress ?: return "ERROR:CLIENT_ADDRESS_UNAVAILABLE"
        AudioBridge.stop(context)
        if (!AudioBridge.start(context, address, port)) {
            return "ERROR:AUDIO_START_FAILED"
        }
        return "OK:AUDIO_START:" + port
    }

    fun broadcastCallState(state: String) {
        updateStatus("Listening on " + BridgeProtocol.PORT + " • Call: " + state)
        val snapshot = synchronized(writers) { writers.entries.map { it.key to it.value } }
        val stale = mutableListOf<Socket>()
        snapshot.forEach { (socket, writer) ->
            try {
                writeLine(writer, "CALL_STATE:" + state)
            } catch (_: Exception) {
                stale.add(socket)
            }
        }
        stale.forEach {
            synchronized(writers) { writers.remove(it) }
            clients.remove(it)
            try { it.close() } catch (_: Exception) { }
        }
    }

    private fun writeLine(writer: BufferedWriter, line: String) {
        synchronized(writer) {
            writer.write(line)
            writer.newLine()
            writer.flush()
        }
    }

    private fun updateStatus(value: String) {
        context.getSharedPreferences(BridgeService.PREFS, Context.MODE_PRIVATE)
            .edit().putString(BridgeService.KEY_STATUS, value).apply()
    }
}
