---
name: android-adb-helper
description: Manage Android emulators and devices via adb and gradle. Use this skill to list devices, install APKs, run/stop apps, read logcat, and build projects without Android Studio.
---

# Android Adb Helper

## Overview
This skill provides a set of commands and workflows to manage Android development tasks directly from the CLI. It covers building, installing, running, and debugging Android applications.

## Quick Start
- **List devices**: `adb devices`
- **Build & Install**: `./gradlew installDebug`
- **Start App**: `adb shell am start -n com.ipanda.android/com.ipanda.android.MainActivity`
- **Read Logs**: `adb logcat --pid=$(adb shell pidof -s com.ipanda.android)`

## Common Workflows

### 1. Build and Run
To build the app and run it on the first available device:
1. Ensure a device is connected: `adb devices`
2. Build and install: `./gradlew installDebug`
3. Launch the main activity:
   ```bash
   adb shell am start -n com.ipanda.android/com.ipanda.android.MainActivity
   ```

### 2. Debugging with Logcat
To see logs only for the current app:
```bash
adb logcat -v time --pid=$(adb shell pidof -s com.ipanda.android)
```
To clear logs before starting a new session:
```bash
adb logcat -c
```

### 3. App Lifecycle Management
- **Force Stop**: `adb shell am force-stop com.ipanda.android`
- **Clear Data**: `adb shell pm clear com.ipanda.android`
- **Uninstall**: `adb uninstall com.ipanda.android`

### 4. Emulator Management
If no device is connected, start an emulator (requires Android SDK):
```bash
emulator -list-avds
emulator -avd <avd_name>
```

## References
For a complete list of commands, see [references/adb-commands.md](references/adb-commands.md).

## Troubleshooting
- **Device not found**: Run `adb kill-server && adb start-server`.
- **Multiple devices**: Use `-s <serial_number>` with adb commands, e.g., `adb -s emulator-5554 shell ...`.
- **Permission denied**: Ensure `gradlew` is executable: `chmod +x gradlew`.
