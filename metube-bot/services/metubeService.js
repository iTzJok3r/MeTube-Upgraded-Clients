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
 * Submit a new download.
 * Uses the same payload shape the Android app sends.
 *
 * @param {string} url            — video/audio URL
 * @param {string} [quality=best] — quality preset
 * @param {string} [format=any]   — output format
 * @param {string} [downloadType=video] — video | audio
 * @returns {object} server response
 */
async function addDownload(url, quality = 'best', format = 'any', downloadType = 'video') {
  const payload = {
    url,
    quality,
    format,
    download_type: downloadType,
    auto_start: true,
  };
  const { data } = await client.post('/add', payload);
  return data;
}

/** Get the configured backend URL (for download links) */
function getBaseUrl() {
  return BASE_URL;
}

module.exports = { getHistory, addDownload, getBaseUrl };
