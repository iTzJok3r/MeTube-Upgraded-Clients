/**
 * socketService.js — Socket.IO client for MeTube real-time events.
 *
 * Connects to the same Socket.IO namespace the web UI uses.
 * Emits Node EventEmitter events that index.js subscribes to:
 *   'added'     — new item entered the queue
 *   'updated'   — progress / status change
 *   'completed' — item finished (success or error)
 *   'canceled'  — item was canceled
 *   'cleared'   — history cleared
 */
const { io } = require('socket.io-client');
const EventEmitter = require('events');
const metubeService = require('./metubeService');

class SocketService extends EventEmitter {
  constructor() {
    super();
    this._socket = null;
    this._connected = false;
  }

  /** Connect (or reconnect) to the MeTube backend */
  connect() {
    if (this._socket) {
      this._socket.removeAllListeners();
      this._socket.disconnect();
    }

    const url = metubeService.getBaseUrl();
    console.log(`[socket] connecting to ${url}`);

    this._socket = io(url, {
      transports: ['polling', 'websocket'],
      reconnection: true,
      reconnectionDelay: 2000,
      reconnectionDelayMax: 10000,
    });

    this._socket.on('connect', () => {
      console.log('[socket] connected');
      this._connected = true;
      this.emit('connected');
    });

    this._socket.on('disconnect', (reason) => {
      console.log(`[socket] disconnected: ${reason}`);
      this._connected = false;
      this.emit('disconnected', reason);
    });

    this._socket.on('connect_error', (err) => {
      console.error(`[socket] connection error: ${err.message}`);
    });

    // MeTube real-time events
    this._socket.on('added', (data) => this._safeEmit('added', data));
    this._socket.on('updated', (data) => this._safeEmit('updated', data));
    this._socket.on('completed', (data) => this._safeEmit('completed', data));
    this._socket.on('canceled', (data) => this._safeEmit('canceled', data));
    this._socket.on('cleared', (data) => this._safeEmit('cleared', data));

    // "all" event — full state resync after reconnect
    this._socket.on('all', (data) => this._safeEmit('all', data));
  }

  _safeEmit(event, data) {
    try {
      this.emit(event, data);
    } catch (err) {
      console.error(`[socket] error in ${event} handler:`, err.message);
    }
  }

  get connected() {
    return this._connected;
  }

  disconnect() {
    if (this._socket) {
      this._socket.disconnect();
      this._socket = null;
      this._connected = false;
    }
  }
}

// Singleton
module.exports = new SocketService();
