# MeTube-Upgraded Clients 📱🤖

This repository contains the standalone **Android App** and **Telegram Bot** for the MeTube ecosystem. These clients are designed to work seamlessly with any [MeTube](https://github.com/alexta69/metube) backend instance.

> [!TIP]
> Use these clients to upgrade your MeTube experience with native mobile support and real-time Telegram notifications!

---

## 📱 MeTube Android App
A native Android client built with Jetpack Compose.
- **Features:** Real-time sync via Socket.IO, Share-to-download integration, and persistent settings.
- **Getting Started:**
  1. Download the latest `.apk` from the **Releases** section.
  2. Install it on your Android device.
  3. Enter your MeTube server URL in the settings.
- **Build from Source:** Open the `metube-app/` directory in Android Studio.

## 🤖 MeTube Telegram Bot
A companion bot to manage downloads and get completion alerts.
- **Features:** Automatic URL detection, `/queue` and `/done` management, and multi-user notification routing.
- **Getting Started:**
  1. Go to `metube-bot/`.
  2. Run `npm install`.
  3. Copy `.env.example` to `.env` and fill in your `BOT_TOKEN` and `ALLOWED_USER_IDS`.
  4. Run `npm start`.

### 🐳 Run Bot with Docker
If you use **Docker** or **CasaOS**:
1. Copy `.env.example` to `.env`.
2. Run `docker-compose up -d`.
3. The bot will start and stay running in the background.

---

## 🔄 Staying Updated
These clients are compatible with the latest MeTube API. Even if you update your original MeTube server to a newer version, these clients will continue to work. We periodically update this repository to match new backend features.

## 📜 Attribution
This project is an enhancement of the original **MeTube** by **alexta69**. 
- Original Backend: [alexta69/metube](https://github.com/alexta69/metube)

## 🛡️ Privacy & Security
- **No Data Collection:** This app does not collect, store, or share any personal data. It does not track your downloads or browsing history.
- **Direct Connection:** The Android App and Telegram Bot communicate **ONLY** with your own personal MeTube server. No third-party servers are involved.
- **Open Source:** Full transparency. You can audit the source code to verify its safety.
- **No Traces:** The app stores only its basic configuration settings on your device.

---

**Maintained by:** iTzJok3r
