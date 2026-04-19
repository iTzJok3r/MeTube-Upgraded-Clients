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
import com.itzjok3r.metubeapp.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    private val _allowBackgroundConnection = MutableStateFlow(settings.getAllowBackground())
    /** Whether to maintain socket connection in background. */
    val allowBackgroundConnection: StateFlow<Boolean> = _allowBackgroundConnection.asStateFlow()

    private val _subscriptions = MutableStateFlow<List<SubscriptionItem>>(emptyList())
    /** Observable list of channel/playlist subscriptions. */
    val subscriptions: StateFlow<List<SubscriptionItem>> = _subscriptions.asStateFlow()

    private val _presets = MutableStateFlow<List<String>>(emptyList())
    /** List of available server-side ytdl-dlp presets. */
    val presets: StateFlow<List<String>> = _presets.asStateFlow()

    private val _serverConfig = MutableStateFlow<Map<String, Any>>(emptyMap())
    /** Current server configuration (capabilities, paths, etc.). */
    val serverConfig: StateFlow<Map<String, Any>> = _serverConfig.asStateFlow()

    private val _networkPolicy = MutableStateFlow(settings.getNetworkPolicy())
    /** Current network policy for device downloads. */
    val networkPolicy: StateFlow<Int> = _networkPolicy.asStateFlow()

    private val _customDirs = MutableStateFlow<List<String>>(emptyList())
    /** Observable list of server-side custom download directories. */
    val customDirs: StateFlow<List<String>> = _customDirs.asStateFlow()

    private val _hasCookies = MutableStateFlow(false)
    /** Whether the server has cookies uploaded. */
    val hasCookies: StateFlow<Boolean> = _hasCookies.asStateFlow()

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
        
        // Setup persistent socket flow collection (lifecycle-bound to ViewModel)
        setupSocketCollection()
    }

    /**
     * Public trigger for manual refresh (e.g. Swipe-To-Refresh)
     */
    fun refresh() {
        loadInitialState()
    }

    /**
     * Hydrate the queue and history from GET /history, then ensure socket is connected.
     */
    private fun loadInitialState() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.getApi().getHistory()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        // Concurrent-safe replacement of full state
                        val newQueue = (body.queue + body.pending).mapNotNull { parseMapToItem(it) }
                        val newHistory = body.done.mapNotNull { parseMapToItem(it) }
                        
                        _queue.value = newQueue
                        _history.value = newHistory

                        Log.i(TAG, "Hydrated state: ${newQueue.size} queue, ${newHistory.size} done")
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

            // Ensure Socket.IO is connected after initial hydration
            SocketManager.connect(_serverUrl.value)
            
            // Fetch extra alignment data
            fetchPresets()
            fetchSubscriptions()
            fetchCookieStatus()
        }
    }

    private fun fetchPresets() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getApi().getPresets()
                if (response.isSuccessful) {
                    _presets.value = response.body()?.get("presets") ?: emptyList()
                    Log.d(TAG, "Fetched ${_presets.value.size} presets")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch presets", e)
            }
        }
    }

    private fun fetchSubscriptions() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getApi().getSubscriptions()
                if (response.isSuccessful) {
                    _subscriptions.value = response.body() ?: emptyList()
                    Log.d(TAG, "Fetched ${_subscriptions.value.size} subscriptions")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch subscriptions", e)
            }
        }
    }

    fun fetchCookieStatus() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getApi().getCookieStatus()
                if (response.isSuccessful) {
                    _hasCookies.value = response.body()?.get("has_cookies") as? Boolean ?: false
                    Log.d(TAG, "Cookie status: ${_hasCookies.value}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch cookie status", e)
            }
        }
    }

    fun deleteCookies() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getApi().deleteCookies()
                if (response.isSuccessful) {
                    _hasCookies.value = false
                    _snackbarMessage.emit("✅ Cookies deleted")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting cookies", e)
                _snackbarMessage.emit("❌ Failed to delete cookies")
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Socket.IO: Connect and subscribe to real-time events
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Setup persistent collection of Socket.IO event flows.
     *
     * This is called exactly ONCE in the ViewModel's init. The collection continues
     * as long as the ViewModel is alive, regardless of foreground/background cycles.
     */
    private fun setupSocketCollection() {
        // Listen for "added" events
        viewModelScope.launch {
            SocketManager.onAdded().collect { item ->
                _queue.update { current: List<DownloadItem> ->
                    _connectionStatus.value = "Connected"
                    val newList: List<DownloadItem> = current + item
                    // De-duplicate in place
                    val seen = LinkedHashMap<String, DownloadItem>()
                    newList.forEach { listItem: DownloadItem -> 
                        if (listItem.id.isNotEmpty()) seen[listItem.id] = listItem 
                    }
                    seen.values.toList()
                }
                Log.d(TAG, "Socket: added '${item.title}'")
            }
        }

        // Listen for "updated" events
        viewModelScope.launch {
            SocketManager.onUpdated().conflate().collect { item ->
                _queue.update { current ->
                    _connectionStatus.value = "Connected"
                    val index = current.indexOfFirst { it.id == item.id }
                    if (index != -1) {
                        current.toMutableList().apply { set(index, item) }
                    } else {
                        current + item
                    }
                }
            }
        }

        // Listen for "completed" events
        viewModelScope.launch {
            SocketManager.onCompleted().collect { item ->
                _connectionStatus.value = "Connected"
                
                // Nuclear update: remove from queue and add to history atomically
                _queue.update { current -> current.filter { q -> q.id != item.id } }
                _history.update { current ->
                    (listOf(item) + current).distinctBy { it.id }
                }
                
                Log.d(TAG, "Socket: completed '${item.title}'")
                showCompletionNotification(item)
            }
        }

        // Listen for "canceled" events
        viewModelScope.launch {
            SocketManager.onCanceled().collect { id ->
                _queue.update { current -> current.filter { item -> item.id != id } }
                Log.d(TAG, "Socket: canceled '$id'")
            }
        }

        // Listen for "cleared" events
        viewModelScope.launch {
            SocketManager.onCleared().collect { id ->
                _history.update { current -> current.filter { item -> item.id != id } }
                Log.d(TAG, "Socket: cleared '$id'")
            }
        }

        // Listen for "all" events
        viewModelScope.launch {
            SocketManager.onAll().collect { raw ->
                withContext(Dispatchers.Default) {
                    val sid = SocketManager.getSocketId()
                    _connectionStatus.value = if (sid != null) "Connected ($sid)" else "Connected"
                    Log.d(TAG, "Socket: reconciliation payload received")
                    try {
                        reconcileFullState(raw)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reconciling 'all' event", e)
                    }
                }
            }
        }

        // Server configuration
        viewModelScope.launch {
            SocketManager.onConfiguration().collect { config ->
                _serverConfig.value = config
            }
        }

        // Custom directories
        viewModelScope.launch {
            SocketManager.onCustomDirs().collect { dirs ->
                _customDirs.value = dirs
            }
        }

        // Subscription updates
        viewModelScope.launch {
            SocketManager.onSubscriptionsAll().collect { subs ->
                _subscriptions.value = subs
            }
        }

        viewModelScope.launch {
            SocketManager.onSubscriptionUpdated().collect { sub ->
                _subscriptions.update { current ->
                    current.map { if (it.id == sub.id) sub else it }
                }
            }
        }

        viewModelScope.launch {
            SocketManager.onSubscriptionAdded().collect { sub ->
                _subscriptions.update { current ->
                    (current + sub).distinctBy { it.id }
                }
            }
        }

        viewModelScope.launch {
            SocketManager.onSubscriptionRemoved().collect { id ->
                _subscriptions.update { it.filter { s -> s.id != id } }
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
            if (!element.isJsonArray) return
            val arr = element.asJsonArray
            if (arr.size() < 2) return

            // Backend dqueue.get() returns (queue_and_pending, done)
            val queueAndPendingArray = arr.get(0).asJsonArray
            val doneArray = arr.get(1).asJsonArray

            val queueItems = mutableListOf<DownloadItem>()
            queueAndPendingArray.forEach { 
                parseJsonObjectToItem(it.asJsonObject, "q_${System.currentTimeMillis()}")?.let { item -> queueItems.add(item) }
            }

            val doneItems = mutableListOf<DownloadItem>()
            doneArray.forEach { 
                parseJsonObjectToItem(it.asJsonObject, "d_${System.currentTimeMillis()}")?.let { item -> doneItems.add(item) }
            }

            // Atomic update to avoid race conditions with incoming individual events
            _queue.update { queueItems }
            _history.update { doneItems }
            
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
        downloadType: String? = null,
        preset: String? = null,
        folder: String? = null,
        autoStart: Boolean = true,
        splitByChapters: Boolean = false,
        chapterTemplate: String? = null,
        subtitleLanguage: String? = "en",
        subtitleMode: String? = "prefer_manual",
        ytdlOptionsOverrides: Map<String, Any>? = null,
        customNamePrefix: String? = null
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
                val request = MeTubeRequestBuilder.buildAddRequest(
                    url, quality, format, codec, downloadType, preset, folder,
                    autoStart, splitByChapters, chapterTemplate, subtitleLanguage, subtitleMode,
                    ytdlOptionsOverrides, customNamePrefix
                )
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
     * Start a pending download.
     */
    fun startDownload(id: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getApi().startDownloads(BulkIdRequest(listOf(id)))
                if (response.isSuccessful) {
                    _snackbarMessage.emit("✅ Download started")
                    Log.i(TAG, "Started download: $id")
                } else {
                    _snackbarMessage.emit("❌ Failed to start download")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting download", e)
            }
        }
    }

    /**
     * Trigger manual check for subscriptions.
     */
    fun checkSubscriptions(ids: List<String>? = null) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getApi().checkSubscriptions(BulkIdRequest(ids ?: emptyList()))
                if (response.isSuccessful) {
                    _snackbarMessage.emit("✅ Subscription check triggered")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking subscriptions", e)
            }
        }
    }

    /**
     * Delete subscriptions.
     */
    fun deleteSubscriptions(ids: List<String>) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getApi().deleteSubscriptions(BulkIdRequest(ids))
                if (response.isSuccessful) {
                    _subscriptions.value = _subscriptions.value.filter { it.id !in ids }
                    _snackbarMessage.emit("✅ Subscriptions deleted")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting subscriptions", e)
            }
        }
    }

    /**
     * Delete an item from the queue or history.
     *
     * MeTube uses the URL as the key for persistent storage.
     */
    fun deleteDownload(item: com.itzjok3r.metubeapp.model.DownloadItem, where: String) {
        viewModelScope.launch {
            try {
                val deleteId = item.url ?: return@launch
                val request = com.itzjok3r.metubeapp.model.DeleteRequest(listOf(deleteId), where)
                val response = RetrofitClient.getApi().deleteDownload(request)
                
                if (response.isSuccessful) {
                    if (where == "queue") {
                        _queue.value = _queue.value.filter { it.id != item.id }
                    } else if (where == "done") {
                        _history.value = _history.value.filter { it.id != item.id }
                    }
                    _snackbarMessage.emit("✅ Item deleted successfully")
                    Log.i(TAG, "Deleted item ${item.id} (url: $deleteId) from $where")
                } else {
                    _snackbarMessage.emit("❌ Failed to delete item")
                    Log.e(TAG, "Delete failed: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error deleting item", e)
                _snackbarMessage.emit("❌ Network error: ${e.localizedMessage}")
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
     * Handle app returning to the foreground.
     * Reconnects the socket and refreshes state.
     */
    fun onForeground() {
        Log.i(TAG, "App returned to foreground: reconnecting socket")
        loadInitialState()
    }

    /**
     * Handle app moving to the background.
     * Disconnects the socket to save battery and data, unless the user opted in.
     */
    fun onBackground() {
        if (!_allowBackgroundConnection.value) {
            Log.i(TAG, "App moved to background: disconnecting socket (Battery Saving Mode)")
            SocketManager.disconnect()
            _connectionStatus.value = "Disconnected"
        } else {
            Log.i(TAG, "App moved to background: keeping socket alive (Performance Mode)")
        }
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

    /**
     * Toggle whether to keep the socket connection alive in the background.
     * WARNING: Keeping it alive significantly increases battery usage.
     */
    fun setAllowBackground(enabled: Boolean) {
        _allowBackgroundConnection.value = enabled
        settings.setAllowBackground(enabled)
    }

    // ────────────────────────────────────────────────────────────────────────
    // Cleanup
    // ────────────────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        SocketManager.disconnect()
        Log.i(TAG, "ViewModel cleared, Socket.IO disconnected")
    }

    /** Local completion push notification. */
    private fun showCompletionNotification(item: DownloadItem) {
        val context = getApplication<Application>()
        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "metube_completed_channel", 
                "Completed Server Downloads", 
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val title = item.title.ifEmpty { item.filename ?: "Download" }
        val intent = android.content.Intent(context, com.itzjok3r.metubeapp.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context, 0, intent, 
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = androidx.core.app.NotificationCompat.Builder(context, "metube_completed_channel")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Server Download Complete")
            .setContentText("'$title' is ready to download/open on your device.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(item.id.hashCode(), notification)
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
                error = (map["error"] ?: map["msg"] ?: map["reason"])?.toString(),
                url = map["url"]?.toString(),
                downloadType = map["download_type"]?.toString(),
                quality = map["quality"]?.toString(),
                format = map["format"]?.toString(),
                codec = map["codec"]?.toString(),
                folder = map["folder"]?.toString(),
                customNamePrefix = map["custom_name_prefix"]?.toString(),
                msg = map["msg"]?.toString(),
                totalSize = (map["total_size"] as? Number)?.toLong(),
                timestamp = (map["timestamp"] as? Number)?.toLong(),
                playlistItemLimit = (map["playlist_item_limit"] as? Number)?.toInt(),
                splitByChapters = map["split_by_chapters"] as? Boolean,
                chapterTemplate = map["chapter_template"]?.toString(),
                subtitleLanguage = map["subtitle_language"]?.toString(),
                subtitleMode = map["subtitle_mode"]?.toString(),
                ytdlOptionsPresets = map["ytdl_options_presets"] as? List<String>
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
            val id = getStr(obj, "_id") ?: getStr(obj, "id") ?: fallbackId
            if (id.isEmpty() && fallbackId.isEmpty()) return null
            
            DownloadItem(
                id = id,
                title = getStr(obj, "title") ?: "Untitled",
                status = getStr(obj, "status") ?: "pending",
                percent = getFloat(obj, "percent"),
                speed = getStr(obj, "speed"),
                eta = getStr(obj, "eta"),
                filename = getStr(obj, "filename"),
                size = getStr(obj, "size"),
                error = getStr(obj, "error") ?: getStr(obj, "msg") ?: getStr(obj, "reason"),
                url = getStr(obj, "url"),
                downloadType = getStr(obj, "download_type"),
                quality = getStr(obj, "quality"),
                format = getStr(obj, "format"),
                codec = getStr(obj, "codec"),
                folder = getStr(obj, "folder"),
                customNamePrefix = getStr(obj, "custom_name_prefix"),
                msg = getStr(obj, "msg"),
                totalSize = getLong(obj, "total_size"),
                timestamp = getLong(obj, "timestamp"),
                playlistItemLimit = getInt(obj, "playlist_item_limit"),
                splitByChapters = getBool(obj, "split_by_chapters"),
                chapterTemplate = getStr(obj, "chapter_template"),
                subtitleLanguage = getStr(obj, "subtitle_language"),
                subtitleMode = getStr(obj, "subtitle_mode"),
                ytdlOptionsPresets = getStrList(obj, "ytdl_options_presets")
            )
        } catch (e: Exception) {
            Log.e(TAG, "JSON parsing failed for DownloadItem", e)
            null
        }
    }

    fun updateNetworkPolicy(policy: Int) {
        settings.setNetworkPolicy(policy)
        _networkPolicy.value = policy
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

    private fun getLong(obj: com.google.gson.JsonObject, key: String): Long? {
        return if (obj.has(key) && !obj.get(key).isJsonNull) obj.get(key).asLong else null
    }

    private fun getInt(obj: com.google.gson.JsonObject, key: String): Int? {
        return if (obj.has(key) && !obj.get(key).isJsonNull) obj.get(key).asInt else null
    }

    private fun getBool(obj: com.google.gson.JsonObject, key: String): Boolean? {
        return if (obj.has(key) && !obj.get(key).isJsonNull) obj.get(key).asBoolean else null
    }

    private fun getStrList(obj: com.google.gson.JsonObject, key: String): List<String>? {
        return try {
            if (obj.has(key) && !obj.get(key).isJsonNull && obj.get(key).isJsonArray) {
                obj.getAsJsonArray(key).map { it.asString }
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
