# MeTube Desktop

A native, cross-platform desktop client for the [MeTube](https://github.com/alexta69/metube) backend.

This application provides a seamless, desktop-native interface for managing YouTube and general media downloads. It communicates directly with your self-hosted MeTube instance via REST API and real-time Socket.IO events, ensuring you have immediate visibility and control over your download queue.

## Features

- **Full Queue & History Management**: View and monitor your ongoing, queued, and completed downloads.
- **Advanced Download Controls**: Configure download type (video/audio), quality, format, and codec. Includes presets for iOS-compatibility and High-Efficiency (H.265) encoding.
- **Native Experience**: Minimizes to the system tray for background execution, issues OS-level notifications on completion, and provides native "Reveal in Folder" actions.
- **Smart Clipboard Detection**: Monitors your clipboard for YouTube/media URLs, allowing for rapid downloading.
- **Real-Time Sync**: Utilizes a custom Socket.IO-over-WebSockets implementation for instantaneous UI updates without polling overhead.

## Architecture & Technology

- **Language**: C++17
- **Framework**: Qt 6.x (Core, Gui, Widgets, Network, WebSockets)
- **Design Pattern**: Service-oriented MVVM (Model-View-ViewModel)

## Documentation

For instructions on building from source, please consult [BUILDING.md](BUILDING.md).

For detailed architectural notes, project handoff information, and the development roadmap, consult [HANDOFF.md](HANDOFF.md).
