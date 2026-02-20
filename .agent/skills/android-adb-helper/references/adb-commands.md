# Android ADB & Gradle Reference

## Device Management
- List devices: `adb devices`
- List emulators: `adb devices -l` (look for `emulator-xxxx`)
- Connect to network device: `adb connect <ip>:<port>`
- Get device state: `adb get-state`
- Wait for device: `adb wait-for-device`

## App Management
- Install APK: `adb install -r <path_to_apk>`
- Uninstall app: `adb uninstall <package_name>`
- Clear app data: `adb shell pm clear <package_name>`
- List installed packages: `adb shell pm list packages`
- Get app path: `adb shell pm path <package_name>`

## Execution
- Start app: `adb shell am start -n <package_name>/<activity_name>`
- Force stop app: `adb shell am force-stop <package_name>`
- Send broadcast: `adb shell am broadcast -a <action>`

## Logging & Debugging
- View logs: `adb logcat`
- Filter logs by package: `adb logcat --pid=$(adb shell pidof -s <package_name>)`
- Clear logs: `adb logcat -c`
- Take screenshot: `adb shell screencap -p /sdcard/screen.png` && `adb pull /sdcard/screen.png`
- Record screen: `adb shell screenrecord /sdcard/demo.mp4`

## File Transfer
- Push file: `adb push <local> <remote>`
- Pull file: `adb pull <remote> <local>`

## Gradle Build Tasks
- Build debug APK: `./gradlew assembleDebug`
- Build and install debug: `./gradlew installDebug`
- Clean build: `./gradlew clean`
- Run tests: `./gradlew test`
- List all tasks: `./gradlew tasks`

## Common Package/Activity Discovery
- Current focused activity: `adb shell dumpsys window | grep -E 'mCurrentFocus|mFocusedApp'`
- Package info: `adb shell dumpsys package <package_name>`
