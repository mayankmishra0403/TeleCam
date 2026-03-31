# TeleCam

Camera app with Telegram cloud upload.

## Build Status

![Kotlin](https://img.shields.io/badge/Kotlin-1.9.20-blue)
![Compose](https://img.shields.io/badge/Compose-BOM%202023.10.01-blue)
![Android](https://img.shields.io/badge/Android-minSdk%2026-green)
![License](https://img.shields.io/badge/License-MIT-green)

## Quick Start

```bash
# Clone the repository
git clone <repo-url>
cd TeleCam

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

## Configuration

1. Create a Telegram Bot via @BotFather
2. Get your Chat ID from @userinfobot
3. Enter credentials in app Settings

## Features

- Photo and video capture
- Automatic Telegram upload
- Offline queue with background sync
- WiFi-only upload option
- Clean Architecture + MVVM

## Tech Stack

| Component | Technology |
|-----------|------------|
| UI | Jetpack Compose |
| Camera | CameraX |
| Database | Room |
| Network | Retrofit + OkHttp |
| Background | WorkManager |
| DI | Hilt |

## Project Structure

```
app/src/main/java/com/telecam/
├── di/           # Dependency injection
├── ui/           # Compose screens
├── camera/       # CameraX implementation
├── data/         # Repository, Room, Retrofit
├── domain/       # Use cases, models
├── sync/         # WorkManager workers
└── utils/        # Utilities
```

## API Reference

### Telegram Bot API

Base URL: `https://api.telegram.org/`

Endpoints used:
- `POST /bot{token}/sendPhoto`
- `POST /bot{token}/sendVideo`
- `POST /bot{token}/sendDocument`
- `POST /bot{token}/getFile`

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## License

MIT
