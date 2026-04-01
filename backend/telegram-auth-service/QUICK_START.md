# 🚀 TeleCam Backend - Quick Start (Railway + 24/7 Free Hosting)

**Goal**: Deploy backend to Railway.app (24/7 free tier) with secure token management

---

## ⚡ 5-Minute Setup

### Step 1: Prepare Local Environment
```bash
cd backend/telegram-auth-service

# Make token manager executable
chmod +x token-manager.sh

# Setup local .env.local with your token
bash token-manager.sh setup
# → Paste your BotFather token when prompted
```

### Step 2: Connect to Railway
```bash
# Go to railway.app → Sign up with GitHub
# Create new project → Deploy from GitHub
# Select: mayankmishra0403/TeleCam repository
```

### Step 3: Configure Railway Secrets
In Railway dashboard, go to **Variables** → Add:
```
BOT_TOKEN=<paste_your_token>
BOT_USERNAME=@tele_ca_m_bot
BASE_APP_DEEP_LINK=telecam://auth
APP_PUBLIC_BASE_URL=<railway_will_generate_this>
AUTH_TOKEN_TTL_MS=600000
PORT=8080
```

### Step 4: Deploy & Test
```bash
# Railway auto-deploys when you push to main
git add backend/
git commit -m "feat: railway deployment config"
git push origin main

# Wait ~2 minutes for Railway to build & deploy
# Check status at: railway.app/dashboard → deployments
```

### Step 5: Get Public URL & Update App
After deployment succeeds:
1. Railway dashboard → Settings → Public Networking → Copy URL
2. Update app config:
```kotlin
// app/build.gradle.kts
buildConfigField("String", "TELEGRAM_AUTH_BASE_URL", 
    "\"https://your-railway-url/\"")
```

---

## 🔐 Token Security (Critical)

### ✅ DO THIS
- [ ] Token stored ONLY in Railway **Secrets** (encrypted)
- [ ] `.env.local` used for local development (in .gitignore)
- [ ] `.env.example` has placeholder only (safe to commit)
- [ ] Token read at runtime: `process.env.BOT_TOKEN`
- [ ] Never hardcode token in source code

### ❌ DON'T DO THIS
- ❌ Commit `.env` files with real tokens
- ❌ Hardcode token in JavaScript/Kotlin
- ❌ Share token in Slack/Discord/GitHub Issues
- ❌ Use same token for dev + production

### 🔄 Token Rotation (Every 90 Days)

```bash
# Check current token status
bash token-manager.sh status

# When ready to rotate:
bash token-manager.sh rotate
# → Follow BotFather steps
# → New token saved to .env.local
# → Then update Railway dashboard

# Verify new token works
bash token-manager.sh check
```

---

## 💰 Cost Breakdown

| Item | Cost | Notes |
|------|------|-------|
| Railway | $0 | $5/month free credits + 100GB outbound |
| Telegram Bot | Free | Unlimited API calls |
| TeleCam App | Free | Your app |
| **Total** | **$0** | Uses free Railway credits |

---

## 📊 What Railway Gives You (Free Tier)

✅ **24/7 uptime** (no sleep mode)
✅ **$5/month credits** (usually enough for small bot)
✅ **100GB outbound** data per month
✅ **Automatic HTTPS** with SSL certificate
✅ **GitHub auto-deploy** (push to main = auto-deploy)
✅ **Environment secrets** (encrypted storage)
✅ **Real-time logs** (debugging)
✅ **Health checks** (auto-restart on crash)

---

## 📋 File Structure

```
backend/
├── telegram-auth-service/
│   ├── src/
│   │   └── index.js                 # Main server (with health check)
│   ├── .env.example                 # Template (TRACK THIS)
│   ├── .env.local                   # Your token (NEVER COMMIT)
│   ├── token-manager.sh             # Token helper script
│   ├── railway.json                 # Railway config
│   ├── DEPLOYMENT.md                # Full guide
│   ├── QUICK_START.md               # This file
│   └── package.json                 # Node dependencies
```

---

## 🧪 Testing Endpoints

### Health Check
```bash
curl https://your-railway-url/api/health
# Response: {"ok":true}
```

### Start Auth Session
```bash
curl -X POST https://your-railway-url/api/auth/telegram/start \
  -H "Content-Type: application/json" \
  -d '{"token":"550e8400-e29b-41d4-a716-446655440000"}'
```

### Verify Token
```bash
curl "https://your-railway-url/api/auth/telegram/verify?token=550e8400-e29b-41d4-a716-446655440000"
```

---

## 🆘 Troubleshooting

### "BOT_TOKEN environment variable not found"
**Fix**: Add `BOT_TOKEN` to Railway **Variables** (not .env file in repo)

### "Unauthorized: Bot token was revoked"
**Fix**: 
1. Go to BotFather in Telegram
2. Send `/token` → Select your bot
3. Copy NEW token
4. Update Railway Variables with new token
5. Redeploy

### "Connection refused" from app
**Fix**: 
1. Use Railway public URL (not localhost)
2. Ensure `APP_PUBLIC_BASE_URL` matches
3. Check Railway deployment shows "Running" (green)

### "Deploy keeps failing"
**Fix**:
1. Check Railway logs (Deployments tab)
2. Ensure `package.json` exists
3. Verify Node.js version is 16+ (`node --version`)
4. Check for syntax errors: `npm run dev`

---

## 🚦 Deployment Checklist

Before pushing to main:
- [ ] `.env.local` created with valid bot token
- [ ] `.env.local` is in .gitignore (verify: `git check-ignore .env.local`)
- [ ] Token validated locally (`bash token-manager.sh check`)
- [ ] Railway account created (railway.app)
- [ ] Repository connected to Railway
- [ ] All env vars added to Railway dashboard
- [ ] Backend runs locally: `npm run start`
- [ ] App config updated with Railway URL

After first deploy:
- [ ] Railway shows "Running" (green status)
- [ ] Public URL accessible in browser
- [ ] Health check works: `curl https://url/api/health`
- [ ] Telegram bot responds in app auth flow
- [ ] Logs show no errors

---

## 📞 Support

- **Railway docs**: https://docs.railway.app
- **Telegram Bot API**: https://core.telegram.org/bots/api
- **Project README**: `../../README.md`
- **Full deployment guide**: `DEPLOYMENT.md`

---

**Ready to deploy? Let's go! 🎉**

```bash
bash token-manager.sh setup    # Local setup
git add .
git commit -m "setup: railway deployment"
git push origin main           # Auto-deploys to Railway
```

