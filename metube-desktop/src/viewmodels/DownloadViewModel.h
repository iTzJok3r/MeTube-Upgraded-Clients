#ifndef DOWNLOADVIEWMODEL_H
#define DOWNLOADVIEWMODEL_H

#include <QObject>
#include <QList>
#include <QVariantMap>
#include <QStringList>
#include "core/DownloadManager.h"

class DownloadViewModel : public QObject {
    Q_OBJECT
public:
    explicit DownloadViewModel(DownloadManager *manager, QObject *parent = nullptr);

    QList<MeTube::DownloadItem> getQueue() const;
    QList<MeTube::DownloadItem> getHistory() const;
    QList<MeTube::DownloadItem> getPending() const;

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

    QList<QVariantMap> getSubscriptions() const;
    QStringList getCustomDirs() const;
    QStringList getPresets() const;
    bool getHasCookies() const;

signals:
    void downloadsUpdated();
    void addDownloadResult(bool success, const QString &message);
    void subscriptionsUpdated();
    void presetsUpdated();
    void customDirsUpdated();
    void cookieStatusUpdated();

private:
    DownloadManager *m_manager;
};

#endif // DOWNLOADVIEWMODEL_H
