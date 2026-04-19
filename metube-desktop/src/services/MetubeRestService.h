#ifndef METUBERESTSERVICE_H
#define METUBERESTSERVICE_H

#include <QObject>
#include <QNetworkAccessManager>
#include <QNetworkReply>
#include <QVariant>
#include <QStringList>
#include "Models.h"

class MetubeRestService : public QObject {
    Q_OBJECT
public:
    explicit MetubeRestService(const QString &baseUrl, QObject *parent = nullptr);

    void fetchHistory();
    void addDownload(const QVariantMap &options);
    void deleteDownload(const QVariantMap &payload);

    void startPending(const QStringList &ids = QStringList());
    void cancelAdd();
    void fetchPresets();

    void fetchSubscriptions();
    void addSubscription(const QVariantMap &options);
    void updateSubscription(const QString &id, const QVariantMap &changes);
    void deleteSubscriptions(const QStringList &ids);
    void checkSubscriptions(const QStringList &ids = QStringList());

    void fetchCookieStatus();
    void deleteCookies();

signals:
    void historyFetched(const QList<MeTube::DownloadItem> &done, 
                        const QList<MeTube::DownloadItem> &queue, 
                        const QList<MeTube::DownloadItem> &pending);
    void addDownloadFinished(bool success, const QString &message);
    void presetsFetched(const QStringList &presets);
    void subscriptionsFetched(const QList<QVariantMap> &subscriptions);
    void cookieStatusFetched(bool hasCookies);
    void genericActionFinished(bool success, const QString &message);
    void errorOccurred(const QString &error);

private slots:
    void onHistoryFinished(QNetworkReply *reply);
    void onAddFinished(QNetworkReply *reply);

private:
    QString m_baseUrl;
    QNetworkAccessManager *m_networkManager;
    
    MeTube::DownloadItem parseDownloadItem(const QJsonObject &obj);
    void postGeneric(const QString &endpoint, const QJsonObject &payload, const QString &successMsg);
};

#endif // METUBERESTSERVICE_H
