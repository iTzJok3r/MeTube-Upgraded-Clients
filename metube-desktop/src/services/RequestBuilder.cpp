#include "RequestBuilder.h"

namespace MeTube {

QVariantMap RequestBuilder::buildAddDownload(const QString &url, 
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
    QVariantMap map;
    map["url"] = url;
    map["download_type"] = type;
    map["quality"] = quality;
    map["auto_start"] = autoStart;
    
    if (!format.isEmpty()) map["format"] = format;
    if (!codec.isEmpty()) map["codec"] = codec;
    if (!folder.isEmpty()) map["folder"] = folder;
    if (!customNamePrefix.isEmpty()) map["custom_name_prefix"] = customNamePrefix;
    if (playlistItemLimit > 0) map["playlist_item_limit"] = playlistItemLimit;
    if (splitByChapters) map["split_by_chapters"] = splitByChapters;
    if (!chapterTemplate.isEmpty()) map["chapter_template"] = chapterTemplate;
    if (!subtitleLanguage.isEmpty()) map["subtitle_language"] = subtitleLanguage;
    if (!subtitleMode.isEmpty()) map["subtitle_mode"] = subtitleMode;
    if (!ytdlOptionsPresets.isEmpty()) map["ytdl_options_presets"] = ytdlOptionsPresets;
    if (!ytdlOptionsOverrides.isEmpty()) map["ytdl_options_overrides"] = ytdlOptionsOverrides;
    
    return map;
}

QVariantMap RequestBuilder::buildDeleteRequest(const QStringList &ids, const QString &where) {
    QVariantMap map;
    map["ids"] = ids;
    map["where"] = where;
    return map;
}

QVariantMap RequestBuilder::buildSubscriptionAdd(const QString &url, 
                                           int checkInterval,
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
    QVariantMap map = buildAddDownload(url, type, quality, format, codec, folder, customNamePrefix, 
                                       playlistItemLimit, autoStart, splitByChapters, chapterTemplate, 
                                       subtitleLanguage, subtitleMode, ytdlOptionsPresets, ytdlOptionsOverrides);
    map["check_interval_minutes"] = checkInterval;
    return map;
}

QVariantMap RequestBuilder::buildAddWithPreset(const QString &url, Preset preset) {
    switch (preset) {
        case Preset::iOSCompatible:
            return buildAddDownload(url, "video", "best", "mp4", "h264");
        case Preset::HighEfficiency:
            return buildAddDownload(url, "video", "best", "mkv", "h265");
        case Preset::Fast720p:
            return buildAddDownload(url, "video", "720", "mp4", "h264");
        case Preset::AudioOnly:
            return buildAddDownload(url, "audio", "best");
        default:
            return buildAddDownload(url);
    }
}

} // namespace MeTube
