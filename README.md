# TeleCam - Camera App with Telegram Cloud Upload

A minimal Android camera app that captures photos/videos and automatically uploads them to Telegram cloud instead of storing locally.

## Features

- **Camera capture**: Photo and video capture using CameraX
- **Offline-first system**: 
  - If internet available → upload immediately to Telegram
  - If offline → store file in local queue
- **Auto-sync**: Background worker uploads pending files when internet is back
- **Telegram integration**: Upload files to your Telegram Bot or channel

## Architecture

```
TeleCam/
├── app/src/main/java/com/telecam/
│   ├── TeleCamApplication.kt      # Application class with Hilt
│   ├── di/                         # Dependency Injection modules
│   │   ├── DatabaseModule.kt       # Room database
│   │   ├── NetworkModule.kt        # Retrofit/OkHttp
│   │   └── RepositoryModule.kt     # Repository bindings
│   ├── ui/                         # Jetpack Compose UI
│   │   ├── MainActivity.kt         # Main activity
│   │   ├── TeleCamApp.kt          # Navigation
│   │   ├── QueueScreen.kt         # Upload queue UI
│   │   ├── camera/                # Camera screen
│   │   ├── settings/              # Settings screen
│   │   └── theme/                 # Material theme
│   ├── camera/                    # CameraX implementation
│   │   └── CameraManager.kt       # Camera manager
│   ├── data/                      # Data layer
│   │   ├── local/                # Room database
│   │   │   ├── dao/              # Data Access Objects
│   │   │   └── entity/           # Database entities
│   │   ├── remote/               # Telegram API service
│   │   └── repository/           # Repository implementations
│   ├── domain/                    # Domain layer
│   │   ├── model/               # Domain models
│   │   └── usecase/             # Use cases
│   ├── sync/                     # WorkManager
│   │   ├── SyncWorker.kt        # Background sync worker
│   │   ├── SyncManager.kt      # Sync coordinator
│   │   └── BootReceiver.kt     # Boot receiver
│   └── utils/                    # Utilities
│       ├── NetworkMonitor.kt    # Network connectivity
│       └── FileManager.kt       # File operations
```

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Camera**: CameraX
- **Background tasks**: WorkManager
- **Local DB**: Room
- **Networking**: Retrofit/OkHttp
- **Architecture**: Clean Architecture + MVVM
- **DI**: Hilt

## Data Models

### UploadQueueItem
- `id`: Int - Primary key
- `filePath`: String - Local file path
- `status`: UploadStatus - PENDING, UPLOADING, UPLOADED, FAILED
- `retryCount`: Int - Number of upload attempts
- `createdAt`: Long - Timestamp
- `fileName`: String - Original filename
- `fileSize`: Long - File size in bytes
- `mimeType`: String - File MIME type
- `errorMessage`: String? - Error description if failed
- `telegramFileId`: String? - Telegram file ID after upload

## Setup

### 1. Create a Telegram Bot

1. Open Telegram and search for @BotFather
2. Send `/newbot` to create a new bot
3. Copy the bot token

### 2. Get Your Chat ID

1. Start a chat with your bot
2. Send any message to the bot
3. Visit `https://api.telegram.org/bot<YOUR_TOKEN>/getUpdates`
4. Find your `chat.id` in the response

### 3. Configure the App

1. Open the app
2. Go to Settings
3. Enter your Bot Token
4. Enter your Chat ID
5. Enable Auto Upload

## Functional Flow

1. User captures media (photo/video)
2. `CameraManager` handles camera operations
3. `UploadFileUseCase.processMediaCapture()` decides:
   - If online (and not wifi-only or on wifi) → upload immediately
   - If offline or wifi-only on mobile → add to queue
4. `SyncWorker` runs periodically to process queue
5. `TelegramRepository` sends files via Bot API
6. `UploadQueueRepository` updates status in Room DB

## Background Sync

The `SyncWorker`:
- Runs every 15 minutes (configurable)
- Only runs when network is available
- Respects WiFi-only setting
- Processes up to 10 items per sync cycle
- Retries failed items up to max retries

## Building

```bash
./gradlew assembleDebug
```

## Requirements

- Android 8.0+ (API 26+)
- Camera permission
- Internet permission
- Storage permissions (for older Android versions)

## Permissions

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
```

## License

MIT License
