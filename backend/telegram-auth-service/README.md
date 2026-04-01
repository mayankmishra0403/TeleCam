# Telegram Auth Service

Node.js backend for TeleCam one-tap Telegram onboarding.

## Features
- Creates short-lived auth sessions from app-generated UUID tokens
- Handles Telegram `/start auth_<token>` via Telegraf bot
- Maps `token -> telegram_user_id`
- Returns one-time verification response to Android app
- Expires and invalidates used tokens

## Setup
1. Copy `.env.example` to `.env`
2. Fill `BOT_TOKEN`, `BOT_USERNAME`
3. Install and run

```bash
npm install
npm run dev
```

## Endpoints
- `POST /api/auth/telegram/start` body `{ "token": "<uuid>" }`
- `GET /api/auth/telegram/verify?token=<uuid>`
- `GET /health`

## Telegram flow
1. Android calls `start`
2. Backend responds with `https://t.me/<bot>?start=auth_<token>`
3. User taps **Start** in bot
4. Bot sends **Continue to App** (`telecam://auth?token=<token>`)
5. Android deep-link opens and calls `verify`
