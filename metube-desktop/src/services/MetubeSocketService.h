#ifndef METUBESOCKETSERVICE_H
#define METUBESOCKETSERVICE_H

#include <QObject>
#include <QtWebSockets/QWebSocket>
#include <QTimer>
#include <QJsonObject>
#include <QNetworkAccessManager>
#include <QNetworkReply>
#include <QNetworkRequest>
#include <QNetworkProxy>
#include <QIcon>

class MetubeSocketService : public QObject {
    Q_OBJECT
public:
    explicit MetubeSocketService(const QString &baseUrl, QObject *parent = nullptr);

    void connectToServer();
    void disconnectFromServer();
    bool isConnected() const;

signals:
    void connected();
    void disconnected();
    void connecting(); // New signal for UI feedback
    void downloadAdded(const QJsonObject &item);
    void downloadUpdated(const QJsonObject &item);
    void downloadCompleted(const QJsonObject &item);
    void downloadCanceled(const QString &id);
    void downloadCleared(const QString &id);
    void configurationReceived(const QJsonObject &config);
    void allHistoryReceived(const QJsonObject &history);
    void customDirsReceived(const QStringList &dirs);
    void ytdlOptionsChanged(qint64 updateTime);
    void subscriptionAdded(const QVariantMap &subscription);
    void subscriptionUpdated(const QVariantMap &subscription);
    void subscriptionRemoved(const QString &id);
    void subscriptionsAllReceived(const QList<QVariantMap> &subscriptions);
    void errorOccurred(const QString &error);
    void sslErrorsOccurred(const QString &errors);

private slots:
    void onConnected();
    void onDisconnected();
    void onError(QAbstractSocket::SocketError error);
    void onSslErrors(const QList<QSslError> &errors);
    void onTextMessageReceived(const QString &message);
    void sendPing();
    void onPollingFinished();
    void performLongPoll();
    void onLongPollFinished();

private:
    void parseSocketIOMessage(const QString &message);
    void processEngineIOPacket(const QString &packet);
    QNetworkRequest createRequest(const QString &url);
    
    QString m_baseUrl;
    QString m_sid;
    QString m_cookieData;
    QWebSocket *m_webSocket;
    QNetworkAccessManager *m_networkManager;
    QTimer *m_pingTimer;
    bool m_isConnected;
    bool m_usePollingOnly;
    bool m_intentionalDisconnect;
    int m_maxPollingErrors;
    int m_pollingErrorCount;
    int m_reconnectDelay;

};

#endif // METUBESOCKETSERVICE_H
