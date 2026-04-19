#include "JsonUtils.h"
#include <QJsonValue>
#include <QJsonArray>

namespace MeTube {
namespace JsonUtils {

MeTube::DownloadItem parseDownloadItem(const QJsonObject& obj) {
    MeTube::DownloadItem item;
    item.id = getString(obj, "id");
    if (item.id.isEmpty()) item.id = getString(obj, "_id"); // Backend sometimes uses _id
    item.url = getString(obj, "url");
    item.title = getString(obj, "title", item.url);
    item.statusText = getString(obj, "status");
    item.status = MeTube::DownloadItem::stringToStatus(item.statusText);
    item.percent = getDouble(obj, "percent");
    item.speed = getString(obj, "speed");
    item.eta = getString(obj, "eta");
    item.filename = getString(obj, "filename");
    item.folder = getString(obj, "folder");
    item.size = getString(obj, "size");
    item.error = getString(obj, "error");
    item.timestamp = getLong(obj, "timestamp");
    item.type = getString(obj, "download_type");
    item.quality = getString(obj, "quality");
    item.codec = getString(obj, "codec");
    item.format = getString(obj, "format");
    return item;
}

HistoryPayload parseHistoryPayload(const QJsonObject& obj) {
    HistoryPayload payload;
    
    auto parseList = [](const QJsonArray& arr) {
        QList<MeTube::DownloadItem> list;
        for (const auto& val : arr) {
            if (val.isObject()) {
                list.append(parseDownloadItem(val.toObject()));
            }
        }
        return list;
    };

    if (obj.contains("done") && obj["done"].isArray()) 
        payload.done = parseList(obj["done"].toArray());
    if (obj.contains("queue") && obj["queue"].isArray()) 
        payload.queue = parseList(obj["queue"].toArray());
    if (obj.contains("pending") && obj["pending"].isArray()) 
        payload.pending = parseList(obj["pending"].toArray());
        
    return payload;
}

QString getString(const QJsonObject& obj, const QString& key, const QString& defaultValue) {
    if (obj.contains(key) && obj[key].isString()) {
        return obj[key].toString();
    }
    return defaultValue;
}

int getInt(const QJsonObject& obj, const QString& key, int defaultValue) {
    if (obj.contains(key)) {
        if (obj[key].isDouble()) return obj[key].toInt();
        if (obj[key].isString()) return obj[key].toString().toInt();
    }
    return defaultValue;
}

double getDouble(const QJsonObject& obj, const QString& key, double defaultValue) {
    if (obj.contains(key)) {
        if (obj[key].isDouble()) return obj[key].toDouble();
        if (obj[key].isString()) return obj[key].toString().toDouble();
    }
    return defaultValue;
}

bool getBool(const QJsonObject& obj, const QString& key, bool defaultValue) {
    if (obj.contains(key)) {
        if (obj[key].isBool()) return obj[key].toBool();
        if (obj[key].isString()) {
            QString s = obj[key].toString().toLower();
            return (s == "true" || s == "1" || s == "on");
        }
    }
    return defaultValue;
}

qint64 getLong(const QJsonObject& obj, const QString& key, qint64 defaultValue) {
    if (obj.contains(key)) {
        if (obj[key].isDouble()) return static_cast<qint64>(obj[key].toDouble());
        if (obj[key].isString()) return obj[key].toString().toLongLong();
    }
    return defaultValue;
}

} // namespace JsonUtils
} // namespace MeTube
