/**
 * MeTube Telegram Bot — index.js
 *
 * Connects to a MeTube backend, lets authorized Telegram users
 * submit download URLs and get real-time completion notifications.
 */
require('dotenv').config();

const { Telegraf } = require('telegraf');
const metubeService = require('./services/metubeService');
const socketService = require('./services/socketService');
const store = require('./services/store');

// ─── Config ──────────────────────────────────────────────────────────────────

const BOT_TOKEN = process.env.BOT_TOKEN;
if (!BOT_TOKEN) {
  console.error('FATAL: BOT_TOKEN is not set. Copy .env.example to .env and fill it in.');
  process.exit(1);
}

const ALLOWED = (process.env.ALLOWED_USER_IDS || '')
  .split(',')
  .map((s) => s.trim())
  .filter(Boolean)
  .map(Number);

if (ALLOWED.length === 0) {
  console.warn('WARNING: ALLOWED_USER_IDS is empty — no one can use the bot.');
}

// ─── Bot setup ───────────────────────────────────────────────────────────────

const bot = new Telegraf(BOT_TOKEN);

// ─── Auth middleware ─────────────────────────────────────────────────────────

function isAuthorized(userId) {
  if (ALLOWED.length === 0) return false;
  return ALLOWED.includes(userId);
}

bot.use((ctx, next) => {
  const userId = ctx.from?.id;
  if (!isAuthorized(userId)) {
    return ctx.reply('⛔ You are not authorized to use this bot.');
  }
  return next();
});

// ─── Helpers ─────────────────────────────────────────────────────────────────

function formatSize(bytes) {
  if (!bytes || bytes <= 0) return '';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
  return (bytes / Math.pow(1024, i)).toFixed(1) + ' ' + units[i];
}

function itemLine(item, index) {
  const title = item.title || item.url || 'Untitled';
  const status = (item.status || 'unknown').charAt(0).toUpperCase() + (item.status || '').slice(1);
  const pct = item.percent != null ? ' ' + Math.round(item.percent) + '%' : '';
  const size = item.size ? ' - ' + formatSize(Number(item.size)) : '';
  return (index + 1) + '. ' + title + '\n   ' + status + pct + size;
}

// ─── Commands ────────────────────────────────────────────────────────────────

bot.command('start', (ctx) => {
  const msg = [
    '\uD83C\uDFAC MeTube Bot',
    '',
    'Send me any video URL and I will add it to MeTube.',
    '',
    'Commands:',
    '/queue - show active downloads',
    '/done - show completed downloads',
    '/help - show this message',
  ].join('\n');
  ctx.reply(msg);
});

bot.command('help', (ctx) => {
  const msg = [
    '\uD83D\uDCD6 Help',
    '',
    '\u2022 Send any URL to download it',
    '\u2022 /queue - active downloads',
    '\u2022 /done - completed downloads',
    '\u2022 You will get a notification when your download finishes',
  ].join('\n');
  ctx.reply(msg);
});

bot.command('queue', async (ctx) => {
  try {
    const history = await metubeService.getHistory();
    const items = [...history.queue, ...history.pending];
    if (items.length === 0) {
      return ctx.reply('\uD83D\uDCED No active downloads.');
    }
    const lines = items.map((item, i) => itemLine(item, i));
    await ctx.reply('\uD83D\uDCE5 Active Downloads (' + items.length + ')\n\n' + lines.join('\n\n'));
  } catch (err) {
    console.error('[cmd:queue]', err.message);
    ctx.reply('\u274C Failed to fetch queue: ' + err.message);
  }
});

bot.command('done', async (ctx) => {
  try {
    const history = await metubeService.getHistory();
    const items = history.done.slice(-15);
    if (items.length === 0) {
      return ctx.reply('\uD83D\uDCED No completed downloads.');
    }
    const lines = items.map((item, i) => itemLine(item, i));
    const total = history.done.length;
    const showing = items.length < total
      ? ' (showing last ' + items.length + ' of ' + total + ')'
      : '';
    await ctx.reply('\u2705 Completed Downloads' + showing + '\n\n' + lines.join('\n\n'));
  } catch (err) {
    console.error('[cmd:done]', err.message);
    ctx.reply('\u274C Failed to fetch history: ' + err.message);
  }
});

// ─── URL detection ───────────────────────────────────────────────────────────

const URL_RE = /https?:\/\/\S+/gi;

bot.on('text', async (ctx) => {
  const text = ctx.message.text || '';
  const rawUrls = text.match(URL_RE);
  if (!rawUrls || rawUrls.length === 0) return;

  // Strip trailing punctuation that the regex may have captured
  const urls = rawUrls.map((u) => u.replace(/[.,;:!?)\]]+$/, ''));

  for (const url of urls) {
    try {
      await ctx.reply('\u23F3 Adding: ' + url);
      const result = await metubeService.addDownload(url);

      if (result && result.status === 'error') {
        await ctx.reply('\u274C Server error: ' + (result.msg || 'unknown'));
        continue;
      }

      // Track ownership by URL for notification routing
      store.setOwner(url, ctx.from.id);

      // Also try to track by ID if the server returns one
      const metubeId = result?.id || result?.entry?.id;
      if (metubeId) {
        store.setOwner(String(metubeId), ctx.from.id);
      }

      await ctx.reply('\u2705 Added to download queue!');
    } catch (err) {
      console.error('[url:add]', err.message);
      const reason =
        err.response?.data?.message ||
        err.response?.data?.msg ||
        err.message;
      await ctx.reply('\u274C Failed to add: ' + reason);
    }
  }
});

// ─── Socket.IO real-time notifications ───────────────────────────────────────

socketService.on('completed', async (data) => {
  try {
    if (!data) return;

    const id = data.id || data.url || '';
    const url = data.url || '';
    const title = data.title || 'Untitled';
    const status = (data.status || 'finished').toLowerCase();
    const isError = status === 'error';

    // Find owner by ID or URL
    let owner = store.getOwner(String(id));
    if (!owner && url) owner = store.getOwner(url);
    if (!owner) {
      console.log('[socket:completed] no owner for ' + (id || url) + ', skipping');
      return;
    }

    const size = data.size ? formatSize(Number(data.size)) : '';
    const errorMsg = data.error || data.msg || '';

    let message;
    if (isError) {
      message = '\u274C Download Failed\n\n'
        + '\uD83D\uDCC4 ' + title + '\n'
        + '\u26A0\uFE0F ' + (errorMsg || 'Unknown error');
    } else {
      const sizeInfo = size ? '\n\uD83D\uDCE6 ' + size : '';
      const filename = data.filename || '';
      let dlLink = '';
      if (filename) {
        const base = metubeService.getBaseUrl();
        dlLink = '\n\uD83D\uDD17 ' + base + '/download/' + encodeURIComponent(filename);
      }
      message = '\u2705 Download Complete\n\n'
        + '\uD83D\uDCC4 ' + title
        + sizeInfo + dlLink;
    }

    await bot.telegram.sendMessage(owner, message, {
      disable_web_page_preview: true,
    });

    // Clean up ownership
    store.remove(String(id));
    if (url) store.remove(url);
  } catch (err) {
    console.error('[socket:completed] notification error:', err.message);
  }
});

socketService.on('connected', async () => {
  console.log('[bot] Socket.IO connected — resyncing state');
  // Resync: fetch latest state to catch anything missed while disconnected
  try {
    const history = await metubeService.getHistory();
    console.log(
      '[bot] resync: ' + history.queue.length + ' queue, '
      + history.done.length + ' done'
    );
  } catch (err) {
    console.warn('[bot] resync failed:', err.message);
  }
});

socketService.on('disconnected', () => {
  console.log('[bot] Socket.IO disconnected');
});

// ─── Startup ─────────────────────────────────────────────────────────────────

async function main() {
  store.load();
  console.log('[bot] loaded job store');

  // Hydrate initial state from backend
  try {
    const history = await metubeService.getHistory();
    console.log(
      '[bot] hydrated: ' + history.queue.length + ' queue, '
      + history.pending.length + ' pending, '
      + history.done.length + ' done'
    );
  } catch (err) {
    console.warn('[bot] initial hydration failed:', err.message);
  }

  // Connect Socket.IO for live updates
  socketService.connect();

  // Launch Telegraf
  const webhookUrl = process.env.BOT_WEBHOOK_URL;
  if (webhookUrl) {
    const port = parseInt(process.env.BOT_WEBHOOK_PORT || '3000', 10);
    const secret = process.env.BOT_WEBHOOK_SECRET || '';
    const hookPath = '/bot-webhook' + (secret ? '-' + secret : '');
    await bot.launch({
      webhook: { domain: webhookUrl, port, hookPath },
    });
    console.log('[bot] started in WEBHOOK mode on port ' + port);
  } else {
    await bot.launch();
    console.log('[bot] started in POLLING mode');
  }

  console.log('[bot] authorized users: ' +
    (ALLOWED.length > 0 ? ALLOWED.join(', ') : 'ALL (open)'));
}

// Graceful shutdown
process.once('SIGINT', () => { bot.stop('SIGINT'); socketService.disconnect(); });
process.once('SIGTERM', () => { bot.stop('SIGTERM'); socketService.disconnect(); });

main().catch((err) => {
  console.error('FATAL:', err);
  process.exit(1);
});
