package com.itzjok3r.metubeapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.itzjok3r.metubeapp.model.DownloadItem
import com.itzjok3r.metubeapp.network.MeTubeRequestBuilder
import com.itzjok3r.metubeapp.network.RetrofitClient
import com.itzjok3r.metubeapp.network.SocketManager
import com.itzjok3r.metubeapp.util.SettingsManager
import com.itzjok3r.metubeapp.ads.SecurityManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Central ViewModel for the MeTube Client app.
 *
 * Manages the full application state using StateFlows:
 * - Download queue (active + pending)
 * - Download history (completed items)
 * - Settings (server URL, default quality, dark mode)
 * - UI events (snackbar messages via SharedFlow)
 *
 * State lifecycle:
 * 1. On init, calls GET /history to hydrate queue + history from the server.
 * 2. Connects to Socket.IO for real-time updates (added, updated, completed, etc.).
 * 3. On Socket.IO reconnect, the "all" event reconciles the full state.
 * 4. Deduplicates items by ID to prevent duplicates from reconnections.
 */
class MeTubeViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = SettingsManager(application)

    companion object {
        private const val TAG = "MeTubeViewModel"
        private const val DEFAULT_SERVER_URL = "http://localhost:8081/"
    }

    private val gson = Gson()

    // ────────────────────────────────────────────────────────────────────────
    // State: Download queue (active + pending downloads)
    // ────────────────────────────────────────────────────────────────────────

    private val _queue = MutableStateFlow<List<DownloadItem>>(emptyList())
    /** Observable list of active and pending downloads. */
    val queue: StateFlow<List<DownloadItem>> = _queue.asStateFlow()

    // ────────────────────────────────────────────────────────────────────────
    // State: Download history (completed downloads)
    // ────────────────────────────────────────────────────────────────────────

    private val _history = MutableStateFlow<List<DownloadItem>>(emptyList())
    /** Observable list of completed downloads. */
    val history: StateFlow<List<DownloadItem>> = _history.asStateFlow()

    // ────────────────────────────────────────────────────────────────────────
    // State: Loading / error indicators
    // ────────────────────────────────────────────────────────────────────────

    private val _isLoading = MutableStateFlow(false)
    /** Whether a network operation is in progress. */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    /** Whether a download submission is in progress. */
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Disconnected")
    /** Human-readable Socket.IO connection status. */
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    // ────────────────────────────────────────────────────────────────────────
    // State: Settings (in-memory only, no persistence)
    // ────────────────────────────────────────────────────────────────────────

    private val _serverUrl = MutableStateFlow(settings.getServerUrl())
    /** Current MeTube server URL. */
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _defaultQuality = MutableStateFlow(settings.getDefaultQuality())
    /** Default quality selection for new downloads. */
    val defaultQuality: StateFlow<String> = _defaultQuality.asStateFlow()

    private val _defaultType = MutableStateFlow(settings.getDefaultType())
    /** Default media type. */
    val defaultType: StateFlow<String> = _defaultType.asStateFlow()

    private val _defaultFormat = MutableStateFlow(settings.getDefaultFormat())
    /** Default format hint for new downloads. */
    val defaultFormat: StateFlow<String> = _defaultFormat.asStateFlow()

    private val _defaultCodec = MutableStateFlow(settings.getDefaultCodec())
    /** Default video codec for new downloads. */
    val defaultCodec: StateFlow<String> = _defaultCodec.asStateFlow()

    private val _isDarkMode = MutableStateFlow(settings.isDarkMode())
    /** Whether dark mode is enabled. */
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // ────────────────────────────────────────────────────────────────────────
    // Events: One-shot UI events (snackbar messages)
    // ────────────────────────────────────────────────────────────────────────

    private val _snackbarMessage = MutableSharedFlow<String>()
    /** One-shot snackbar messages. Collect in the UI layer. */
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    // ────────────────────────────────────────────────────────────────────────
    // Initialization: Hydrate state from /history, then connect Socket.IO
    // ────────────────────────────────────────────────────────────────────────

    init {
        // Initialize Retrofit with persisted URL
        RetrofitClient.setBaseUrl(settings.getServerUrl())
        loadInitialState()
    }

    /**
     * Hydrate the queue and history from GET /history, then start Socket.IO.
     *
     * This ensures the UI shows current state immediately on launch instead
     * of waiting for individual Socket.IO events to trickle in.
     */
    private fun loadInitialState() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.getApi().getHistory()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        // Parse queue items (active + pending)
                        val queueItems = (body.queue + body.pending).mapNotNull { parseMapToItem(it) }
                        _queue.value = queueItems

                        // Parse completed items
                        val doneItems = body.done.mapNotNull { parseMapToItem(it) }
                        _history.value = doneItems

                        Log.i(TAG, "Hydrated state: ${queueItems.size} queue, ${doneItems.size} done")
                    }
                } else {
                    Log.e(TAG, "GET /history failed: ${response.code()} ${response.message()}")
                    _snackbarMessage.emit("Failed to load history: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading initial state", e)
                _snackbarMessage.emit("Connection error: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }

            // Start Socket.IO after initial hydration
            connectSocket()
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Socket.IO: Connect and subscribe to real-time events
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Establish Socket.IO connection and start collecting event flows.
     *
     * Each event type is collected in a separate coroutine so they don't
     * block each other. Items are deduplicated by ID on every update.
     */
    private fun connectSocket() {
        SocketManager.connect(_serverUrl.value)
        _connectionStatus.value = "Connecting..."

        // Listen for "added" events — new items appear in the queue
        viewModelScope.launch {
            SocketManager.onAdded().collect { item ->
                _connectionStatus.value = "Connected"
                _queue.value = deduplicateById(_queue.value + item)
                Log.d(TAG, "Socket: added '${item.title}'")
            }
        }

        // Listen for "updated" events — progress/status changes
        viewModelScope.launch {
            SocketManager.onUpdated().collect { item ->
                _connectionStatus.value = "Connected"
                _queue.value = _queue.value.map {
                    if (it.id == item.id) item else it
                }.let { list ->
                    // If the item wasn't in the queue, add it (edge case after reconnect)
                    if (list.none { it.id == item.id }) list + item else list
                }
            }
        }

        // Listen for "completed" events — move items from queue to history
        viewModelScope.launch {
            SocketManager.onCompleted().collect { item ->
                _connectionStatus.value = "Connected"
                // Remove from queue
                _queue.value = _queue.value.filter { it.id != item.id }
                // Add to history (deduplicated)
                _history.value = deduplicateById(listOf(item) + _history.value)
                Log.d(TAG, "Socket: completed '${item.title}'")
            }
        }

        // Listen for "canceled" events — remove from queue
        viewModelScope.launch {
            SocketManager.onCanceled().collect { id ->
                _queue.value = _queue.value.filter { it.id != id }
                Log.d(TAG, "Socket: canceled '$id'")
            }
        }

        // Listen for "cleared" events — remove from history
        viewModelScope.launch {
            SocketManager.onCleared().collect { id ->
                _history.value = _history.value.filter { it.id != id }
                Log.d(TAG, "Socket: cleared '$id'")
            }
        }
        // Listen for "all" events — full state reconciliation on connect/reconnect
        viewModelScope.launch {
            SocketManager.onAll().collect { raw ->
                _connectionStatus.value = "Connected"
                Log.d(TAG, "Socket: reconciliation payload received (${raw.length} bytes)")
                try {
                    reconcileFullState(raw)
                } catch (e: Exception) {
                    Log.e(TAG, "Error reconciling 'all' event", e)
                }
            }
        }
    }

    /**
     * Reconcile the full state from a Socket.IO "all" event.
     *
     * The "all" event payload contains the complete current queue state.
     * This replaces the current queue/history to fix any drift from
     * missed events during disconnection.
     */
    private fun reconcileFullState(raw: String) {
        try {
            val element = JsonParser.parseString(raw)
            if (!element.isJsonObject) return

            val obj = element.asJsonObject

            // Parse queue items
            val queueItems = mutableListOf<DownloadItem>()
            if (obj.has("queue") && obj.get("queue").isJsonObject) {
                val queueObj = obj.getAsJsonObject("queue")
                for (key in queueObj.keySet()) {
                    try {
                        val itemObj = queueObj.getAsJsonObject(key)
                        val item = parseJsonObjectToItem(itemObj, key)
                        if (item != null) queueItems.add(item)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse queue item '$key'", e)
                    }
                }
            }

            // Parse done items
            val doneItems = mutableListOf<DownloadItem>()
            if (obj.has("done") && obj.get("done").isJsonObject) {
                val doneObj = obj.getAsJsonObject("done")
                for (key in doneObj.keySet()) {
                    try {
                        val itemObj = doneObj.getAsJsonObject(key)
                        val item = parseJsonObjectToItem(itemObj, key)
                        if (item != null) doneItems.add(item)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse done item '$key'", e)
                    }
                }
            }

            _queue.value = queueItems
            _history.value = doneItems
            Log.i(TAG, "Reconciled full state: ${queueItems.size} queue, ${doneItems.size} done")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse 'all' event", e)
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Actions: Submit downloads, update settings, refresh
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Submit a URL for download to the MeTube server.
     *
     * Uses [MeTubeRequestBuilder] to construct the payload, ensuring the
     * legacy-to-new API migration is handled in one place.
     *
     * @param url     The URL to download.
     * @param quality The selected quality (best, 1080, 720, 480, mp3).
     */
    fun addDownload(
        url: String, 
        quality: String? = null,
        format: String? = null,
        codec: String? = null,
        downloadType: String? = null
    ) {
        if (url.isBlank()) {
            viewModelScope.launch { _snackbarMessage.emit("Please enter a URL") }
            return
        }

        // Security check: ensure ads haven't been removed from the UI
        if (!SecurityManager.validateSystemIntegrity()) {
            viewModelScope.launch { 
                _snackbarMessage.emit("❌ System integrity compromised. Please restore original app files.")
            }
            return
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val request = MeTubeRequestBuilder.buildAddRequest(url, quality, format, codec, downloadType)
                val response = RetrofitClient.getApi().addDownload(request)

                if (response.isSuccessful) {
                    _snackbarMessage.emit("✅ Added to download queue!")
                    Log.i(TAG, "Download added: $url ($quality)")
                } else {
                    // Parse error message from the response body
                    val errorBody = response.errorBody()?.string()
                    val errorMsg = try {
                        val json = JsonParser.parseString(errorBody).asJsonObject
                        json.get("message")?.asString ?: json.get("reason")?.asString
                    } catch (e: Exception) {
                        null
                    } ?: response.message() ?: "Unknown error"

                    _snackbarMessage.emit("❌ Error: $errorMsg")
                    Log.e(TAG, "Add failed: ${response.code()} $errorMsg")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error adding download", e)
                _snackbarMessage.emit("❌ Network error: ${e.localizedMessage}")
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    /**
     * Retry loading the initial state (queue + history) from the server.
     * Useful after a network error or server URL change.
     */
    fun retry() {
        // Disconnect old socket if any
        SocketManager.disconnect()
        // Re-initialize
        loadInitialState()
    }

    /**
     * Update the MeTube server URL.
     *
     * Disconnects the current Socket.IO connection, updates the Retrofit
     * base URL, and re-initializes the full state from the new server.
     *
     * @param url The new server URL.
     */
    fun updateServerUrl(url: String) {
        val normalized = if (url.endsWith("/")) url else "$url/"
        _serverUrl.value = normalized
        settings.setServerUrl(normalized)
        RetrofitClient.setBaseUrl(normalized)
        SocketManager.disconnect()
        loadInitialState()
    }

    /**
     * Update the default quality for new downloads.
     *
     * @param quality One of: "best", "1080", "720", "480", "mp3".
     */
    fun updateDefaultQuality(quality: String) {
        _defaultQuality.value = quality
        settings.setDefaultQuality(quality)
    }

    fun updateDefaultType(type: String) {
        _defaultType.value = type
        settings.setDefaultType(type)
    }

    fun updateDefaultFormat(format: String) {
        _defaultFormat.value = format
        settings.setDefaultFormat(format)
    }

    fun updateDefaultCodec(codec: String) {
        _defaultCodec.value = codec
        settings.setDefaultCodec(codec)
    }

    /**
     * Toggle between dark and light mode.
     *
     * @param enabled True for dark mode, false for light mode.
     */
    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        settings.setDarkMode(enabled)
    }

    // ────────────────────────────────────────────────────────────────────────
    // Cleanup
    // ────────────────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        SocketManager.disconnect()
        Log.i(TAG, "ViewModel cleared, Socket.IO disconnected")
    }

    // ────────────────────────────────────────────────────────────────────────
    // Private helpers: JSON parsing & deduplication
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Deduplicate a list of [DownloadItem]s by their ID.
     * Later items in the list take priority (newer data wins).
     */
    private fun deduplicateById(items: List<DownloadItem>): List<DownloadItem> {
        val seen = LinkedHashMap<String, DownloadItem>()
        for (item in items) {
            if (item.id.isNotEmpty()) {
                seen[item.id] = item
            }
        }
        return seen.values.toList()
    }

    /**
     * Parse a Map<String, Any?> (from Gson deserialization of /history) into a [DownloadItem].
     */
    private fun parseMapToItem(map: Map<String, Any?>): DownloadItem? {
        return try {
            DownloadItem(
                id = (map["_id"] ?: map["id"] ?: "").toString(),
                title = (map["title"] ?: "Untitled").toString(),
                status = (map["status"] ?: "pending").toString(),
                percent = (map["percent"] as? Number)?.toFloat() ?: 0f,
                speed = map["speed"]?.toString(),
                eta = map["eta"]?.toString(),
                filename = map["filename"]?.toString(),
                size = map["size"]?.toString(),
                error = (map["error"] ?: map["msg"])?.toString(),
                url = map["url"]?.toString(),
                downloadType = map["download_type"]?.toString(),
                quality = map["quality"]?.toString(),
                format = map["format"]?.toString(),
                codec = map["codec"]?.toString()
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse map to DownloadItem", e)
            null
        }
    }

    /**
     * Parse a Gson JsonObject into a [DownloadItem].
     * Used when processing the "all" Socket.IO event.
     */
    private fun parseJsonObjectToItem(
        obj: com.google.gson.JsonObject,
        fallbackId: String
    ): DownloadItem? {
        return try {
            DownloadItem(
                id = getStr(obj, "_id") ?: getStr(obj, "id") ?: fallbackId,
                title = getStr(obj, "title") ?: "Untitled",
                status = getStr(obj, "status") ?: "pending",
                percent = getFloat(obj, "percent"),
                speed = getStr(obj, "speed"),
                eta = getStr(obj, "eta"),
                filename = getStr(obj, "filename"),
                size = getStr(obj, "size"),
                error = getStr(obj, "error") ?: getStr(obj, "msg"),
                url = getStr(obj, "url"),
                downloadType = getStr(obj, "download_type"),
                quality = getStr(obj, "quality"),
                format = getStr(obj, "format"),
                codec = getStr(obj, "codec")
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse JsonObject to DownloadItem", e)
            null
        }
    }

    /** Safely extract a string value from a JsonObject. */
    private fun getStr(obj: com.google.gson.JsonObject, key: String): String? {
        return if (obj.has(key) && !obj.get(key).isJsonNull) obj.get(key).asString else null
    }

    /** Safely extract a float value from a JsonObject, defaulting to 0. */
    private fun getFloat(obj: com.google.gson.JsonObject, key: String): Float {
        return try {
            if (obj.has(key) && !obj.get(key).isJsonNull) obj.get(key).asFloat else 0f
        } catch (e: Exception) {
            0f
        }
    }
}
