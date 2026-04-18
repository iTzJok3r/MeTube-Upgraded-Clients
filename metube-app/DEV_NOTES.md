# MeTube Android App - Dev Notes

## Recent Updates: Web UI Parity & Advanced Options Settings
To accurately match the robust configurability of the MeTube web client, we implemented a complete options propagation tree throughout the Android client. 
- **Type, Format, Codec Selectors**: Three synchronized dropdowns are exposed in the HomeScreen. The exact variables mapped are natively processed by `app/main.py`.
- **Persistent State**: Default selections (e.g. pinned formats) are stored safely globally using `MeTubeViewModel` and rendered immediately upon the app launching on the `SettingsScreen`.
- **Dynamic Content Visibility**: Strict constraints ensure that nonsensical options cannot be pushed. For instance, `AnimatedVisibility` logic safely unmounts the `Video Codec` dropdown from the ViewTree completely whenever an Audio-Only condition is detected (selected Type = Audio or Format in `mp3, m4a, etc.`). 

### Dev Notes (Stopping Point)
- **Current Status:** All mapping data code, Retrofit schemas, ViewModels, and Android Jetpack Compose UX constraints compiled perfectly.
- **Unresolved actions before next step:** The debug APK building the `AnimatedVisibility` dynamic layout patch finished compiling, but we could not inject (`adb install`) the APK over the bridge onto the VM because the LDPlayer environment disconnected/timed out gracefully (`adb.exe: no devices/emulators found`). 
- **Recommendation on resume:** Launch LDPlayer, ensure `adb devices` is healthy, then simply issue the `adb install -r -d "app/build/outputs/apk/debug/app-debug.apk"` payload. Then proceed sequentially to the Node.js/Telegram layer.
