#ifndef MAINWINDOW_H
#define MAINWINDOW_H

#include <QMainWindow>
#include <QStackedWidget>
#include <QListWidget>

#include "core/AppSettings.h"
#include "core/DownloadManager.h"
#include "viewmodels/DownloadViewModel.h"

// Screens
#include "screens/HomeScreen.h"
#include "screens/QueueScreen.h"
#include "screens/HistoryScreen.h"
#include "screens/SettingsScreen.h"

class MainWindow : public QMainWindow {
    Q_OBJECT
public:
    MainWindow(QWidget *parent = nullptr);
    ~MainWindow();

private slots:
    void onNavigationChanged(int index);

private:
    void setupUi();

    AppSettings *m_settings;
    DownloadManager *m_manager;
    DownloadViewModel *m_viewModel;

    class SystemTrayManager *m_tray;
    class ClipboardMonitor *m_clipboardMonitor;
    class NativeNotificationService *m_notifications;

    QListWidget *m_sidebar;
    QStackedWidget *m_stack;
};

#endif // MAINWINDOW_H
