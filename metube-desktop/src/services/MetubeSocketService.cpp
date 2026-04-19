#include "MetubeSocketService.h"
#include <QJsonDocument>
#include <QJsonArray>
#include <QDebug>
#include <QNetworkRequest>
#include <QAbstractSocket>
#include <QNetworkProxy>
#include <QIcon>

MetubeSocketService::MetubeSocketService(const QString &baseUrl, QObject *parent)
    : QObject(parent), m_baseUrl(baseUrl), m_isConnected(false), m_usePollingOnly(false),
      m_maxPollingErrors(5), m_pollingErrorCount(0), m_reconnectDelay(5000),
      m_intentionalDisconnect(false) {
    
    m_webSocket = new QWebSocket(QString(), QWebSocketProtocol::VersionLatest, this);
    m_networkManager = new QNetworkAccessManager(this);
    m_pingTimer = new QTimer(this);
    
    // Connect WebSocket signals for diagnostics
    connect(m_webSocket, &QWebSocket::connected, this, &MetubeSocketService::onConnected);
    connect(m_webSocket, &QWebSocket::disconnected, this, &MetubeSocketService::onDisconnected);
    connect(m_webSocket, &QWebSocket::errorOccurred, this, &MetubeSocketService::onError);
    connect(m_webSocket, qOverload<const QList<QSslError>&>(&QWebSocket::sslErrors), this, &MetubeSocketService::onSslErrors);
    connect(m_webSocket, &QWebSocket::textMessageReceived, this, &MetubeSocketService::onTextMessageReceived);
    
    // New Qt 6.5+ signals if available (otherwise logging in onError is fine)
    // connect(m_webSocket, &QWebSocket::handshakeInterruptedOnError, ...);
    
    connect(m_pingTimer, &QTimer::timeout, this, &MetubeSocketService::sendPing);
}

QNetworkRequest MetubeSocketService::createRequest(const QString &urlStr) {
    QNetworkRequest request((QUrl(urlStr)));
    request.setRawHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) MeTubeDesktop/1.1");
    
    // Cloudflare/Nginx often require explicit Host/Origin for WS upgrades
    QUrl url(m_baseUrl);
    request.setRawHeader("Host", url.host().toUtf8());
    
    QString origin = m_baseUrl;
    if (origin.endsWith("/")) origin.chop(1);
    request.setRawHeader("Origin", origin.toUtf8());
    
    if (!m_cookieData.isEmpty()) {
        request.setRawHeader("Cookie", m_cookieData.toUtf8());
    }
    
    return request;
}

void MetubeSocketService::connectToServer() {
    m_sid.clear();
    m_isConnected = false;
    m_pollingErrorCount = 0;
    
    QString pollUrl = m_baseUrl;
    if (!pollUrl.endsWith("/")) pollUrl += "/";
    pollUrl += "socket.io/?EIO=4&transport=polling&t=" + QString::number(QDateTime::currentMSecsSinceEpoch());
    
    qDebug() << "SocketService: Starting handshake at" << pollUrl;
    
    QNetworkRequest request = createRequest(pollUrl);
    QNetworkReply *reply = m_networkManager->get(request);
    connect(reply, &QNetworkReply::finished, this, &MetubeSocketService::onPollingFinished);
}

void MetubeSocketService::onPollingFinished() {
    QNetworkReply *reply = qobject_cast<QNetworkReply*>(sender());
    if (!reply) return;
    reply->deleteLater();

    if (reply->error() != QNetworkReply::NoError) {
        qDebug() << "SocketService Handshake Error:" << reply->errorString();
        emit errorOccurred("Handshake failed: " + reply->errorString());
        return;
    }

    // Update cookies
    QList<QByteArray> rawCookies = reply->rawHeaderList();
    for (const auto &header : rawCookies) {
        if (header.toLower() == "set-cookie") {
            if (!m_cookieData.isEmpty()) m_cookieData += "; ";
            m_cookieData += reply->rawHeader(header);
        }
    }

    QString data = QString::fromUtf8(reply->readAll());
    // EIO=4 handshake returns open packet '0' containing SID
    if (data.startsWith('0')) {
        QJsonDocument doc = QJsonDocument::fromJson(data.mid(1).toUtf8());
        m_sid = doc.object()["sid"].toString();
        qDebug() << "SocketService: SID received:" << m_sid;

        if (m_usePollingOnly) {
            qDebug() << "SocketService: Polling-only mode active. Skipping WS upgrade.";
            m_isConnected = true;
            m_webSocket->sendTextMessage("40"); // Handshake Socket.IO over polling (simulated)
            performLongPoll();
            emit connected();
        } else {
            // Attempt WS upgrade
            QString wsUrl = m_baseUrl;
            if (wsUrl.startsWith("https")) wsUrl.replace(0, 5, "wss");
            else if (wsUrl.startsWith("http")) wsUrl.replace(0, 4, "ws");
            if (!wsUrl.endsWith("/")) wsUrl += "/";
            wsUrl += QString("socket.io/?EIO=4&transport=websocket&sid=%1").arg(m_sid);

            QNetworkRequest request = createRequest(wsUrl);
            qDebug() << "SocketService: Probing WebSocket upgrade at" << wsUrl;
            m_webSocket->open(request);
        }
    }
}

void MetubeSocketService::performLongPoll() {
    if (!m_sid.isEmpty()) {
        QString pollUrl = m_baseUrl;
        if (!pollUrl.endsWith("/")) pollUrl += "/";
        pollUrl += QString("socket.io/?EIO=4&transport=polling&sid=%1&t=%2")
                    .arg(m_sid)
                    .arg(QDateTime::currentMSecsSinceEpoch());

        QNetworkReply *reply = m_networkManager->get(createRequest(pollUrl));
        connect(reply, &QNetworkReply::finished, this, &MetubeSocketService::onLongPollFinished);
    }
}

void MetubeSocketService::onLongPollFinished() {
    QNetworkReply *reply = qobject_cast<QNetworkReply*>(sender());
    if (!reply) return;
    reply->deleteLater();

    if (reply->error() == QNetworkReply::NoError) {
        m_pollingErrorCount = 0;
        QString data = QString::fromUtf8(reply->readAll());
        if (!data.isEmpty()) {
            // EIO v4 polling can contain multiple packets
            // Format: <packet1><packet2>...
            // Each packet starts with a type digit
            processEngineIOPacket(data);
        }
        // Immediately start next poll for "real-time" responsiveness
        QTimer::singleShot(10, this, &MetubeSocketService::performLongPoll);
    } else if (reply->error() != QNetworkReply::OperationCanceledError) {
        m_pollingErrorCount++;
        qDebug() << "SocketService Polling Error:" << reply->errorString();
        if (m_pollingErrorCount >= m_maxPollingErrors) {
            onDisconnected();
        } else {
            QTimer::singleShot(2000, this, &MetubeSocketService::performLongPoll);
        }
    }
}

void MetubeSocketService::processEngineIOPacket(const QString &packet) {
    if (packet.isEmpty()) return;
    
    char type = packet[0].toLatin1();
    if (type == '0') { // Open
        m_isConnected = true;
        emit connected();
    } else if (type == '2') { // Ping
        // Send pong back via POST if polling
        if (m_usePollingOnly) {
            QString postUrl = m_baseUrl;
            if (!postUrl.endsWith("/")) postUrl += "/";
            postUrl += QString("socket.io/?EIO=4&transport=polling&sid=%1").arg(m_sid);
            QNetworkRequest req = createRequest(postUrl);
            req.setHeader(QNetworkRequest::ContentTypeHeader, "text/plain;charset=UTF-8");
            m_networkManager->post(req, "3");
        }
    } else if (type == '4') { // Message
        parseSocketIOMessage(packet.mid(1));
    }
}

void MetubeSocketService::onTextMessageReceived(const QString &message) {
    processEngineIOPacket(message);
}

void MetubeSocketService::onConnected() {
    qDebug() << "SocketService: WebSocket transport connected.";
    m_isConnected = true;
    m_webSocket->sendTextMessage("40"); // Socket.IO connect
    m_pingTimer->start(25000);
    emit connected();
}

void MetubeSocketService::onDisconnected() {
    qDebug() << "SocketService: Disconnected.";
    m_isConnected = false;
    m_pingTimer->stop();
    emit disconnected();
    // Only auto-reconnect if this wasn't an intentional disconnect
    if (!m_intentionalDisconnect) {
        QTimer::singleShot(m_reconnectDelay, this, &MetubeSocketService::connectToServer);
    }
}

void MetubeSocketService::onError(QAbstractSocket::SocketError error) {
    QString err = m_webSocket->errorString();
    qDebug() << "SocketService WS Error:" << error << "-" << err;
    
    // If WS upgrade fails (ConnectionRefused/HandshakeError), fallback to polling
    if (!m_isConnected && !m_usePollingOnly) {
        qDebug() << "SocketService: WebSocket failed. Falling back to Polling-only mode.";
        m_usePollingOnly = true;
        connectToServer();
    }
}

void MetubeSocketService::onSslErrors(const QList<QSslError> &errors) {
    QStringList errs;
    for (const auto &e : errors) errs << e.errorString();
    qDebug() << "SocketService SSL Errors:" << errs.join(", ");
    // Usually we don't ignore, but if behind a corporate proxy it might be needed
    // m_webSocket->ignoreSslErrors();
}

void MetubeSocketService::sendPing() {
    if (m_isConnected && !m_usePollingOnly) {
        m_webSocket->sendTextMessage("2");
    }
}

void MetubeSocketService::parseSocketIOMessage(const QString &payload) {
    if (payload.isEmpty()) return;
    
    // 0: connect, 2: event
    if (payload.startsWith('0')) {
        qDebug() << "SocketService: Socket.IO Session Established.";
        return;
    }
    if (!payload.startsWith('2')) return;

    QJsonDocument doc = QJsonDocument::fromJson(payload.mid(1).toUtf8());
    if (!doc.isArray()) return;

    QJsonArray arr = doc.array();
    if (arr.size() < 2) return;

    QString eventName = arr[0].toString();
    QJsonValue rawData = arr[1];
    QJsonObject data;
    QJsonArray arrayData;
    QString stringData;

    // ALIGNMENT: Backend uses serializer.encode() which often returns a JSON string
    if (rawData.isString()) {
        stringData = rawData.toString();
        QJsonParseError parseError;
        QJsonDocument subDoc = QJsonDocument::fromJson(stringData.toUtf8(), &parseError);
        
        if (parseError.error == QJsonParseError::NoError) {
            if (subDoc.isObject()) {
                data = subDoc.object();
            } else if (subDoc.isArray()) {
                arrayData = subDoc.array();
            }
        }
        
        // Strip quotes for simple string payloads like subscription_removed
        if (stringData.startsWith('"') && stringData.endsWith('"')) {
            stringData = stringData.mid(1, stringData.length() - 2);
        }
    } else if (rawData.isObject()) {
        data = rawData.toObject();
    } else if (rawData.isArray()) {
        arrayData = rawData.toArray();
    } else if (rawData.isString()) { // Unlikely given the above branch, but safe fallback 
        stringData = rawData.toString();
    }

    if (eventName == "added") emit downloadAdded(data);
    else if (eventName == "updated") emit downloadUpdated(data);
    else if (eventName == "completed") emit downloadCompleted(data);
    else if (eventName == "cleared") emit downloadCleared(data["id"].toString());
    else if (eventName == "canceled") emit downloadCanceled(data["id"].toString());
    else if (eventName == "configuration") emit configurationReceived(data);
    else if (eventName == "all") {
        if (!arrayData.isEmpty()) {
            QJsonObject wrapped;
            // Backend all returns [queue+pending, done]
            wrapped["queue"] = arrayData.at(0);
            if (arrayData.size() > 1) {
                wrapped["done"] = arrayData.at(1);
            }
            emit allHistoryReceived(wrapped);
        } else {
            emit allHistoryReceived(data);
        }
    }
    else if (eventName == "custom_dirs") {
        QStringList dirs;
        for (const auto &val : arrayData) dirs.append(val.toString());
        emit customDirsReceived(dirs);
    }
    else if (eventName == "ytdl_options_changed") {
        emit ytdlOptionsChanged(data["update_time"].toVariant().toLongLong());
    }
    else if (eventName == "subscription_added") emit subscriptionAdded(data.toVariantMap());
    else if (eventName == "subscription_updated") emit subscriptionUpdated(data.toVariantMap());
    else if (eventName == "subscription_removed") emit subscriptionRemoved(stringData);
    else if (eventName == "subscriptions_all") {
        QList<QVariantMap> subs;
        for (const auto &val : arrayData) subs.append(val.toObject().toVariantMap());
        emit subscriptionsAllReceived(subs);
    }
}

void MetubeSocketService::disconnectFromServer() {
    m_intentionalDisconnect = true;
    m_pingTimer->stop();
    m_isConnected = false;
    m_sid.clear();
    m_webSocket->close();
}

bool MetubeSocketService::isConnected() const {
    return m_isConnected;
}
