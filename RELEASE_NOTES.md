# MeTube Upgraded Clients - Release v2.0 (April 2026)

## 📱 MeTube Android App (v1.1.1)

### What's New
- **🔔 Runtime Permissions**: The app now requests notification permissions on the first launch (Android 13+). 
- **🚀 Real Background Persistent Sync**: Implemented a proper Android Foreground Service (`MeTubeService`). The app now stays alive and synced when minimized.
- **📟 Live Sync Notification**: A persistent notification now appears when Background Sync is active.
- **⚡ Supercharged Synchronization**: Completely re-engineered the Socket.IO event system for maximum stability.
- **🔄 Performance Toggle Logic**: The "Run in background" setting now has real technical logic—it handles the service lifecycle automatically.
- **🚀 Architectural Hardening**: Full senior-level audit, thread-safety pass, and optimization for production stability.
- **🔄 Swipe-to-Refresh**: Manual recovery support added to Queue, History, and Subscriptions screens.
- **🔋 Lifecycle Optimization**: Migrated to `collectAsStateWithLifecycle` to conserve device resources and battery.
- **📺 Subscriptions**: Full in-app management of channel and playlist trackers.
- **⚙️ Advanced Controls**: Support for custom types, formats, qualities, codecs, and chapter splitting.

---

## April 2026 — v1.0.5 (Android Regression Fix)

### Android App
- **🔧 Queue Synchronization Fix**: Corrected the Socket.IO reconciliation logic to handle paired payloads from the backend. Real-time items now populate correctly even after reconnections.
- **🔄 Refresh Loop Fix**: Resolved an infinite spinner loop in the Queue screen by correctly ending the refresh operation when loading completes.
- **⚡ Supercharged Synchronization**: Completely re-engineered the Socket.IO event system for maximum stability.

### Desktop App
- **🔄 Sync Hardening**: Completed synchronization pass to ensure full parity with Web UI.
- **📦 Portable Build**: Standalone Windows executable included in this release.

---

## 💻 MeTube Desktop Native (v1.1.0)

### What's New
- **📊 Initial State Sync**: Fixed payload remapping for the `all` event. Active downloads now populate immediately on app start.
- **⚡ Batch Operations**: Mass add URLs via **Batch Import** and save your history via **Batch Export** (.txt).
- **🎨 Native Experience**: Modernized UI with system tray integration, dynamic notifications, and connection status indicators.
- **🔗 Intelligent Clipboard**: Automatic detection of video URLs for rapid downloading.
- **🛡️ Hardened Core**: Fixed WebSocket memory leaks and signal race conditions for 24/7 stability.

---

## 🤖 MeTube Telegram Bot (v2.1.0)

### What's New
- **💎 Multi-Feature Parity**: Support for folders, chapters, sub-processing, and server presets.
- **📟 Live Notifications**: Dynamic progress updates during active downloads.
- **🍪 Authentication**: New `/cookies` command to manage server-side session cookies.
- **📋 Management**: `/subs` and `/check` commands for robust subscription tracking.

---

### Packaging Info
- `MeTube-Upgraded.apk`: Android Production Build.
- `metube-app/`: Android Source code.
- `metube-bot/`: Telegram Bot Source code.
- `metube-desktop/`: Desktop Client Source code.

*Maintained by: iTzJok3r*
