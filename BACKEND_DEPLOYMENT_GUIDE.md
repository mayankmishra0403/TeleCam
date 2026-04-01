# 🚀 Backend 24/7 Free Hosting Complete Guide

## 📍 Current Status
✅ Backend code ready for deployment
✅ Railway config added (`railway.json`)
✅ Secure token management setup
✅ All files committed & pushed to `main`

---

## 🎯 Your Exact Next Steps (Copy-Paste)

### Step 1️⃣: Create Railway Account (3 minutes)
```
1. Go to https://railway.app
2. Click "Start For Free" (top right)
3. Sign in with GitHub
   - Authorize Railway to access your repos
4. Done! You have Railway account
```

### Step 2️⃣: Deploy Repository (2 minutes)
```
1. In Railway dashboard, click "Create New Project"
2. Select "Deploy from GitHub"
3. Search for: mayankmishra0403/TeleCam
4. Click "Deploy Now"
5. Railway will auto-build and start deploying
   - This takes ~2-3 minutes
   - Check progress in "Deployments" tab
```

### Step 3️⃣: Add Secret Environment Variables (3 minutes)
```
While Railway is deploying, go to:
  Dashboard → telegram-auth-service → Variables

ADD THESE (exactly):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Key                    Value
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
PORT                   8080
HOST                   0.0.0.0
NODE_ENV               production
BOT_TOKEN              <YOUR_TOKEN_HERE>
BOT_USERNAME           @tele_ca_m_bot
BASE_APP_DEEP_LINK     telecam://auth
AUTH_TOKEN_TTL_MS      600000
APP_PUBLIC_BASE_URL    (leave empty for now)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

⚠️ CRITICAL: BOT_TOKEN value:
   - Get from BotFather in Telegram
   - Chat: @BotFather
   - Send: /token
   - Select: @tele_ca_m_bot
   - Copy the token (looks like: 123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11)
```

### Step 4️⃣: Get Public URL (2 minutes)
```
Wait for deployment to finish (green ✓ status)

Then:
  Dashboard → Settings → Public Networking
  
You'll see: https://telecam-backend-prod-xxxx.railway.app

Copy this URL!
```

### Step 5️⃣: Update APP_PUBLIC_BASE_URL (1 minute)
```
Go back to Variables tab

Update: APP_PUBLIC_BASE_URL = https://telecam-backend-prod-xxxx.railway.app

Save → Railway auto-redeploys
```

### Step 6️⃣: Update TeleCam Android App (3 minutes)
```
File: app/build.gradle.kts

Find this line (around line 23):
  buildConfigField("String", "TELEGRAM_AUTH_BASE_URL", 
      "\"http://192.168.1.45:8080/\"")

Change to:
  buildConfigField("String", "TELEGRAM_AUTH_BASE_URL", 
      "\"https://telecam-backend-prod-xxxx.railway.app/\"")

Then:
  git add app/build.gradle.kts
  git commit -m "update: backend URL to railway deployment"
  git push origin main
```

### Step 7️⃣: Test Everything (2 minutes)
```
✓ Health check:
  curl https://your-railway-url/api/health
  Should show: {"ok":true}

✓ Build and run app:
  ./gradlew installDebug
  adb shell am start -n com.telecam/.MainActivity

✓ Test auth flow in app:
  1. Open TeleCam app
  2. Tap "Connect with Telegram"
  3. Should open Telegram bot
  4. Tap "Start"
  5. Should redirect back to app
  6. Login complete! ✓
```

---

## 🔐 Token Security - Read This!

### What is BOT_TOKEN?
- Telegram bot API key
- Like a password for your bot
- Should NEVER be in git history
- Already leaked once (fixed now)

### Where to Store?
```
❌ DON'T:  Commit to .env or code
✅ DO:     Store ONLY in Railway Secrets (encrypted)
✅ DO:     Use .env.local for local dev (in .gitignore)
```

### Local Development Setup
```bash
cd backend/telegram-auth-service

# Make script executable
chmod +x token-manager.sh

# Setup local .env.local
bash token-manager.sh setup
# → Paste your test bot token

# Verify it works
bash token-manager.sh check
```

### Token Rotation (Every 90 Days)
```bash
# When you need new token:
bash token-manager.sh rotate

# → Follow BotFather steps
# → Run: bash token-manager.sh check
# → Update Railway Variables with new token
# → Done!
```

---

## 💰 Cost Breakdown

### What You Get (FREE)
✅ Backend hosted 24/7
✅ $5/month free credits on Railway
✅ 100GB outbound data/month
✅ Auto-HTTPS with SSL
✅ Auto-deploy on every git push
✅ Unlimited API calls (Telegram is free)
✅ Real-time logs & monitoring

### After Free Credits (If Scales)
- **Expected cost**: $0-5/month (most small bots are free)
- **Upgrade only if**: 10K+ daily active users or high traffic
- **Can disable**: Railway → Settings → auto-pause

### Total Cost for This Project
💵 **$0** (covered by Railway free credits)

---

## 📊 What Railway Gives You vs Competitors

| Feature | Railway | Render | Heroku | Oracle Cloud |
|---------|---------|--------|--------|--------------|
| **Free Tier** | $5/mo credits | Limited | ❌ Paid only | Free (limited) |
| **Uptime** | 24/7 | 24/7* (sleeps) | - | 24/7 |
| **Auto-deploy** | ✅ GitHub | ✅ GitHub | - | Manual |
| **HTTPS** | ✅ Auto | ✅ Auto | - | ✅ Auto |
| **Secrets** | ✅ Encrypted | ✅ Encrypted | - | ✅ Encrypted |
| **Ease of use** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | - | ⭐⭐ (complex) |
| **Best for** | Small/Medium | Small | - | Large/Enterprise |

**Recommendation**: Railway is perfect for TeleCam

---

## ❓ FAQ

**Q: Will backend go to sleep?**
A: No! Railway doesn't sleep. Runs 24/7 (unlike Render free tier)

**Q: What if I exceed $5/month?**
A: You set a limit. Railway stops when limit reached (won't charge more)

**Q: Can I use same token for dev + prod?**
A: Yes, but safer to have separate test bot for local dev

**Q: What if bot token is leaked again?**
A: Just rotate it in BotFather, update Railway Variables, done!

**Q: How do I monitor if backend is down?**
A: Railway dashboard shows status + logs real-time

**Q: Can I scale later?**
A: Yes! Railway auto-scales. Just pay as needed

---

## 🚨 If Something Goes Wrong

### "BOT_TOKEN environment variable not found"
```
Fix: Go to Railway Variables tab → Add BOT_TOKEN
```

### "Connection refused" from app
```
Fix: 
1. Check Railway deployment shows "Running" (green ✓)
2. Use Railway public URL (not localhost)
3. Ensure APP_PUBLIC_BASE_URL matches in Variables
```

### "Unauthorized bot" error
```
Fix:
1. Verify token with: curl https://api.telegram.org/bot<TOKEN>/getMe
2. If 404, token might be expired/revoked
3. Generate new token in BotFather: /token
4. Update Railway Variables with new token
```

### "Deploy keeps failing"
```
Fix: Check Railway Logs tab for errors
Common causes:
- Missing BOT_TOKEN variable
- Syntax error in index.js
- Incorrect Node.js version
```

---

## 📝 Files You Just Got

1. **`railway.json`** - Railway config (auto-detected)
2. **`DEPLOYMENT.md`** - Full production guide
3. **`QUICK_START.md`** - 5-minute quick guide
4. **`token-manager.sh`** - Token management helper
5. **Enhanced `.gitignore`** - Prevents token leaks

All committed ✓

---

## ✅ Final Checklist Before Launching

- [ ] Railway account created
- [ ] Repository deployed to Railway
- [ ] BOT_TOKEN added to Railway Variables
- [ ] Other env vars added
- [ ] Deployment shows "Running" (green)
- [ ] Public URL obtained
- [ ] APP_PUBLIC_BASE_URL updated
- [ ] Health check works: `curl https://url/api/health`
- [ ] App build.gradle.kts updated
- [ ] TeleCam app rebuilt and installed
- [ ] Auth flow tested end-to-end
- [ ] All working! 🎉

---

## 🎉 You're Done!

Backend is now:
✅ Deployed to Railway (24/7)
✅ Running free (on $5 credits)
✅ Token secured (encrypted at rest)
✅ Auto-scaling ready

**Time to celebrate!** 🚀

---

## 📞 Next Questions?

- **Railway Issues**: https://docs.railway.app
- **Telegram Bot API**: https://core.telegram.org/bots/api
- **Token Security**: `token-manager.sh help`
- **Deployment Details**: `DEPLOYMENT.md`

