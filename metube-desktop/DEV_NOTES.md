# MeTube Desktop Upgraded - Dev Notes

## v1.0.0 — April 2026

### What's New
- **🌐 Robust Proxy-Aware Connectivity**: Completely overhauled the Socket.IO handshake to support environments behind Nginx Proxy Manager and Cloudflare Tunnels. 
  - Implemented HTTP Polling handshake to retrieve Session ID (SID) and affinity cookies.
  - Aligned headers (`Host`, `Origin`, `User-Agent`) and proxy settings to match the successful Android client behavior.
- **🔔 System Tray & Notifications**:
  - New minimized-to-tray capability with "Show/Exit" menu.
  - Native Windows notifications when downloads complete or errors occur.
- **🎨 Global UI Alignment**:
  - The application icon is now applied globally to the taskbar, tray, and all window titles.
  - **Download Options**: Aligned with the Web UI — supports Video, Audio, Thumbnails, and Captions.
- **📋 Clipboard Monitoring**: Automatically detects MeTube-compatible URLs in your clipboard for quick downloading.

### Technical Implementation
- **MetubeSocketService**: Implements a two-step handshake (Polling -> WebSocket upgrade) with automatic cookie propagation for session affinity.
- **SystemTrayManager**: Manages the `QSystemTrayIcon` and background lifecycle.
- **NativeNotificationService**: Bridges the `DownloadManager` events to the OS notification system.
- **HomeScreen**: Added "thumbnail" and "subtitle" types to the `m_typeCombo`.

### Build & Deploy
- **Compiler**: MinGW 8.1.0 64-bit
- **Qt Version**: 6.7.2
- **Deployment**: `windeployqt` with manual DLL inclusion for standard C++ libraries (`libstdc++-6.dll`, etc.).
