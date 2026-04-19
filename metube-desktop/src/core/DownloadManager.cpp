#include "DownloadManager.h"
#include "services/MetubeSocketService.h"
#include "services/RequestBuilder.h"
#include "utils/JsonUtils.h"

DownloadManager::DownloadManager(AppSettings *settings, QObject *parent)
    : QObject(parent), m_settings(settings) {
    m_restService = nullptr;
    m_socketService = nullptr;
    updateServerUrl(m_settings->serverUrl());
    
    // Reconnect when server URL changes in settings
    connect(m_settings, &AppSettings::serverUrlChanged, this, &DownloadManager::updateServerUrl);
}

void DownloadManager::updateServerUrl(const QString &url) {
    // Safely tear down old services — disconnect signals first to prevent
    // stale signal delivery from deleteLater'd objects
    if (m_socketService) {
        m_socketService->disconnectFromServer();
        m_socketService->disconnect(this);
        m_socketService->deleteLater();
        m_socketService = nullptr;
    }
    if (m_restService) {
        m_restService->disconnect(this);
        m_restService->deleteLater();
        m_restService = nullptr;
    }

    m_restService = new MetubeRestService(url, this);
    m_socketService = new MetubeSocketService(url, this);

    connect(m_restService, &MetubeRestService::historyFetched, this, &DownloadManager::onHistoryFetched);
    connect(m_restService, &MetubeRestService::errorOccurred, this, &DownloadManager::errorOccurred);
    connect(m_restService, &MetubeRestService::addDownloadFinished, this, &DownloadManager::addDownloadResult);
    
    // Core Socket Sync
    connect(m_socketService, &MetubeSocketService::connected, this, [this]() {
        emit connectionStatusChanged(true);
        refreshHistory();
        m_restService->fetchPresets();
        m_restService->fetchCookieStatus();
    });
    connect(m_socketService, &MetubeSocketService::disconnected, this, [this]() {
        emit connectionStatusChanged(false);
    });
    
    connect(m_socketService, &MetubeSocketService::allHistoryReceived, this, [this](const QJsonObject &obj) {
        auto payload = MeTube::JsonUtils::parseHistoryPayload(obj);
        onHistoryFetched(payload.done, payload.queue, payload.pending);
    });

    connect(m_socketService, &MetubeSocketService::customDirsReceived, this, [this](const QStringList &dirs) {
        if (m_customDirs != dirs) {
            m_customDirs = dirs;
            emit customDirsChanged();
        }
    });

    connect(m_socketService, &MetubeSocketService::subscriptionsAllReceived, this, [this](const QList<QVariantMap> &subs) {
        m_subscriptions = subs;
        emit subscriptionsChanged();
    });

    connect(m_socketService, &MetubeSocketService::subscriptionAdded, this, [this](const QVariantMap &sub) {
        m_subscriptions.append(sub);
        emit subscriptionsChanged();
    });

    connect(m_socketService, &MetubeSocketService::subscriptionRemoved, this, [this](const QString &id) {
        for (int i = 0; i < m_subscriptions.size(); ++i) {
            if (m_subscriptions[i]["id"].toString() == id) {
                m_subscriptions.removeAt(i);
                emit subscriptionsChanged();
                break;
            }
        }
    });

    connect(m_socketService, &MetubeSocketService::subscriptionUpdated, this, [this](const QVariantMap &sub) {
        QString id = sub["id"].toString();
        bool found = false;
        for (int i = 0; i < m_subscriptions.size(); ++i) {
            if (m_subscriptions[i]["id"].toString() == id) {
                m_subscriptions[i] = sub;
                found = true;
                emit subscriptionsChanged();
                break;
            }
        }
        if (!found) {
            m_subscriptions.append(sub);
            emit subscriptionsChanged();
        }
    });

    connect(m_restService, &MetubeRestService::genericActionFinished, this, [](bool success, const QString &msg) {
        if (!success) qDebug() << "REST Action Error:" << msg;
    });

    connect(m_restService, &MetubeRestService::presetsFetched, this, [this](const QStringList &presets) {
        m_presets = presets;
        emit presetsChanged();
    });

    connect(m_restService, &MetubeRestService::subscriptionsFetched, this, [this](const QList<QVariantMap> &subs) {
        m_subscriptions = subs;
        emit subscriptionsChanged();
    });

    connect(m_restService, &MetubeRestService::cookieStatusFetched, this, [this](bool hasCookies) {
        m_hasCookies = hasCookies;
        emit cookieStatusChanged();
    });

    connect(m_socketService, &MetubeSocketService::downloadAdded, this, [this](const QJsonObject &obj) {
        m_queue.append(MeTube::JsonUtils::parseDownloadItem(obj));
        emit dataChanged();
    });

    connect(m_socketService, &MetubeSocketService::downloadUpdated, this, [this](const QJsonObject &obj) {
        MeTube::DownloadItem item = MeTube::JsonUtils::parseDownloadItem(obj);
        bool found = false;
        
        for (int i = 0; i < m_queue.size(); ++i) {
            bool matches = false;
            if (!item.id.isEmpty() && !m_queue[i].id.isEmpty()) {
                matches = (m_queue[i].id == item.id);
            } else if (!item.url.isEmpty() && !m_queue[i].url.isEmpty()) {
                matches = (m_queue[i].url == item.url);
            }
            
            if (matches) {
                m_queue[i] = item;
                found = true;
                break;
            }
        }
        
        if (!found) {
            m_queue.append(item);
        }
        emit dataChanged();
    });

    connect(m_socketService, &MetubeSocketService::downloadCompleted, this, [this](const QJsonObject &obj) {
        MeTube::DownloadItem item = MeTube::JsonUtils::parseDownloadItem(obj);
        item.status = MeTube::DownloadStatus::Completed;
        item.statusText = "finished";
        item.percent = 100.0;
        
        // Move from queue to done (incremental, no REST call)
        for (int i = 0; i < m_queue.size(); ++i) {
            if (m_queue[i].id == item.id) {
                m_queue.removeAt(i);
                break;
            }
        }
        // Also check pending
        for (int i = 0; i < m_pending.size(); ++i) {
            if (m_pending[i].id == item.id) {
                m_pending.removeAt(i);
                break;
            }
        }
        m_done.prepend(item);
        emit downloadFinished(item.title);
        emit dataChanged();
    });

    connect(m_socketService, &MetubeSocketService::downloadCanceled, this, [this](const QString &id) {
        bool changed = false;
        for (int i = 0; i < m_queue.size(); ++i) {
            if (m_queue[i].id == id) {
                m_queue.removeAt(i);
                changed = true;
                break;
            }
        }
        // Also check pending list — backend can cancel pending items
        for (int i = 0; i < m_pending.size(); ++i) {
            if (m_pending[i].id == id) {
                m_pending.removeAt(i);
                changed = true;
                break;
            }
        }
        if (changed) emit dataChanged();
    });

    connect(m_socketService, &MetubeSocketService::downloadCleared, this, [this](const QString &id) {
        bool changed = false;
        for (int i = 0; i < m_done.size(); ++i) {
            if (m_done[i].id == id) {
                m_done.removeAt(i);
                changed = true;
                break;
            }
        }
        if (changed) emit dataChanged();
    });

    connect(m_socketService, &MetubeSocketService::errorOccurred, this, &DownloadManager::errorOccurred);
    connect(m_socketService, &MetubeSocketService::sslErrorsOccurred, this, &DownloadManager::errorOccurred);

    m_socketService->connectToServer();
}

void DownloadManager::refreshHistory() {
    m_restService->fetchHistory();
}

void DownloadManager::addDownload(const QString &url, 
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
    QVariantMap options = MeTube::RequestBuilder::buildAddDownload(url, type, quality, format, codec,
                                                                   folder, customNamePrefix, playlistItemLimit,
                                                                   autoStart, splitByChapters, chapterTemplate,
                                                                   subtitleLanguage, subtitleMode,
                                                                   ytdlOptionsPresets, ytdlOptionsOverrides);
    m_restService->addDownload(options);
}

void DownloadManager::startPending(const QStringList &ids) { m_restService->startPending(ids); }
void DownloadManager::cancelAdd() { m_restService->cancelAdd(); }
void DownloadManager::fetchPresets() { m_restService->fetchPresets(); }
void DownloadManager::fetchSubscriptions() { m_restService->fetchSubscriptions(); }
void DownloadManager::addSubscription(const QVariantMap &options) { m_restService->addSubscription(options); }
void DownloadManager::updateSubscription(const QString &id, const QVariantMap &changes) { m_restService->updateSubscription(id, changes); }
void DownloadManager::deleteSubscriptions(const QStringList &ids) { m_restService->deleteSubscriptions(ids); }
void DownloadManager::checkSubscriptions(const QStringList &ids) { m_restService->checkSubscriptions(ids); }
void DownloadManager::fetchCookieStatus() { m_restService->fetchCookieStatus(); }
void DownloadManager::deleteCookies() { m_restService->deleteCookies(); }

void DownloadManager::removeDownload(const QString &id, const QString &where) {
    QVariantMap options = MeTube::RequestBuilder::buildDeleteRequest({id}, where);
    m_restService->deleteDownload(options);
    
    // Optimistic UI update
    if (where == "queue") {
        for (int i = 0; i < m_queue.size(); ++i) {
            if (m_queue[i].id == id) {
                m_queue.removeAt(i);
                break;
            }
        }
    } else {
        for (int i = 0; i < m_done.size(); ++i) {
            if (m_done[i].id == id) {
                m_done.removeAt(i);
                break;
            }
        }
    }
    emit dataChanged();
}

void DownloadManager::onHistoryFetched(const QList<MeTube::DownloadItem> &done, 
                                      const QList<MeTube::DownloadItem> &queue, 
                                      const QList<MeTube::DownloadItem> &pending) {
    // Basic deduplication and merging
    // In a more advanced version, we'd preserve UI states for items
    m_done = done;
    m_queue = queue;
    m_pending = pending;
    
    emit dataChanged();
}
