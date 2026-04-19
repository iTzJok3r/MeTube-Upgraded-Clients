# Development Notes & Handoff

This document outlines the architectural decisions, current status, known limitations, and future roadmap for the MeTube Desktop Client. It serves as a handoff artifact for maintainers taking over the codebase.

## 🏗️ Architecture & Component Design
The application enforces a strict separation of concerns utilizing a Service-Oriented MVVM pattern:

- **Data Models (`src/core/Models.h`)**: Defines shared data structures, particularly `MeTube::DownloadItem` and status enumerations.
- **Managers & ViewModels (`src/core/DownloadManager.cpp`, `src/viewmodels/DownloadViewModel.cpp`)**: 
  - `DownloadManager` acts as the single source of truth for the application state. It orchestrates downstream REST and WebSocket services and manages data deduplication.
  - `DownloadViewModel` exposes this state reactivity to the view layer without tightly coupling widgets to services.
- **REST Layer (`src/services/MetubeRestService.cpp`)**: Executes `/api/v1/history` and `/api/v1/download` actions using `QNetworkAccessManager` with defensive JSON payload parsing.
- **Real-Time Pipeline (`src/services/MetubeSocketService.cpp`)**: Implementing a custom socket Engine.IO v4 protocol atop `QWebSocket`. This avoids heavy external dependencies while cleanly managing ping/pong heartbeats and event translation (`added`, `updated`, `completed`).

## 🖥 Native OS Integrations
- **Tray & Background Mode**: Managed by `SystemTrayManager`. Note that tray availability dynamically relies on the host OS. Applications running on diverse Linux Desktop Environments should verify `QSystemTrayIcon::isSystemTrayAvailable()` at runtime.
- **Native Notifications**: `NativeNotificationService` abstracts message delivery using OS-level popups triggered upon completed downloads or errors.
- **Clipboard Discovery**: `ClipboardMonitor` asynchronously checks the OS clipboard for known media formats and alerts the user.

## 🚧 Status: Core v1 Feature Complete
The desktop client is structurally and functionally feature-complete for Version 1. It operates flawlessly alongside a standard MeTube backend, accurately submitting advanced transcoding preferences and tracking real-time progression. 

*Note: This is "feature complete" regarding code execution, but lacks final packaging structures (like installers or AppImage generation).*

## ⚠️ Known Limitations
1. **macOS Support**: While the Qt 6 code is highly portable, full deployment on macOS requires generating an application bundle (`.app`) with correct `Info.plist` specifications. This environment scaffolding is not currently implemented in the CMake logic.
2. **Linux System Tray**: Depending on the Wayland/X11 compositors or desktop environments (GNOME, plasma), system tray icons may behave inconsistently without specific extension packages (e.g., AppIndicator).
3. **Queue Scalability**: Data synchronization in `DownloadManager` currently utilizes a brute-force iterative replacement technique. For vast histories (>1000 items), this O(N) deduplication paired with full QList UI repaints might introduce main-thread lag. Optimal scaling will require implementing a proper `QAbstractListModel` with precise `beginInsertRows`/`dataChanged` signal handling.
4. **Backend Coupling**: The client relies absolutely on the upstream server. There is currently no multi-threaded background local downloader. If the connection fails, local queue progression halts.

## 🔮 Future Roadmap
1. **List Model Refactor**: Refactor `DownloadListWidget` from using item views to leveraging a highly performant `QAbstractListModel`.
2. **Packaging**: Generate OS-specific artifacts (`.msi`/`.exe` for Windows, `.AppImage`/`.deb` for Linux, `.dmg` for macOS) using CPack.
3. **Advanced Filtering**: Introduce queue grouping, sophisticated sorting, and robust search to the History tab.
4. **Theming**: Implement a global QSS-based theme manager that queries the native OS for preferred Dark/Light mode color palettes.
