# MeTube Upgraded Clients - Release v2.0 (April 2026)

## 📱 MeTube Android App (v1.1.1)

### What's New
- **🔔 Runtime Permissions**: The app now requests notification permissions on the first launch (Android 13+). 
- **🚀 Real Background Persistent Sync**: Implemented a proper Android Foreground Service (`MeTubeService`). The app now stays alive and synced when minimized.
- **📟 Live Sync Notification**: A persistent notification now appears when Background Sync is active, showing real-time speeds and active counts.
- **⚡ Supercharged Synchronization**: Completely re-engineered the Socket.IO event system for maximum stability and backend alignment.
- **🛡️ Ecosystem Hardening**: senior-level audit and optimization pass (Phase 11) for full-stack stability.
- **🔄 Sync Technicals**: Fixed Android snake_case mapping and Desktop all-event pair parsing for absolute UI parity.
- **🔋 Performance & Battery**: Implemented `WakeLock` in the background service to ensure reliable long-running downloads.
- **🎨 UI Polish**: Added type-specific icons (Video, Audio, Captions) to all download lists.
- **🔄 Performance Toggle Logic**: The "Run in background" setting now has real technical logic—it handles the service lifecycle automatically.
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
- **⚡ Handshake Robustness**: Improved socket initialization by clearing session IDs on reconnect to prevent race conditions.
- **🎨 Visual Feedback**: Added a real-time connection status indicator ("Connecting...", "Connected", "Disconnected") to the status bar.
- **🛡️ Hardened Core**: Fixed WebSocket memory leaks and signal race conditions for 24/7 stability.
- **📦 Runtime Fix**: Bundled the latest MinGW-w64 runtime libraries to resolve cross-system "Entry Point Not Found" errors.

---

## 🤖 MeTube Telegram Bot (v2.1.0)

### What's New
- **💎 Multi-Feature Parity**: Support for folders, chapters, sub-processing, and server presets.
- **📟 Live Notifications**: Dynamic progress updates during active downloads.
- **🛡️ Offline Recovery**: Automated checking of the job store on reconnect to notify for completions missed during downtime.
- **⚡ Submission Debounce**: Implemented a 1-second rate limit per user for URL submissions to enhance stability.
- **📋 Management**: `/subs` and `/check` commands for robust subscription tracking.

---

### Packaging Info
- `MeTube-Upgraded.apk`: Android Production Build.
- `metube-app/`: Android Source code.
- `metube-bot/`: Telegram Bot Source code.
- `metube-desktop/`: Desktop Client Source code.

*Maintained by: iTzJok3r*
