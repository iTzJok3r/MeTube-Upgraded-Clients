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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
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

    /** StateFlow that tracks the active socket instance for reactive flow management. */
    private val _socketState = MutableStateFlow<Socket?>(null)

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
            _socketState.value = socket
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
            _socketState.value = null
            currentUrl = ""
            Log.i(TAG, "Socket.IO disconnected and cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during Socket.IO disconnect", e)
        }
    }

    /**
     * Get the current Socket.IO session ID.
     */
    fun getSocketId(): String? = socket?.id()

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
     * Create a Flow that emits the configuration map for the "configuration" event.
     */
    fun onConfiguration(): Flow<Map<String, Any>> = _socketState.flatMapLatest { s ->
        if (s == null) return@flatMapLatest emptyFlow()
        
        callbackFlow {
            s.on("configuration") { args ->
                try {
                    val raw = args.firstOrNull()?.toString()
                    if (raw != null) {
                        val config = gson.fromJson<Map<String, Any>>(raw, object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type)
                        trySend(config)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing 'configuration' event", e)
                }
            }
            awaitClose { s.off("configuration") }
        }
    }

    /**
     * Create a Flow that emits the list of custom directories for the "custom_dirs" event.
     */
    fun onCustomDirs(): Flow<List<String>> = _socketState.flatMapLatest { s ->
        if (s == null) return@flatMapLatest emptyFlow()
        
        callbackFlow {
            s.on("custom_dirs") { args ->
                try {
                    val raw = args.firstOrNull()?.toString()
                    if (raw != null) {
                        val listType = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
                        val dirs = gson.fromJson<List<String>>(raw, listType)
                        trySend(dirs)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing 'custom_dirs' event", e)
                }
            }
            awaitClose { s.off("custom_dirs") }
        }
    }

    /**
     * Create a Flow that emits [com.itzjok3r.metubeapp.model.SubscriptionItem]s for the "subscription_added" event.
     */
    fun onSubscriptionAdded(): Flow<com.itzjok3r.metubeapp.model.SubscriptionItem> = _socketState.flatMapLatest { s ->
        if (s == null) return@flatMapLatest emptyFlow()
        
        callbackFlow {
            s.on("subscription_added") { args ->
                try {
                    val raw = args.firstOrNull()?.toString()
                    if (raw != null) {
                        val sub = gson.fromJson(raw, com.itzjok3r.metubeapp.model.SubscriptionItem::class.java)
                        trySend(sub)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing 'subscription_added' event", e)
                }
            }
            awaitClose { s.off("subscription_added") }
        }
    }

    /**
     * Create a Flow that emits the ID string for "subscription_removed" events.
     */
    fun onSubscriptionRemoved(): Flow<String> = _socketState.flatMapLatest { s ->
        if (s == null) return@flatMapLatest emptyFlow()
        
        callbackFlow {
            s.on("subscription_removed") { args ->
                try {
                    val id = parseIdFromArgs(args)
                    if (id != null) {
                        trySend(id)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing 'subscription_removed' event", e)
                }
            }
            awaitClose { s.off("subscription_removed") }
        }
    }

    /**
     * Create a Flow that emits the full list of subscriptions for the "subscriptions_all" event.
     */
    fun onSubscriptionsAll(): Flow<List<com.itzjok3r.metubeapp.model.SubscriptionItem>> = _socketState.flatMapLatest { s ->
        if (s == null) return@flatMapLatest emptyFlow()
        
        callbackFlow {
            s.on("subscriptions_all") { args ->
                try {
                    val raw = args.firstOrNull()?.toString() ?: return@on
                    val listType = object : com.google.gson.reflect.TypeToken<List<com.itzjok3r.metubeapp.model.SubscriptionItem>>() {}.type
                    val subs: List<com.itzjok3r.metubeapp.model.SubscriptionItem> = gson.fromJson(raw, listType)
                    trySend(subs)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing 'subscriptions_all' event", e)
                }
            }
            awaitClose { s.off("subscriptions_all") }
        }
    }

    /**
     * Create a Flow that emits a [com.itzjok3r.metubeapp.model.SubscriptionItem] for the "subscription_updated" event.
     */
    fun onSubscriptionUpdated(): Flow<com.itzjok3r.metubeapp.model.SubscriptionItem> = _socketState.flatMapLatest { s ->
        if (s == null) return@flatMapLatest emptyFlow()
        
        callbackFlow {
            s.on("subscription_updated") { args ->
                try {
                    val raw = args.firstOrNull()?.toString()
                    if (raw != null) {
                        val sub = gson.fromJson(raw, com.itzjok3r.metubeapp.model.SubscriptionItem::class.java)
                        trySend(sub)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing 'subscription_updated' event", e)
                }
            }
            awaitClose { s.off("subscription_updated") }
        }
    }

    /**
     * Create a Flow that emits the ID string for "canceled" events.
     */
    fun onCanceled(): Flow<String> = _socketState.flatMapLatest { s ->
        if (s == null) return@flatMapLatest emptyFlow()
        
        callbackFlow {
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
    }

    /**
     * Create a Flow that emits the ID string for "cleared" events.
     */
    fun onCleared(): Flow<String> = _socketState.flatMapLatest { s ->
        if (s == null) return@flatMapLatest emptyFlow()
        
        callbackFlow {
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
    }

    /**
     * Create a Flow that emits the full "all" event payload (sent on Socket.IO connect).
     */
    fun onAll(): Flow<String> = _socketState.flatMapLatest { s ->
        if (s == null) return@flatMapLatest emptyFlow()
        
        callbackFlow {
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
    private fun createEventFlow(eventName: String): Flow<DownloadItem> = _socketState.flatMapLatest { s ->
        if (s == null) return@flatMapLatest emptyFlow()

        callbackFlow {
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
                id = obj.getStringOrNull("_id") ?: obj.getStringOrNull("id") ?: "",
                title = obj.getStringOrDefault("title", "Untitled"),
                status = obj.getStringOrDefault("status", "pending"),
                percent = obj.getFloatOrDefault("percent", 0f),
                speed = obj.getStringOrNull("speed"),
                eta = obj.getStringOrNull("eta"),
                filename = obj.getStringOrNull("filename"),
                size = obj.getStringOrNull("size"),
                error = obj.getStringOrNull("error") ?: obj.getStringOrNull("msg") ?: obj.getStringOrNull("reason"),
                url = obj.getStringOrNull("url"),
                downloadType = obj.getStringOrNull("download_type"),
                quality = obj.getStringOrNull("quality"),
                format = obj.getStringOrNull("format"),
                codec = obj.getStringOrNull("codec"),
                folder = obj.getStringOrNull("folder"),
                customNamePrefix = obj.getStringOrNull("custom_name_prefix"),
                msg = obj.getStringOrNull("msg"),
                totalSize = obj.getLongOrNull("total_size"),
                timestamp = obj.getLongOrNull("timestamp"),
                playlistItemLimit = obj.getIntOrNull("playlist_item_limit"),
                splitByChapters = obj.getBooleanOrNull("split_by_chapters"),
                chapterTemplate = obj.getStringOrNull("chapter_template"),
                subtitleLanguage = obj.getStringOrNull("subtitle_language"),
                subtitleMode = obj.getStringOrNull("subtitle_mode"),
                ytdlOptionsPresets = obj.getStringListOrNull("ytdl_options_presets")
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

    private fun JsonObject.getLongOrNull(key: String): Long? {
        return try {
            if (has(key) && !get(key).isJsonNull) get(key).asLong else null
        } catch (e: Exception) {
            null
        }
    }

    private fun JsonObject.getIntOrNull(key: String): Int? {
        return try {
            if (has(key) && !get(key).isJsonNull) get(key).asInt else null
        } catch (e: Exception) {
            null
        }
    }

    private fun JsonObject.getBooleanOrNull(key: String): Boolean? {
        return try {
            if (has(key) && !get(key).isJsonNull) get(key).asBoolean else null
        } catch (e: Exception) {
            null
        }
    }

    private fun JsonObject.getStringListOrNull(key: String): List<String>? {
        return try {
            if (has(key) && !get(key).isJsonNull && get(key).isJsonArray) {
                get(key).asJsonArray.map { it.asString }
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
