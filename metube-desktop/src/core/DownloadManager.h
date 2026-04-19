#ifndef DOWNLOADMANAGER_H
#define DOWNLOADMANAGER_H

#include <QObject>
#include <QList>
#include <QVariantMap>
#include <QStringList>
#include <QVariant>
#include "Models.h"
#include "services/MetubeRestService.h"
#include "services/MetubeSocketService.h"
#include "core/AppSettings.h"

class DownloadManager : public QObject {
    Q_OBJECT
public:
    explicit DownloadManager(AppSettings *settings, QObject *parent = nullptr);

    void updateServerUrl(const QString &url);
    const QList<MeTube::DownloadItem>& activeDownloads() const { return m_queue; }
    const QList<MeTube::DownloadItem>& completedDownloads() const { return m_done; }
    const QList<MeTube::DownloadItem>& pendingDownloads() const { return m_pending; }

    void refreshHistory();
    void addDownload(const QString &url, 
                     const QString &type = "video", 
                     const QString &quality = "best",
                     const QString &format = "",
                     const QString &codec = "",
                     const QString &folder = "",
                     const QString &customNamePrefix = "",
                     int playlistItemLimit = 0,
                     bool autoStart = true,
                     bool splitByChapters = false,
                     const QString &chapterTemplate = "",
                     const QString &subtitleLanguage = "",
                     const QString &subtitleMode = "",
                     const QStringList &ytdlOptionsPresets = QStringList(),
                     const QVariantMap &ytdlOptionsOverrides = QVariantMap());
    void removeDownload(const QString &id, const QString &where);

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

    QList<MeTube::DownloadItem> done() const { return m_done; }
    QList<MeTube::DownloadItem> queue() const { return m_queue; }
    QList<MeTube::DownloadItem> pending() const { return m_pending; }

    QList<QVariantMap> subscriptions() const { return m_subscriptions; }
    QStringList customDirs() const { return m_customDirs; }
    QStringList presets() const { return m_presets; }
    bool hasCookies() const { return m_hasCookies; }

signals:
    void dataChanged();
    void errorOccurred(const QString &error);
    void downloadFinished(const QString &title);
    void addDownloadResult(bool success, const QString &message);
    void connectionStatusChanged(bool connected);
    void socketConnecting(); // New proxy signal
    void subscriptionsChanged();
    void presetsChanged();
    void customDirsChanged();
    void cookieStatusChanged();

private slots:
    void onHistoryFetched(const QList<MeTube::DownloadItem> &done, 
                          const QList<MeTube::DownloadItem> &queue, 
                          const QList<MeTube::DownloadItem> &pending);

private:
    AppSettings *m_settings;
    MetubeRestService *m_restService;
    MetubeSocketService *m_socketService;
    
    QList<MeTube::DownloadItem> m_done;
    QList<MeTube::DownloadItem> m_queue;
    QList<MeTube::DownloadItem> m_pending;

    QList<QVariantMap> m_subscriptions;
    QStringList m_customDirs;
    QStringList m_presets;
    bool m_hasCookies = false;
};

#endif // DOWNLOADMANAGER_H
