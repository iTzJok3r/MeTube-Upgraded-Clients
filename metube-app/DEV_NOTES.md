# MeTube Android App - Dev Notes

## v1.0.5 — April 2026

### What's New
- **⚡ Supercharged Synchronization**: Completely re-engineered the Socket.IO event system in the Android app. 
- **🚀 Real-Time Reliability**: Active downloads now appear instantly without needing a manual refresh.
- **🔄 Robust Reconnections**: The app now automatically recovers and reconciles its state even after long background periods or internet drops.
- **🛡️ Atomic State Guard**: Implemented thread-safe atomic updates in the ViewModel to prevent UI inconsistencies during high-frequency server updates.

### Technical Changes
- **Modified**: `SocketManager.kt` — Migrated to `StateFlow` and `flatMapLatest` to ensure event listeners survive socket instance replacement.
- **Modified**: `MeTubeViewModel.kt` — Switched to atomic `.update` for state reconciliation and fixed startup race conditions.
- **Modified**: `HistoryScreen.kt` — Verified and refined direct file download logic.

---

## v1.0.4 — April 2026

### What's New
- **🚀 Architectural Hardening**: Full senior-level audit and optimization pass to ensure production-grade stability.
- **🔄 Swipe-to-Refresh**: Added manual recovery support to Queue, History, and Subscriptions screens.
- **🔋 Performance & Battery**: Migrated to lifecycle-aware state collection (`collectAsStateWithLifecycle`) to reduce unnecessary background UI collection and conserve app resources.
- **🛡️ Thread Safety**: Re-engineered state updates with atomic operations to eliminate race conditions during high-frequency server updates.
- **🐛 Socket Synchronization Fix**: Resolved a critical bug in `SocketManager` where download status and progress were not being updated in real-time due to JSON parsing omissions.

### Technical Changes
- **Modified**: `MeTubeViewModel.kt` — Atomic state management via `.update` for list mutations; fixed lifecycle-bound socket subscriptions and improved state reconciliation for the `all` event.
- **Modified**: `MainActivity.kt` — Efficient lifecycle-bound UI state collection.
- **Modified**: `SocketManager.kt` — Fixed `DownloadItem` parsing to include all missing fields (`id`, `status`, `percent`, `speed`, `eta`) and improved `TypeToken` type safety.
- **Improved**: `QueueScreen.kt`, `HistoryScreen.kt`, `SubscriptionsScreen.kt` — Integrated `PullToRefresh` for manual error recovery and dynamic list updates.

---

## v1.0.3 — April 2026

### What's New
- **🌐 Network-Aware Downloads**: Added connectivity-risk checks to protect users from high data costs when downloading files from the server to the device.
- **🛡️ Download Policies**: New settings allow users to choose between "Always Allow", "Warn on Metered" (default), or "Unmetered Only" for server-to-device transfers.
- **📊 Connectivity Classification**: The app now uses `isActiveNetworkMetered` and Data Saver signals to assess whether a connection may be costly for large server-to-device downloads.
- **⚠️ Explanatory Alerts**: Risky downloads now trigger a confirmation dialog clarifying that data consumption occurs during the server-to-device file transfer.

### Technical Changes
- **NEW**: `NetworkUtils.kt` — Centralized network risk classification using `ConnectivityManager` and `NetworkCapabilities`.
- **Modified**: `SettingsManager.kt` — Persistent storage for `KEY_NETWORK_POLICY`.
- **Modified**: `HistoryScreen.kt` — Integrated `onDownloadClick` flow with `AlertDialog` support and policy enforcement.
- **Modified**: `SettingsScreen.kt` — New "Network & Downloads" configuration section.
- **Modified**: `MeTubeViewModel.kt` — Reactive state for network policy management.

---

## v1.0.2 — April 2026

### What's New
- **🔗 Backend Alignment**: Improved alignment with MeTube's official logic and data models.
- **📺 Subscriptions Screen**: Manage channel and playlist subscriptions directly; view, check, and delete from a dedicated tab.
- **⚡ Manual Download Start**: Support for `auto_start=false`. Pending items in the queue now feature a "Play" button to trigger the download manually.
- **📋 Server Presets**: Integration with server-side `ytdl-dlp` presets dropdown on the Home screen.
- **🛠️ Advanced Parameters**: Added support for `auto_start` and `split_by_chapters` toggles.
- **📁 Conditional Folders**: The "Folder" input only appears if the server has `CUSTOM_DIRS` enabled (synced via `configuration` event).

### Technical Changes
- **Modified**: `SocketManager.kt` — Specialized typed flows for `configuration`, `subscriptions_all`, and subscription lifecycle events.
- **Modified**: `DownloadItem.kt`, `AddRequest.kt`, `SubscriptionItem.kt` — Expanded model field coverage to better match backend `DownloadInfo` and `SubscriptionInfo` structures.
- **Modified**: `MeTubeRequestBuilder.kt` — Reconstruction of the `/add` payload including confirmed backend parameters.
- **Modified**: `MeTubeViewModel.kt` — Reconcilers for new socket events and support for manual start logic.

---

## v1.0.1 — April 2026

### What's New
- **📱 Mobile Data Warning**: A native alert dialog now appears when users tap "Add to Queue" while on mobile or metered data.
- **🗑️ Delete/Remove Actions**: Queue and completed items can now be removed directly from the app, matching the MeTube web UI behavior.
- **⚙️ Advanced Download Options**: Support for Type (Video/Audio/Captions/Thumbnail), Format (MP4/WebM/MP3/M4A/OPUS/iOS), Quality (Best/1080p/720p/480p), and Video Codec (H.264/H.265/AV1/VP9) selectors.
- **🔔 Completion Notifications**: Native Android notifications fire when a server-side download finishes.
- **🎨 Dynamic Options UI**: The Video Codec dropdown auto-hides when an audio-only type or format is selected.

### Technical Changes
- **Modified**: `HomeScreen.kt` — Mobile data detection and `AlertDialog` confirmation flow.
- **Modified**: `MeTubeViewModel.kt` — Delete action, completion notification channel, advanced options state flows.

### Build Info
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34
- **Compose BOM**: 2024.02.00
- **Kotlin Compiler Extension**: 1.5.8

---

#### v1.0.0 — Initial Release
- Web UI Parity & Advanced Options Settings.
- Persistent state via `SettingsManager` + `MeTubeViewModel`.
- Socket.IO real-time updates and full state reconciliation.
- Retrofit REST integration for `/history` and `/add` endpoints.
