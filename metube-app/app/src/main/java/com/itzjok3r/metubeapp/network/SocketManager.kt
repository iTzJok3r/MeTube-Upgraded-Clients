package com.itzjok3r.metubeapp.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.itzjok3r.metubeapp.model.DownloadItem
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONObject
import java.net.URI

/**
 * Manages the Socket.IO connection to the MeTube server for real-time events.
 *
 * Provides reactive [Flow] streams for each event type (added, updated, completed,
 * canceled, cleared, error). Handles automatic reconnection and defensive JSON parsing.
 *
 * Usage pattern:
 * 1. Call [connect] to establish the Socket.IO connection.
 * 2. Collect from event flows ([onAdded], [onUpdated], etc.) in the ViewModel.
 * 3. Call [disconnect] when the app goes to background or is destroyed.
 *
 * The "all" event (emitted on connect) provides the full current state and is
 * used for initial hydration alongside the /history endpoint.
 */
object SocketManager {

    private const val TAG = "SocketManager"
    private val gson = Gson()

    /** The Socket.IO client instance; recreated when the server URL changes. */
    @Volatile
    private var socket: Socket? = null

    /** Tracks the URL the current socket is connected to. */
    @Volatile
    private var currentUrl: String = ""

    /**
     * Connect to the MeTube Socket.IO server.
     *
     * If already connected to the same URL, this is a no-op.
     * If the URL has changed, disconnects the old socket first.
     *
     * @param serverUrl The MeTube server base URL.
     */
    fun connect(serverUrl: String) {
        val normalizedUrl = if (serverUrl.endsWith("/")) serverUrl.dropLast(1) else serverUrl

        // Already connected to the same server — no-op
        if (socket?.connected() == true && currentUrl == normalizedUrl) {
            Log.d(TAG, "Already connected to $normalizedUrl")
            return
        }

        // Disconnect existing socket if URL changed
        disconnect()

        try {
            // Configure Socket.IO options for Cloudflare-proxied HTTPS
            val options = IO.Options().apply {
                forceNew = true
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE
                reconnectionDelay = 1000
                reconnectionDelayMax = 5000
                transports = arrayOf("polling", "websocket")
                timeout = 15000
            }

            socket = IO.socket(URI.create(normalizedUrl), options).apply {
                // Connection lifecycle logging
                on(Socket.EVENT_CONNECT) {
                    Log.i(TAG, "Socket.IO connected to $normalizedUrl")
                }
                on(Socket.EVENT_DISCONNECT) { args ->
                    val reason = args.firstOrNull()?.toString() ?: "unknown"
                    Log.w(TAG, "Socket.IO disconnected: $reason")
                }
                on(Socket.EVENT_CONNECT_ERROR) { args ->
                    val error = args.firstOrNull()?.toString() ?: "unknown error"
                    Log.e(TAG, "Socket.IO connection error: $error")
                }

                connect()
            }
            currentUrl = normalizedUrl
            Log.i(TAG, "Initiating Socket.IO connection to $normalizedUrl")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create Socket.IO connection", e)
        }
    }

    /**
     * Disconnect from the Socket.IO server and release resources.
     */
    fun disconnect() {
        try {
            socket?.apply {
                off()          // Remove all listeners
                disconnect()   // Close the connection
                close()        // Release resources
            }
            socket = null
            currentUrl = ""
            Log.i(TAG, "Socket.IO disconnected and cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during Socket.IO disconnect", e)
        }
    }

    /**
     * Check whether the socket is currently connected.
     */
    fun isConnected(): Boolean = socket?.connected() == true

    /**
     * Create a Flow that emits [DownloadItem]s for the "added" event.
     *
     * Emitted when a new download is added to the MeTube queue.
     */
    fun onAdded(): Flow<DownloadItem> = createEventFlow("added")

    /**
     * Create a Flow that emits [DownloadItem]s for the "updated" event.
     *
     * Emitted when a download's progress, status, or metadata changes.
     * These events fire frequently during active downloads.
     */
    fun onUpdated(): Flow<DownloadItem> = createEventFlow("updated")

    /**
     * Create a Flow that emits [DownloadItem]s for the "completed" event.
     *
     * Emitted when a download finishes successfully.
     */
    fun onCompleted(): Flow<DownloadItem> = createEventFlow("completed")

    /**
     * Create a Flow that emits the ID string for "canceled" events.
     */
    fun onCanceled(): Flow<String> = callbackFlow {
        val s = socket ?: run {
            close()
            return@callbackFlow
        }
        s.on("canceled") { args ->
            try {
                val id = parseIdFromArgs(args)
                if (id != null) {
                    trySend(id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing 'canceled' event", e)
            }
        }
        awaitClose {
            s.off("canceled")
        }
    }

    /**
     * Create a Flow that emits the ID string for "cleared" events.
     */
    fun onCleared(): Flow<String> = callbackFlow {
        val s = socket ?: run {
            close()
            return@callbackFlow
        }
        s.on("cleared") { args ->
            try {
                val id = parseIdFromArgs(args)
                if (id != null) {
                    trySend(id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing 'cleared' event", e)
            }
        }
        awaitClose {
            s.off("cleared")
        }
    }

    /**
     * Create a Flow that emits the full "all" event payload (sent on Socket.IO connect).
     *
     * This contains the complete current state of the download queue and is useful
     * for reconciling state after a reconnection.
     */
    fun onAll(): Flow<String> = callbackFlow {
        val s = socket ?: run {
            close()
            return@callbackFlow
        }
        s.on("all") { args ->
            try {
                val raw = args.firstOrNull()?.toString()
                if (raw != null) {
                    trySend(raw)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing 'all' event", e)
            }
        }
        awaitClose {
            s.off("all")
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Generic event flow factory for events that carry a DownloadItem JSON.
     *
     * The MeTube backend emits event data as a JSON-encoded string (via ObjectSerializer).
     * This method defensively parses that string into a [DownloadItem], falling back
     * on empty/default fields if any property is missing or malformed.
     */
    private fun createEventFlow(eventName: String): Flow<DownloadItem> = callbackFlow {
        val s = socket ?: run {
            close()
            return@callbackFlow
        }

        s.on(eventName) { args ->
            try {
                val item = parseDownloadItem(args)
                if (item != null) {
                    trySend(item)
                } else {
                    Log.w(TAG, "Failed to parse '$eventName' event: ${args.firstOrNull()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing '$eventName' event", e)
            }
        }

        awaitClose {
            s.off(eventName)
        }
    }

    /**
     * Parse Socket.IO event args into a [DownloadItem].
     *
     * The backend sends data as a JSON string via ObjectSerializer.
     * Handles both direct JSONObject args and string-encoded JSON.
     */
    private fun parseDownloadItem(args: Array<out Any>): DownloadItem? {
        val raw = args.firstOrNull() ?: return null

        return try {
            // The backend serializes via ObjectSerializer which produces a JSON string
            val jsonString = when (raw) {
                is JSONObject -> raw.toString()
                is String -> raw
                else -> raw.toString()
            }

            // Parse using Gson for type safety
            val jsonElement = JsonParser.parseString(jsonString)
            if (!jsonElement.isJsonObject) return null
            val obj = jsonElement.asJsonObject

            DownloadItem(
                id = obj.getStringOrDefault("_id", obj.getStringOrDefault("id", "")),
                title = obj.getStringOrDefault("title", "Untitled"),
                status = obj.getStringOrDefault("status", "pending"),
                percent = obj.getFloatOrDefault("percent", 0f),
                speed = obj.getStringOrNull("speed"),
                eta = obj.getStringOrNull("eta"),
                filename = obj.getStringOrNull("filename"),
                size = obj.getStringOrNull("size"),
                error = obj.getStringOrNull("error") ?: obj.getStringOrNull("msg"),
                url = obj.getStringOrNull("url")
            )
        } catch (e: Exception) {
            Log.e(TAG, "JSON parsing failed for DownloadItem", e)
            null
        }
    }

    /**
     * Parse an ID string from Socket.IO event args.
     * The backend wraps IDs in JSON string encoding (with quotes).
     */
    private fun parseIdFromArgs(args: Array<out Any>): String? {
        val raw = args.firstOrNull() ?: return null
        val str = raw.toString().trim().removeSurrounding("\"")
        return str.ifEmpty { null }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Gson extension helpers for defensive JSON parsing
    // ────────────────────────────────────────────────────────────────────────

    private fun JsonObject.getStringOrDefault(key: String, default: String): String {
        return try {
            if (has(key) && !get(key).isJsonNull) get(key).asString else default
        } catch (e: Exception) {
            default
        }
    }

    private fun JsonObject.getStringOrNull(key: String): String? {
        return try {
            if (has(key) && !get(key).isJsonNull) get(key).asString else null
        } catch (e: Exception) {
            null
        }
    }

    private fun JsonObject.getFloatOrDefault(key: String, default: Float): Float {
        return try {
            if (has(key) && !get(key).isJsonNull) get(key).asFloat else default
        } catch (e: Exception) {
            default
        }
    }
}
