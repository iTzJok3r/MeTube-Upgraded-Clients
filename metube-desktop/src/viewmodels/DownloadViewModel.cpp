#include "DownloadViewModel.h"

DownloadViewModel::DownloadViewModel(DownloadManager *manager, QObject *parent)
    : QObject(parent), m_manager(manager) {
    connect(m_manager, &DownloadManager::dataChanged, this, &DownloadViewModel::downloadsUpdated);
    connect(m_manager, &DownloadManager::addDownloadResult, this, &DownloadViewModel::addDownloadResult);
    connect(m_manager, &DownloadManager::subscriptionsChanged, this, &DownloadViewModel::subscriptionsUpdated);
    connect(m_manager, &DownloadManager::presetsChanged, this, &DownloadViewModel::presetsUpdated);
    connect(m_manager, &DownloadManager::customDirsChanged, this, &DownloadViewModel::customDirsUpdated);
    connect(m_manager, &DownloadManager::cookieStatusChanged, this, &DownloadViewModel::cookieStatusUpdated);
}

QList<MeTube::DownloadItem> DownloadViewModel::getQueue() const {
    return m_manager->activeDownloads();
}

QList<MeTube::DownloadItem> DownloadViewModel::getHistory() const {
    return m_manager->completedDownloads();
}

QList<MeTube::DownloadItem> DownloadViewModel::getPending() const {
    return m_manager->pendingDownloads();
}

void DownloadViewModel::removeDownload(const QString &id, const QString &where) {
    m_manager->removeDownload(id, where);
}

void DownloadViewModel::addDownload(const QString &url, 
                                    const QString &type, 
                                    const QString &quality,
                                    const QString &format,
                                    const QString &codec,
                                    const QString &folder,
                                    const QString &customNamePrefix,
                                    int playlistItemLimit,
                                    bool autoStart,
                                    bool splitByChapters,
                                    const QString &chapterTemplate,
                                    const QString &subtitleLanguage,
                                    const QString &subtitleMode,
                                    const QStringList &ytdlOptionsPresets,
                                    const QVariantMap &ytdlOptionsOverrides) {
    m_manager->addDownload(url, type, quality, format, codec, folder, customNamePrefix, playlistItemLimit,
                           autoStart, splitByChapters, chapterTemplate, subtitleLanguage, subtitleMode,
                           ytdlOptionsPresets, ytdlOptionsOverrides);
}

void DownloadViewModel::startPending(const QStringList &ids) { m_manager->startPending(ids); }
void DownloadViewModel::cancelAdd() { m_manager->cancelAdd(); }
void DownloadViewModel::fetchPresets() { m_manager->fetchPresets(); }
void DownloadViewModel::fetchSubscriptions() { m_manager->fetchSubscriptions(); }
void DownloadViewModel::addSubscription(const QVariantMap &options) { m_manager->addSubscription(options); }
void DownloadViewModel::updateSubscription(const QString &id, const QVariantMap &changes) { m_manager->updateSubscription(id, changes); }
void DownloadViewModel::deleteSubscriptions(const QStringList &ids) { m_manager->deleteSubscriptions(ids); }
void DownloadViewModel::checkSubscriptions(const QStringList &ids) { m_manager->checkSubscriptions(ids); }
void DownloadViewModel::fetchCookieStatus() { m_manager->fetchCookieStatus(); }
void DownloadViewModel::deleteCookies() { m_manager->deleteCookies(); }

QList<QVariantMap> DownloadViewModel::getSubscriptions() const { return m_manager->subscriptions(); }
QStringList DownloadViewModel::getCustomDirs() const { return m_manager->customDirs(); }
QStringList DownloadViewModel::getPresets() const { return m_manager->presets(); }
bool DownloadViewModel::getHasCookies() const { return m_manager->hasCookies(); }
