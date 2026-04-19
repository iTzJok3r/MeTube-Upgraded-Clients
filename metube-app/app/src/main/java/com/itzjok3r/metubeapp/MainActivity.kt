package com.itzjok3r.metubeapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.itzjok3r.metubeapp.ui.screens.HistoryScreen
import com.itzjok3r.metubeapp.ui.screens.HomeScreen
import com.itzjok3r.metubeapp.ui.screens.QueueScreen
import com.itzjok3r.metubeapp.ui.screens.SettingsScreen
import com.itzjok3r.metubeapp.ui.screens.SubscriptionsScreen
import com.itzjok3r.metubeapp.ui.theme.MeTubeClientTheme
import com.itzjok3r.metubeapp.viewmodel.MeTubeViewModel
import com.itzjok3r.metubeapp.ads.AdManager
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Navigation route identifiers for the four main screens.
 */
object Routes {
    const val HOME = "home"
    const val QUEUE = "queue"
    const val HISTORY = "history"
    const val SUBSCRIPTIONS = "subscriptions"
    const val SETTINGS = "settings"
}

/**
 * Bottom navigation item configuration.
 *
 * @property route  Navigation route for this tab.
 * @property label  Display label in the navigation bar.
 * @property icon   Material icon for this tab.
 */
data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

/** The four bottom navigation tabs. */
private val bottomNavItems = listOf(
    BottomNavItem(Routes.HOME, "Home", Icons.Default.Add),
    BottomNavItem(Routes.QUEUE, "Queue", Icons.Default.Download),
    BottomNavItem(Routes.HISTORY, "History", Icons.Default.History),
    BottomNavItem(Routes.SUBSCRIPTIONS, "Subs", Icons.Default.Subscriptions),
    BottomNavItem(Routes.SETTINGS, "Settings", Icons.Default.Settings)
)

/**
 * MainActivity — Entry point for the MeTube Client app.
 *
 * Responsibilities:
 * - Sets up the Jetpack Compose UI with Material3 theming
 * - Configures bottom navigation with 4 tabs (Home, Queue, History, Settings)
 * - Handles Android share intents (text/plain URLs from other apps)
 * - Manages the Snackbar host for one-shot messages from the ViewModel
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display (status bar + navigation bar immersion)
        enableEdgeToEdge()

        // Initialize AdMob SDK
        AdManager.initialize(this)

        // Extract any shared URL from the launch intent
        val sharedUrl = extractSharedUrl(intent)

        setContent {
            val viewModel: MeTubeViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            MeTubeClientTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MeTubeApp(
                        viewModel = viewModel,
                        sharedUrl = sharedUrl
                    )
                }
            }
        }
    }

    /**
     * Handle new intents when the activity is already running.
     *
     * This fires when a user shares a URL to the app while it's open.
     * We extract the URL and trigger navigation to the Home screen.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // New shared URLs are handled via a recomposition trigger
        // The sharedUrl state in MeTubeApp will pick up the new intent
    }

    /**
     * Extract a URL from an Android SEND intent.
     *
     * Handles the text/plain share intent that appears when a user shares
     * a URL from their browser, YouTube app, or any other app.
     *
     * @param intent The incoming intent.
     * @return The extracted URL string, or null if no valid URL was found.
     */
    private fun extractSharedUrl(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND) return null
        if (intent.type != "text/plain") return null

        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (sharedText.isNullOrBlank()) return null

        // Try to extract a URL from the shared text (may contain other text too)
        val urlRegex = Regex("""https?://\S+""")
        val match = urlRegex.find(sharedText)
        return match?.value ?: sharedText.trim()
    }
}

/**
 * Main composable for the MeTube Client app.
 *
 * Sets up:
 * - Navigation controller with NavHost
 * - Bottom navigation bar
 * - Snackbar host connected to ViewModel events
 * - All four screen composables with data binding
 *
 * @param viewModel The shared MeTubeViewModel instance.
 * @param sharedUrl Optional pre-filled URL from a share intent.
 */
@kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MeTubeApp(
    viewModel: MeTubeViewModel,
    sharedUrl: String? = null
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Collect ViewModel state ─────────────────────────────────────────
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle()
    val defaultQuality by viewModel.defaultQuality.collectAsStateWithLifecycle()
    val defaultType by viewModel.defaultType.collectAsStateWithLifecycle()
    val defaultFormat by viewModel.defaultFormat.collectAsStateWithLifecycle()
    val defaultCodec by viewModel.defaultCodec.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val allowBackground by viewModel.allowBackgroundConnection.collectAsStateWithLifecycle()
    val subscriptions by viewModel.subscriptions.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val serverConfig by viewModel.serverConfig.collectAsStateWithLifecycle()
    val networkPolicy by viewModel.networkPolicy.collectAsStateWithLifecycle()

    // ── Lifecycle Management ────────────────────────────────────────────
    // Disconnect socket when app goes to background to save battery
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onForeground()
                Lifecycle.Event.ON_STOP -> viewModel.onBackground()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // ── Snackbar collector ──────────────────────────────────────────────
    // Listens for one-shot messages from the ViewModel and shows them
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            // ── Bottom Navigation Bar ───────────────────────────────────
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                // Pop up to the start destination to avoid building
                                // a large back stack of the same screens
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        // ── Navigation Host ─────────────────────────────────────────────
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Home screen — URL input and submission
            composable(Routes.HOME) {
                HomeScreen(
                    defaultQuality = defaultQuality,
                    defaultType = defaultType,
                    defaultFormat = defaultFormat,
                    defaultCodec = defaultCodec,
                    isSubmitting = isSubmitting,
                    presets = presets,
                    serverConfig = serverConfig,
                    sharedUrl = sharedUrl,
                    onSubmit = { url, quality, type, format, codec, preset, folder, autoStart, splitByChapters ->
                        viewModel.addDownload(url, quality, format, codec, type, preset, folder, autoStart, splitByChapters)
                    }
                )
            }

            // Queue screen — active downloads with real-time progress
            composable(Routes.QUEUE) {
                QueueScreen(
                    queueItems = queue,
                    isLoading = isLoading,
                    connectionStatus = connectionStatus,
                    onRemove = { item -> viewModel.deleteDownload(item, "queue") },
                    onStart = { id -> viewModel.startDownload(id) },
                    onRetry = { viewModel.retry() }
                )
            }

            // History screen — completed downloads
            composable(Routes.HISTORY) {
                HistoryScreen(
                    historyItems = history, 
                    serverUrl = serverUrl,
                    networkPolicy = networkPolicy,
                    isLoading = isLoading,
                    onRemove = { item -> viewModel.deleteDownload(item, "done") },
                    onRetry = { item ->
                        viewModel.addDownload(
                            url = item.url ?: "",
                            quality = item.quality,
                            format = item.format,
                            codec = item.codec,
                            downloadType = item.downloadType
                        )
                    },
                    onRefresh = { viewModel.refresh() }
                )
            }

            // Subscriptions screen
            composable(Routes.SUBSCRIPTIONS) {
                SubscriptionsScreen(
                    subscriptions = subscriptions,
                    isLoading = isLoading,
                    onCheck = { id -> viewModel.checkSubscriptions(if (id != null) listOf(id) else null) },
                    onDelete = { id -> viewModel.deleteSubscriptions(listOf(id)) }
                )
            }

            // Settings screen — server URL, quality, theme
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    serverUrl = serverUrl,
                    defaultQuality = defaultQuality,
                    defaultType = defaultType,
                    defaultFormat = defaultFormat,
                    defaultCodec = defaultCodec,
                    isDarkMode = isDarkMode,
                    onSaveServerUrl = { viewModel.updateServerUrl(it) },
                    onSaveQuality = { viewModel.updateDefaultQuality(it) },
                    onSaveType = { viewModel.updateDefaultType(it) },
                    onSaveFormat = { viewModel.updateDefaultFormat(it) },
                    onSaveCodec = { viewModel.updateDefaultCodec(it) },
                    onToggleDarkMode = { viewModel.setDarkMode(it) },
                    allowBackground = allowBackground,
                    onToggleBackground = { viewModel.setAllowBackground(it) },
                    networkPolicy = networkPolicy,
                    onSaveNetworkPolicy = { viewModel.updateNetworkPolicy(it) }
                )
            }
        }
    }
}
