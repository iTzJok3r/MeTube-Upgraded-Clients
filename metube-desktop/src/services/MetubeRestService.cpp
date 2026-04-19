#include "MetubeRestService.h"
#include <QJsonDocument>
#include <QJsonObject>
#include <QJsonArray>
#include <QUrlQuery>
#include "utils/JsonUtils.h"

using namespace MeTube;

MetubeRestService::MetubeRestService(const QString &baseUrl, QObject *parent)
    : QObject(parent), m_baseUrl(baseUrl) {
    m_networkManager = new QNetworkAccessManager(this);
    
    if (!m_baseUrl.endsWith("/")) {
        m_baseUrl += "/";
    }
}

void MetubeRestService::fetchHistory() {
    QUrl url(m_baseUrl + "history");
    QNetworkRequest request(url);
    QNetworkReply *reply = m_networkManager->get(request);
    connect(reply, &QNetworkReply::finished, this, [this, reply]() { onHistoryFinished(reply); });
}

void MetubeRestService::addDownload(const QVariantMap &options) {
    QUrl url(m_baseUrl + "add");
    QNetworkRequest request(url);
    request.setHeader(QNetworkRequest::ContentTypeHeader, "application/json");
    
    QJsonDocument doc = QJsonDocument::fromVariant(options);
    QNetworkReply *reply = m_networkManager->post(request, doc.toJson());
    connect(reply, &QNetworkReply::finished, this, [this, reply]() { onAddFinished(reply); });
}

void MetubeRestService::deleteDownload(const QVariantMap &payload) {
    QUrl url(m_baseUrl + "delete");
    QNetworkRequest request(url);
    request.setHeader(QNetworkRequest::ContentTypeHeader, "application/json");
    
    QJsonDocument doc = QJsonDocument::fromVariant(payload);
    QNetworkReply *reply = m_networkManager->post(request, doc.toJson());
    connect(reply, &QNetworkReply::finished, reply, &QNetworkReply::deleteLater);
}

void MetubeRestService::onHistoryFinished(QNetworkReply *reply) {
    reply->deleteLater();
    if (reply->error() != QNetworkReply::NoError) {
        emit errorOccurred("Connection failed: " + reply->errorString());
        return;
    }

    QByteArray data = reply->readAll();
    QJsonDocument doc = QJsonDocument::fromJson(data);
    if (!doc.isObject()) {
        emit errorOccurred("Invalid backend response");
        return;
    }

    QJsonObject root = doc.object();
    QList<DownloadItem> done, queue, pending;

    auto parseList = [this](const QJsonArray &arr, QList<DownloadItem> &list) {
        for (const auto &val : arr) {
            if (val.isObject()) {
                list.append(parseDownloadItem(val.toObject()));
            }
        }
    };

    parseList(root["done"].toArray(), done);
    parseList(root["queue"].toArray(), queue);
    parseList(root["pending"].toArray(), pending);

    emit historyFetched(done, queue, pending);
}

void MetubeRestService::onAddFinished(QNetworkReply *reply) {
    QByteArray data = reply->readAll();
    bool success = (reply->error() == QNetworkReply::NoError);
    QString msg = success ? "Request submitted" : reply->errorString();
    
    if (!success && !data.isEmpty()) {
        QJsonDocument doc = QJsonDocument::fromJson(data);
        if (doc.isObject() && doc.object().contains("error")) {
            msg = doc.object()["error"].toString();
        }
    }
    
    emit addDownloadFinished(success, msg);
    reply->deleteLater();
}

DownloadItem MetubeRestService::parseDownloadItem(const QJsonObject &obj) {
    DownloadItem item;
    item.id = JsonUtils::getString(obj, "id");
    item.title = JsonUtils::getString(obj, "title");
    item.url = JsonUtils::getString(obj, "url");
    item.quality = JsonUtils::getString(obj, "quality");
    item.type = JsonUtils::getString(obj, "download_type");
    item.codec = JsonUtils::getString(obj, "codec");
    item.format = JsonUtils::getString(obj, "format");
    item.folder = JsonUtils::getString(obj, "folder");
    item.filename = JsonUtils::getString(obj, "filename");
    
    item.statusText = JsonUtils::getString(obj, "status");
    item.status = DownloadItem::stringToStatus(item.statusText);
    
    item.percent = JsonUtils::getDouble(obj, "percent");
    item.speed = JsonUtils::getString(obj, "speed");
    item.eta = JsonUtils::getString(obj, "eta");
    item.size = JsonUtils::getString(obj, "size");
    item.error = JsonUtils::getString(obj, "error");
    item.timestamp = JsonUtils::getLong(obj, "timestamp");
    
    return item;
}

void MetubeRestService::startPending(const QStringList &ids) {
    QJsonObject obj;
    if (!ids.isEmpty()) obj["ids"] = QJsonArray::fromStringList(ids);
    postGeneric("start", obj, "Pending downloads started");
}

void MetubeRestService::cancelAdd() {
    postGeneric("cancel-add", QJsonObject(), "Add cancelled");
}

void MetubeRestService::fetchPresets() {
    QUrl url(m_baseUrl + "presets");
    QNetworkRequest request(url);
    QNetworkReply *reply = m_networkManager->get(request);
    connect(reply, &QNetworkReply::finished, this, [this, reply]() {
        if (reply->error() == QNetworkReply::NoError) {
            QJsonDocument doc = QJsonDocument::fromJson(reply->readAll());
            QJsonArray arr = doc.object()["presets"].toArray();
            QStringList presets;
            for (const auto &val : arr) presets.append(val.toString());
            emit presetsFetched(presets);
        }
        reply->deleteLater();
    });
}

void MetubeRestService::fetchSubscriptions() {
    QUrl url(m_baseUrl + "subscriptions");
    QNetworkRequest request(url);
    QNetworkReply *reply = m_networkManager->get(request);
    connect(reply, &QNetworkReply::finished, this, [this, reply]() {
        if (reply->error() == QNetworkReply::NoError) {
            QJsonDocument doc = QJsonDocument::fromJson(reply->readAll());
            QJsonArray arr = doc.array();
            QList<QVariantMap> subs;
            for (const auto &val : arr) subs.append(val.toObject().toVariantMap());
            emit subscriptionsFetched(subs);
        }
        reply->deleteLater();
    });
}

void MetubeRestService::addSubscription(const QVariantMap &options) {
    postGeneric("subscribe", QJsonObject::fromVariantMap(options), "Subscription added");
}

void MetubeRestService::updateSubscription(const QString &id, const QVariantMap &changes) {
    QJsonObject obj = QJsonObject::fromVariantMap(changes);
    obj["id"] = id;
    postGeneric("subscriptions/update", obj, "Subscription updated");
}

void MetubeRestService::deleteSubscriptions(const QStringList &ids) {
    QJsonObject obj;
    obj["ids"] = QJsonArray::fromStringList(ids);
    postGeneric("subscriptions/delete", obj, "Subscriptions deleted");
}

void MetubeRestService::checkSubscriptions(const QStringList &ids) {
    QJsonObject obj;
    if (!ids.isEmpty()) obj["ids"] = QJsonArray::fromStringList(ids);
    postGeneric("subscriptions/check", obj, "Subscriptions check triggered");
}

void MetubeRestService::fetchCookieStatus() {
    QUrl url(m_baseUrl + "cookie-status");
    QNetworkRequest request(url);
    QNetworkReply *reply = m_networkManager->get(request);
    connect(reply, &QNetworkReply::finished, this, [this, reply]() {
        if (reply->error() == QNetworkReply::NoError) {
            QJsonDocument doc = QJsonDocument::fromJson(reply->readAll());
            emit cookieStatusFetched(doc.object()["has_cookies"].toBool());
        }
        reply->deleteLater();
    });
}

void MetubeRestService::deleteCookies() {
    postGeneric("delete-cookies", QJsonObject(), "Cookies deleted");
}

void MetubeRestService::postGeneric(const QString &endpoint, const QJsonObject &payload, const QString &successMsg) {
    QUrl url(m_baseUrl + endpoint);
    QNetworkRequest request(url);
    request.setHeader(QNetworkRequest::ContentTypeHeader, "application/json");
    QJsonDocument doc(payload);
    QNetworkReply *reply = m_networkManager->post(request, doc.toJson());
    connect(reply, &QNetworkReply::finished, this, [this, reply, successMsg]() {
        bool success = (reply->error() == QNetworkReply::NoError);
        QString msg = success ? successMsg : reply->errorString();
        if (!success) {
            QJsonDocument errDoc = QJsonDocument::fromJson(reply->readAll());
            if (errDoc.isObject() && errDoc.object().contains("error")) {
                msg = errDoc.object()["error"].toString();
            }
        }
        emit genericActionFinished(success, msg);
        reply->deleteLater();
    });
}
