#ifndef APPSETTINGS_H
#define APPSETTINGS_H

#include <QObject>
#include <QSettings>

class AppSettings : public QObject {
    Q_OBJECT
public:
    explicit AppSettings(QObject *parent = nullptr);

    QString serverUrl() const;
    void setServerUrl(const QString &url);

    // Add other settings here as needed (default quality, etc)

signals:
    void serverUrlChanged(const QString &newUrl);

private:
    QSettings m_settings;
    static const QString KEY_SERVER_URL;
};

#endif // APPSETTINGS_H
