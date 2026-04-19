#ifndef REQUESTBUILDER_H
#define REQUESTBUILDER_H

#include <QVariant>
#include <QString>

namespace MeTube {

class RequestBuilder {
public:
    static QVariantMap buildAddDownload(const QString &url, 
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

    enum class Preset {
        Default,
        iOSCompatible,
        HighEfficiency,
        Fast720p,
        AudioOnly
    };
    static QVariantMap buildAddWithPreset(const QString &url, Preset preset);
    
    static QVariantMap buildDeleteRequest(const QStringList &ids, const QString &where);
    
    static QVariantMap buildSubscriptionAdd(const QString &url, 
                                           int checkInterval = 60,
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

    // Add other payload builders here
};

} // namespace MeTube

#endif // REQUESTBUILDER_H
