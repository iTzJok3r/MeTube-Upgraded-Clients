#ifndef MODELS_H
#define MODELS_H

#include <QString>
#include <QList>
#include <QMetaType>

namespace MeTube {

enum class DownloadStatus {
    Pending,
    Queued,
    Downloading,
    Transcoding,
    Completed,
    Failed,
    Canceled,
    Unknown
};

struct DownloadItem {
    QString id;
    QString url;
    QString title;
    QString statusText;
    DownloadStatus status;
    double percent;
    QString speed;
    QString eta;
    QString filename;
    QString folder;
    QString size;
    QString error;
    qint64 timestamp;

    QString type; // video, audio
    QString quality;
    QString codec;
    QString format;

    static DownloadStatus stringToStatus(const QString &s) {
        QString lower = s.toLower();
        if (lower == "finished" || lower == "completed") return DownloadStatus::Completed;
        if (lower == "failed" || lower == "error") return DownloadStatus::Failed;
        if (lower == "downloading") return DownloadStatus::Downloading;
        if (lower == "transcoding") return DownloadStatus::Transcoding;
        if (lower == "queued") return DownloadStatus::Queued;
        if (lower == "canceled") return DownloadStatus::Canceled;
        return DownloadStatus::Pending;
    }
};

} // namespace MeTube

Q_DECLARE_METATYPE(MeTube::DownloadItem)
Q_DECLARE_METATYPE(QList<MeTube::DownloadItem>)

#endif // MODELS_H
