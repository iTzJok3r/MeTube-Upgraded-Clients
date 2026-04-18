/**
 * Lightweight JSON store for mapping MeTube download IDs → Telegram user IDs.
 * Persists to data/jobs.json so ownership survives bot restarts.
 */
const fs = require('fs');
const path = require('path');

const DATA_DIR = path.join(__dirname, '..', 'data');
const STORE_PATH = path.join(DATA_DIR, 'jobs.json');

let _data = {};

function _ensureDir() {
  if (!fs.existsSync(DATA_DIR)) fs.mkdirSync(DATA_DIR, { recursive: true });
}

function load() {
  _ensureDir();
  try {
    if (fs.existsSync(STORE_PATH)) {
      _data = JSON.parse(fs.readFileSync(STORE_PATH, 'utf8'));
    }
  } catch {
    _data = {};
  }
}

function save() {
  _ensureDir();
  const tmp = STORE_PATH + '.tmp';
  fs.writeFileSync(tmp, JSON.stringify(_data, null, 2));
  fs.renameSync(tmp, STORE_PATH);
}

/** Record that telegramUserId owns metubeId */
function setOwner(metubeId, telegramUserId) {
  _data[metubeId] = telegramUserId;
  save();
}

/** Get the Telegram user ID that owns metubeId, or null */
function getOwner(metubeId) {
  return _data[metubeId] ?? null;
}

/** Remove a job entry */
function remove(metubeId) {
  delete _data[metubeId];
  save();
}

/** Get all entries */
function all() {
  return { ..._data };
}

module.exports = { load, save, setOwner, getOwner, remove, all };
