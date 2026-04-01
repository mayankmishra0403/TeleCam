# TeleCam Backend - Deployment Guide (Railway + Free 24/7 Hosting)

## Overview
- **Platform**: Railway.app (free tier includes $5/month credits + 100GB outbound)
- **Cost**: $0-5/month (free credits typically cover small projects)
- **Uptime**: 24/7 (no sleep mode like Render free tier)
- **Status**: Perfect for production Telegram bot backend

---

## Setup Steps

### 1. Prerequisites
- GitHub account (to connect Railway)
- Telegram bot already created with BotFather (have bot token ready)

### 2. Create Railway Account & Deploy

**Step-by-step:**

1. Go to [railway.app](https://railway.app)
2. Sign up with GitHub (easiest method)
3. Click **"Create New Project"** → **"Deploy from GitHub"**
4. Select this repository: `mayankmishra0403/TeleCam`
5. Railway will detect `package.json` and auto-configure Node.js
6. Select `/backend/telegram-auth-service` as the root directory (or skip if auto-detected)

### 3. Configure Environment Variables

In Railway dashboard:
1. Go to **"Variables"** tab
2. Add these secrets:

```
BOT_TOKEN=<your_actual_telegram_bot_token_from_botfather>
BOT_USERNAME=@tele_ca_m_bot
BASE_APP_DEEP_LINK=telecam://auth
APP_PUBLIC_BASE_URL=<railway_generated_url>
AUTH_TOKEN_TTL_MS=600000
PORT=8080
```

**Important**: 
- `BOT_TOKEN`: Never commit this. Railway stores it securely as a secret.
- `APP_PUBLIC_BASE_URL`: Railway will auto-generate a URL like `https://telecam-backend-prod.railway.app`. Update this after deployment.

### 4. Deploy

1. Railway auto-deploys when you push to `main`
2. Or manually trigger via **"Deploy"** button in dashboard
3. Check **"Deployments"** tab for status

### 5. Get Public URL

After successful deployment:
1. Go to **"Settings"** → **"Public Networking"**
2. Railway generates public URL (e.g., `https://telecam-backend-prod.railway.app`)
3. Update `APP_PUBLIC_BASE_URL` environment variable with this URL
4. App will automatically redeployify

---

## Security Best Practices

### A. Token Management (CRITICAL)

**DO:**
✅ Store bot token only in Railway **Secrets** (encrypted at rest)
✅ Rotate token every 90 days via BotFather
✅ Use `System.getenv("BOT_TOKEN")` to read at runtime (never hardcode)
✅ Keep `.env.example` with placeholder values only

**DON'T:**
❌ Never commit real secrets to git (even in `.env` files)
❌ Never hardcode token in source code
❌ Never share token in Slack/Discord/emails
❌ Never push secrets to public repos

### B. Automated Token Rotation (Optional)

Create a reminder script (not auto-rotating for safety):

```bash
# token-rotation-reminder.sh
#!/bin/bash
echo "Reminder: Rotate BOT_TOKEN in BotFather every 90 days"
echo "Current date: $(date)"
echo "Next rotation: $(date -d '+90 days' +%Y-%m-%d)"
```

Run monthly:
```bash
crontab -e
# Add: 0 0 1 * * ~/token-rotation-reminder.sh
```

### C. Local Development

**Setup local .env.local (never commit):**

```bash
# backend/telegram-auth-service/.env.local
BOT_TOKEN=<test_token_from_botfather>
BOT_USERNAME=@tele_ca_m_bot
BASE_APP_DEEP_LINK=telecam://auth
APP_PUBLIC_BASE_URL=http://localhost:8080
AUTH_TOKEN_TTL_MS=600000
PORT=8080
```

Add to `.gitignore`:
```
**/.env.local
**/.env.*.local
```

---

## Monitoring & Debugging

### 1. View Logs

Railway dashboard → **"Logs"** tab:
- Real-time request logs
- Error traces
- Bot activity

### 2. Check Bot Health

```bash
curl https://api.telegram.org/bot<BOT_TOKEN>/getMe
```

Response should show bot info if token is valid.

### 3. Test Auth Flow

```bash
# Start auth session
curl -X POST https://<railway_url>/api/auth/telegram/start \
  -H "Content-Type: application/json" \
  -d '{"token":"550e8400-e29b-41d4-a716-446655440000"}'

# Verify token (before user confirms)
curl "https://<railway_url>/api/auth/telegram/verify?token=550e8400-e29b-41d4-a716-446655440000"
```

---

## Alternative Free Options (Comparison)

| Platform | Cost | Uptime | Sleep? | Scale |
|----------|------|--------|--------|-------|
| **Railway** | $0-5/mo | 24/7 | ❌ No | ⭐⭐⭐ Medium |
| Render | Free | 24/7* | ⚠️ Yes (15min idle) | ⭐⭐ Small |
| Vercel | Free | 24/7 | ❌ No | ⭐ Serverless only |
| Heroku | Paid | - | ❌ (deprecated free) | ❌ No |
| Oracle Cloud | Free | 24/7 | ❌ No (if eligible) | ⭐⭐⭐⭐ Large |

**Recommendation**: Railway (best balance) or Oracle Cloud (if you have account).

---

## Update App to Use Railway Backend

### Android App (`app/build.gradle.kts`)

```kotlin
val telegramBotToken = (project.findProperty("TELEGRAM_BOT_TOKEN") as String?)
    ?: System.getenv("TELEGRAM_BOT_TOKEN") ?: ""

buildConfigField("String", "TELEGRAM_AUTH_BASE_URL", 
    "\"https://<railway_url>/\"")  // Update this
buildConfigField("String", "TELEGRAM_BOT_TOKEN", "\"$telegramBotToken\"")
```

### Local Build

```bash
export TELEGRAM_BOT_TOKEN="your_test_token_here"
cd app
./gradlew installDebug
```

---

## Troubleshooting

### Issue: "BOT_TOKEN not found"
**Solution**: Add `BOT_TOKEN` to Railway **Variables** tab (not .env file)

### Issue: "Unauthorized bot" errors
**Solution**: 
1. Verify token is valid: `curl https://api.telegram.org/bot<TOKEN>/getMe`
2. Check token hasn't rotated in BotFather
3. Restart Railway deployment (redeploy in dashboard)

### Issue: Deep link not working
**Solution**: Ensure `BASE_APP_DEEP_LINK` matches your app's manifest deep link scheme

### Issue: "Connection refused" from app
**Solution**: 
1. Use Railway public URL, not `localhost`
2. Verify `APP_PUBLIC_BASE_URL` is correctly set
3. Check Railway deployment is "Running" (green status)

---

## Git Hygiene (POST-SECURITY-FIX)

### Current Status
✅ Working tree: Clean (no hardcoded secrets)
⚠️ Git history: Old commits still contain token

### Cleanup (If Needed)

```bash
# View commits with token
git grep "8519978215:AAG" $(git rev-list --all)

# Rewrite history (DANGEROUS - requires force push)
git filter-repo --replace-all --values 8519978215:AAG REDACTED

# Force push (⚠️ all team members must re-clone)
git push --force-with-lease
```

**Recommendation**: If repo is private, history cleanup is optional. If public, rotate token immediately (already done).

---

## Deployment Checklist

- [ ] Railway account created
- [ ] Repository connected to Railway
- [ ] `BOT_TOKEN` added to Railway Secrets
- [ ] Other env vars added (`BOT_USERNAME`, `BASE_APP_DEEP_LINK`, etc.)
- [ ] Deployment successful (green status)
- [ ] Public URL generated
- [ ] App `TELEGRAM_AUTH_BASE_URL` updated to Railway URL
- [ ] Test auth flow end-to-end
- [ ] Monitor logs for errors
- [ ] Calendar reminder set for token rotation (90 days)

---

## Next Steps

1. **Deploy now**: Push to `main`, Railway auto-deploys
2. **Test**: Verify bot responds in Telegram
3. **Update app**: Change backend URL in Android code
4. **Monitor**: Check Railway logs for issues
5. **Rotate token**: Set 90-day reminder in BotFather

---

**Questions?**
- Railway docs: https://docs.railway.app
- Telegram Bot API: https://core.telegram.org/bots/api
- Project README: `./README.md`
