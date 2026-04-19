#ifndef SYSTEMTRAYMANAGER_H
#define SYSTEMTRAYMANAGER_H

#include <QObject>
#include <QCoreApplication>
#include <QSystemTrayIcon>
#include <QMenu>
#include <QAction>
#include <QApplication>

class SystemTrayManager : public QObject {
    Q_OBJECT
public:
    explicit SystemTrayManager(QWidget *mainWindow, QObject *parent = nullptr)
        : QObject(parent), m_mainWindow(mainWindow) {
        
        m_trayIcon = new QSystemTrayIcon(QIcon(":/icons/app.png"), this);
        
        auto *menu = new QMenu(mainWindow);  // Parent to mainWindow for proper cleanup
        auto *showAction = menu->addAction("Show/Hide");
        menu->addSeparator();
        auto *quitAction = menu->addAction("Quit MeTube");

        m_trayIcon->setContextMenu(menu);

        connect(showAction, &QAction::triggered, this, [this]() {
            if (m_mainWindow->isVisible()) m_mainWindow->hide();
            else { m_mainWindow->show(); m_mainWindow->activateWindow(); }
        });

        connect(quitAction, &QAction::triggered, qApp, &QCoreApplication::quit);
        
        m_trayIcon->show();
    }

    void showNotification(const QString &title, const QString &message) {
        m_trayIcon->showMessage(title, message, QSystemTrayIcon::Information, 5000);
    }

private:
    QSystemTrayIcon *m_trayIcon;
    QWidget *m_mainWindow;
};

#endif // SYSTEMTRAYMANAGER_H
