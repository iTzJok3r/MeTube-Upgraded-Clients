/**
 * metubeService.js — HTTP client for the MeTube REST API.
 *
 * Mirrors the Android app's payload-building philosophy:
 *   - POST /add  with { url, quality, format, download_type }
 *   - GET  /history  → { queue:[], done:[], pending:[] }
 */
const axios = require('axios');

const BASE_URL = (process.env.METUBE_URL || 'https://y.itzjok3r.qzz.io/').replace(/\/$/, '');

const client = axios.create({
  baseURL: BASE_URL,
  timeout: 15_000,
  headers: { 'Content-Type': 'application/json' },
});

/**
 * Fetch full state from the backend.
 * @returns {{ queue: object[], done: object[], pending: object[] }}
 */
async function getHistory() {
  const { data } = await client.get('/history');
  return {
    queue: Array.isArray(data.queue) ? data.queue : [],
    done: Array.isArray(data.done) ? data.done : [],
    pending: Array.isArray(data.pending) ? data.pending : [],
  };
}

/**
 * Submit a new download with full parameter support.
 * @param {object} options - See Web UI AddDownloadPayload
 */
async function addDownload(options) {
  const payload = {
    url: options.url,
    download_type: options.downloadType || 'video',
    quality: options.quality || 'best',
    format: options.format || 'any',
    codec: options.codec || 'auto',
    folder: options.folder || '',
    custom_name_prefix: options.customNamePrefix || '',
    playlist_item_limit: options.playlistItemLimit || 0,
    auto_start: options.autoStart !== false,
    split_by_chapters: !!options.splitByChapters,
    chapter_template: options.chapterTemplate || '',
    subtitle_language: options.subtitleLanguage || '',
    subtitle_mode: options.subtitleMode || 'prefer_manual',
    ytdl_options_presets: options.ytdlOptionsPresets || [],
    ytdl_options_overrides: options.ytdlOptionsOverrides || '',
  };
  const { data } = await client.post('/add', payload);
  return data;
}

/**
 * Handle subscription management.
 */
async function getSubscriptions() {
  const { data } = await client.get('/subscriptions');
  return data;
}

async function checkSubscriptions(ids = []) {
  const { data } = await client.post('/subscriptions/check', { ids });
  return data;
}

async function deleteSubscription(ids) {
  const { data } = await client.post('/subscriptions/delete', { ids });
  return data;
}

/**
 * Handle cookie management.
 */
async function getCookieStatus() {
  const { data } = await client.get('/cookie-status');
  return data;
}

async function deleteCookies() {
  const { data } = await client.post('/delete-cookies');
  return data;
}

/**
 * Build a download URL matching the MeTube Web UI logic.
 * Mirrors the official Web UI's payload-building and routing philosophy.
 * @param {object} item - Download item from Socket.IO or API
 * @param {object} config - Server configuration (for PUBLIC_HOST_URL)
 */
function buildDownloadLink(item, config = {}) {
  let baseDir = config["PUBLIC_HOST_URL"] || 'download/';
  const isAudio = item.download_type === 'audio' || (item.filename && item.filename.toLowerCase().endsWith('.mp3'));
  
  if (isAudio) {
    baseDir = config["PUBLIC_HOST_AUDIO_URL"] || 'audio_download/';
  }

  // Ensure absolute URL if baseDir is relative
  let url = BASE_URL;
  if (!url.endsWith('/')) url += '/';
  url += baseDir;

  if (item.folder) {
    const encodedFolder = item.folder
      .split('/')
      .filter(s => s.length > 0)
      .map(s => encodeURIComponent(s))
      .join('/') + '/';
    url += encodedFolder;
  }

  return url + encodeURIComponent(item.filename || '');
}

/** Get the configured backend URL */
function getBaseUrl() {
  return BASE_URL;
}

module.exports = {
  getHistory,
  addDownload,
  getSubscriptions,
  checkSubscriptions,
  deleteSubscription,
  getCookieStatus,
  deleteCookies,
  buildDownloadLink,
  getBaseUrl
};
