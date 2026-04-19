#ifndef NATIVENOTIFICATIONSERVICE_H
#define NATIVENOTIFICATIONSERVICE_H

#include <QObject>
#include <QString>
#include "SystemTrayManager.h"

class NativeNotificationService : public QObject {
    Q_OBJECT
public:
    explicit NativeNotificationService(class SystemTrayManager *tray, QObject *parent = nullptr)
        : QObject(parent), m_tray(tray) {}

public slots:
    void notifyCompletion(const QString &title) {
        if (m_tray) {
            m_tray->showNotification("Download Complete", title + " has been successfully downloaded.");
        }
    }

    void notifyError(const QString &error) {
        if (m_tray) {
            m_tray->showNotification("MeTube Error", error);
        }
    }

private:
    class SystemTrayManager *m_tray;
};

#endif // NATIVENOTIFICATIONSERVICE_H
