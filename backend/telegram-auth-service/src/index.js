import dotenv from 'dotenv';
import express from 'express';
import { Telegraf, Markup } from 'telegraf';

dotenv.config();

const PORT = Number(process.env.PORT || 8080);
const HOST = process.env.HOST || '0.0.0.0';
const BASE_APP_DEEP_LINK = process.env.BASE_APP_DEEP_LINK || 'telecam://auth';
const APP_PUBLIC_BASE_URL = String(process.env.APP_PUBLIC_BASE_URL || '').trim().replace(/\/+$/, '');
const BOT_TOKEN = process.env.BOT_TOKEN;
const BOT_USERNAME = process.env.BOT_USERNAME;
const AUTH_TOKEN_TTL_MS = Number(process.env.AUTH_TOKEN_TTL_MS || 10 * 60 * 1000);

if (!BOT_TOKEN || !BOT_USERNAME) {
  throw new Error('BOT_TOKEN and BOT_USERNAME are required.');
}

const app = express();
app.use(express.json());

const authSessions = new Map();

const cleanupExpired = () => {
  const now = Date.now();
  for (const [token, session] of authSessions.entries()) {
    if (session.expiresAt <= now) {
      authSessions.delete(token);
    }
  }
};

setInterval(cleanupExpired, 30_000);

const NORMALIZED_BOT_USERNAME = String(BOT_USERNAME || '').trim().replace(/^@/, '');
const createAuthUrl = (token) => `https://t.me/${NORMALIZED_BOT_USERNAME}?start=auth_${token}`;
const buildDeepLink = (token) => {
  const separator = BASE_APP_DEEP_LINK.includes('?') ? '&' : '?';
  return `${BASE_APP_DEEP_LINK}${separator}token=${encodeURIComponent(token)}`;
};
const buildOpenAppUrl = (token) => {
  if (!/^https?:\/\//i.test(APP_PUBLIC_BASE_URL)) return null;
  return `${APP_PUBLIC_BASE_URL}/open-app?token=${encodeURIComponent(token)}`;
};

app.post('/api/auth/telegram/start', (req, res) => {
  const token = String(req.body?.token || '').trim();
  const isUuidLike = /^[0-9a-fA-F-]{36}$/.test(token);

  if (!isUuidLike) {
    return res.status(400).json({ message: 'Invalid token format' });
  }

  const expiresAt = Date.now() + AUTH_TOKEN_TTL_MS;
  authSessions.set(token, {
    token,
    status: 'pending',
    telegramUserId: null,
    username: null,
    createdAt: Date.now(),
    expiresAt,
    used: false
  });

  return res.json({
    token,
    authUrl: createAuthUrl(token),
    expiresAt
  });
});

app.get('/api/auth/telegram/verify', (req, res) => {
  const token = String(req.query?.token || '').trim();
  const session = authSessions.get(token);

  if (!session) {
    return res.status(404).json({ message: 'Token not found or expired' });
  }

  if (session.expiresAt <= Date.now()) {
    authSessions.delete(token);
    return res.status(410).json({ message: 'Token expired' });
  }

  if (session.used) {
    return res.status(409).json({ message: 'Token already used' });
  }

  if (session.status !== 'verified' || !session.telegramUserId) {
    return res.status(202).json({
      token,
      telegramUserId: 0,
      username: null,
      isVerified: false
    });
  }

  session.used = true;

  return res.json({
    token,
    telegramUserId: session.telegramUserId,
    username: session.username,
    isVerified: true
  });
});

app.get('/health', (_, res) => {
  res.json({ ok: true });
});

app.get('/open-app', (req, res) => {
  const token = String(req.query?.token || '').trim();
  const isUuidLike = /^[0-9a-fA-F-]{36}$/.test(token);

  if (!isUuidLike) {
    return res.status(400).send('Invalid token');
  }

  const deepLink = buildDeepLink(token);

  return res
    .status(200)
    .type('html')
    .send(`<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width,initial-scale=1" />
  <title>Open TeleCam</title>
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, sans-serif; margin: 0; padding: 24px; background: #0b0f14; color: #f2f5f9; }
    .card { max-width: 460px; margin: 40px auto; background: #131a22; border-radius: 16px; padding: 24px; border: 1px solid #223042; }
    h1 { margin: 0 0 10px; font-size: 24px; }
    p { margin: 0 0 16px; line-height: 1.45; color: #c3d1de; }
    a.btn { display: inline-block; background: #2f9cf4; color: #fff; text-decoration: none; padding: 12px 18px; border-radius: 12px; font-weight: 600; }
  </style>
</head>
<body>
  <div class="card">
    <h1>Connected successfully ✅</h1>
    <p>Opening TeleCam app automatically. If it does not open, tap the button below.</p>
    <a class="btn" href="${deepLink}">Back to TeleCam</a>
  </div>
  <script>
    setTimeout(function () {
      window.location.href = ${JSON.stringify(deepLink)};
    }, 300);
  </script>
</body>
</html>`);
});

const bot = new Telegraf(BOT_TOKEN);

bot.start(async (ctx) => {
  try {
    const payload = ctx.startPayload || '';

    if (!payload.startsWith('auth_')) {
      await ctx.reply('Welcome to TeleCam auth bot. Please open setup from the app.');
      return;
    }

    const token = payload.replace('auth_', '').trim();
    const session = authSessions.get(token);

    if (!session) {
      await ctx.reply('This auth link is invalid or expired. Please start again from TeleCam app.');
      return;
    }

    if (session.expiresAt <= Date.now()) {
      authSessions.delete(token);
      await ctx.reply('This auth session has expired. Please retry from app.');
      return;
    }

    if (session.used) {
      await ctx.reply('This auth session was already used. Please create a new one in app.');
      return;
    }

    session.status = 'verified';
    session.telegramUserId = ctx.from?.id || null;
    session.username = ctx.from?.username || null;

    const openAppUrl = buildOpenAppUrl(token);

    if (openAppUrl) {
      await ctx.reply(
        'Connected successfully ✅\n\nTap the button to return to TeleCam.',
        Markup.inlineKeyboard([
          Markup.button.url('Back to TeleCam', openAppUrl)
        ])
      );
      return;
    }

    await ctx.reply(
      'Connected successfully ✅\n\nNow go back to TeleCam app and tap "I tapped Start in Telegram" to finish setup.\n\nTip: set APP_PUBLIC_BASE_URL to enable Back to TeleCam button in Telegram.'
    );
  } catch (error) {
    console.error('bot.start handler failed', error);
    await ctx.reply('Setup hit a temporary issue. Please retry from TeleCam app.');
  }
});

bot.catch((error) => {
  console.error('Unhandled bot error', error);
});

Promise.all([
  bot.launch(),
  new Promise((resolve) => app.listen(PORT, HOST, resolve))
])
  .then(() => {
    console.log(`telegram-auth-service running on ${HOST}:${PORT}`);
  })
  .catch((error) => {
    console.error('Failed to start service', error);
    process.exit(1);
  });

process.once('SIGINT', () => bot.stop('SIGINT'));
process.once('SIGTERM', () => bot.stop('SIGTERM'));
