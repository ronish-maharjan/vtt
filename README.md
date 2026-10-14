# Whistle Keyboard

A minimal Android keyboard dedicated to speech-to-text. It records speech, sends a WAV file to a local Whistle transcription server running in Termux, and inserts the recognized text into the currently focused text field.

## Architecture

- **VoiceKeyboardService** - Android InputMethodService implementing the IME
- **AudioRecorder** - Records PCM 16-bit mono audio at 16kHz using AudioRecord, writes valid WAV files
- **TranscriptionClient** - HTTP client for local Whistle server (http://127.0.0.1:8765)
- **KeyboardView** - Custom view with microphone button and state indicators
- **MainActivity** - Setup/diagnostic activity for enabling keyboard and testing server

## Requirements

- Android 8.0 (API 26) or higher
- ARM64 device
- Termux with Whistle transcription server running on port 8765

## Installation

### From GitHub Actions

1. Go to the repository's **Actions** tab
2. Select the latest successful workflow run
3. Download the `whistle-keyboard-debug-apk` artifact
4. Transfer the APK to your Android device
5. Install the APK (enable "Install unknown apps" if prompted)

### Direct APK Link

After a successful build, the debug APK is available at:
```
https://github.com/<owner>/<repo>/actions/artifacts/<artifact-id>
```

Or from the Actions page: click the workflow run → Artifacts → `whistle-keyboard-debug-apk`

## Setup

### 1. Enable the Keyboard

1. Open **Settings** → **Languages & input** → **On-screen keyboard** → **Manage keyboards**
2. Turn on **Whistle Voice Keyboard**
3. Accept the warning about input method security

### 2. Select the Keyboard

1. Tap any text field to show the keyboard
2. Tap the keyboard switcher icon (usually bottom-right)
3. Select **Whistle Voice Keyboard**

### 3. Grant Microphone Permission

1. Open the **Whistle Keyboard** app from your app drawer
2. Tap **Grant Permission**
3. Allow microphone access when prompted

### 4. Start the Termux Transcription Server

In Termux, run:

```bash
cd ~/voice-typing
node server.js
```

The server runs on `http://127.0.0.1:8765`.

### 5. Test Server Connection

In the Whistle Keyboard app, tap **Test Server Connection**. You should see "Server is reachable ✓".

## Usage

1. Open any app with a text field (messages, notes, browser, etc.)
2. Select Whistle Keyboard as the input method
3. Tap the **microphone button** to start recording
4. Speak your text
5. Tap the **microphone button again** to stop recording and transcribe
6. The recognized text will be inserted into the text field

### Keyboard States

- **Idle** (gray microphone) - Ready to record
- **Recording** (red pulsing circle) - Recording audio, tap to stop
- **Processing** (blue spinning) - Transcribing, wait for result
- **Error** (red with message) - Tap to retry

### Switching Keyboards

Tap **"Switch Keyboard"** at the bottom of the keyboard view, or use the system keyboard switcher.

## Termux Server Setup

The keyboard requires a local Whistle transcription server. This project does **not** include the model or server executable.

### Prerequisites

- Termux installed from F-Droid or GitHub (not Play Store)
- Whistle model (`whistle.cact`) and `needle` executable in `~/voice-typing/`
- Node.js server script (`server.js`) in `~/voice-typing/`

### Starting the Server

```bash
cd ~/voice-typing
node server.js
```

### Health Check

```bash
curl http://127.0.0.1:8765/health
# Expected: {"status": "ok"}
```

### Transcription Test

```bash
curl -X POST http://127.0.0.1:8765/transcribe \
  --data-binary @test.wav \
  -H "Content-Type: application/octet-stream"
# Expected: {"text": "recognized speech", "language": "en", ...}
```

## Known Limitations

- **No background recording** - Recording stops when keyboard is dismissed
- **No automatic punctuation** - Text inserted exactly as returned by Whistle
- **Local network only** - Only connects to 127.0.0.1:8765, no cloud services
- **Requires Termux server** - Server must be started manually before use
- **No offline mode** - Requires the local server to be running
- **ARM64 only** - Built for arm64-v8a architecture

## Troubleshooting

### Keyboard doesn't appear in keyboard list

- Ensure the app is installed and not disabled
- Restart the device
- Check Settings → Languages & input → On-screen keyboard → Manage keyboards

### "Server unreachable" error

- Verify Termux server is running: `curl http://127.0.0.1:8765/health`
- Check Termux allows network access (not restricted by battery optimization)
- Ensure server.js is listening on 0.0.0.0:8765 or 127.0.0.1:8765

### "Permission denied" for microphone

- Go to Settings → Apps → Whistle Keyboard → Permissions → Microphone → Allow
- Reopen the Whistle Keyboard app and tap Grant Permission

### Text not inserted

- Ensure a text field is focused when transcription completes
- Some apps (WebViews, certain editors) may not support InputConnection properly
- Try a different text field

### Recording fails immediately

- Check microphone permission is granted
- Ensure no other app is using the microphone
- Restart the app

## Privacy

- **No cloud services** - All transcription happens locally on your device
- **No analytics** - No tracking, telemetry, or data collection
- **No internet access** - Only connects to localhost (127.0.0.1)
- **No persistent storage** - Audio files deleted after transcription
- **Open source** - Full source code available for audit

## Building Locally

If you want to build locally (requires Android SDK):

```bash
# Install Android SDK and set ANDROID_HOME
export ANDROID_HOME=~/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# Build
./gradlew assembleDebug

# APK location
app/build/outputs/apk/debug/app-debug.apk
```

## License

MIT License - See LICENSE file for details.

## Project Structure

```
whistle-keyboard/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/dev/whistlekeyboard/
│   │   │   ├── MainActivity.kt          # Setup/diagnostic activity
│   │   │   ├── VoiceKeyboardService.kt  # IME service
│   │   │   ├── AudioRecorder.kt         # WAV recording
│   │   │   ├── TranscriptionClient.kt   # HTTP client
│   │   │   └── KeyboardView.kt          # Custom keyboard UI
│   │   ├── res/
│   │   │   ├── layout/activity_main.xml
│   │   │   ├── xml/method.xml           # IME metadata
│   │   │   ├── xml/network_security_config.xml
│   │   │   ├── values/strings.xml
│   │   │   ├── values/styles.xml
│   │   │   └── values/colors.xml
│   │   └── build.gradle.kts
│   ├── build.gradle.kts
│   └── ...
├── .github/workflows/build.yml          # CI/CD
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── gradle/wrapper/
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make minimal, focused changes
4. Ensure all tests pass
5. Submit a pull request

Keep the codebase minimal - no unnecessary dependencies, abstractions, or features.