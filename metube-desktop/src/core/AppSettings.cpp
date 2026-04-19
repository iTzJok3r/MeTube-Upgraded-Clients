#include "AppSettings.h"

const QString AppSettings::KEY_SERVER_URL = "server/url";

AppSettings::AppSettings(QObject *parent) : QObject(parent) {
}

QString AppSettings::serverUrl() const {
    return m_settings.value(KEY_SERVER_URL, "http://localhost:8081").toString();
}

void AppSettings::setServerUrl(const QString &url) {
    if (serverUrl() != url) {
        m_settings.setValue(KEY_SERVER_URL, url);
        emit serverUrlChanged(url);
    }
}
