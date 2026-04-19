#ifndef JSONUTILS_H
#define JSONUTILS_H

#include <QJsonObject>
#include <QString>
#include <QVariant>

#include "core/Models.h"

namespace MeTube {
namespace JsonUtils {

MeTube::DownloadItem parseDownloadItem(const QJsonObject& obj);

struct HistoryPayload {
    QList<MeTube::DownloadItem> done;
    QList<MeTube::DownloadItem> queue;
    QList<MeTube::DownloadItem> pending;
};
HistoryPayload parseHistoryPayload(const QJsonObject& obj);

QString getString(const QJsonObject& obj, const QString& key, const QString& defaultValue = "");
int getInt(const QJsonObject& obj, const QString& key, int defaultValue = 0);
double getDouble(const QJsonObject& obj, const QString& key, double defaultValue = 0.0);
bool getBool(const QJsonObject& obj, const QString& key, bool defaultValue = false);
qint64 getLong(const QJsonObject& obj, const QString& key, qint64 defaultValue = 0);

} // namespace JsonUtils
} // namespace MeTube

#endif // JSONUTILS_H
