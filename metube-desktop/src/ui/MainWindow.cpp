#include "MainWindow.h"
#include <QHBoxLayout>
#include <QVBoxLayout>
#include <QApplication>
#include <QStatusBar>
#include <QTimer>
#include <QIcon>
#include <QLabel>
#include "services/SystemTrayManager.h"
#include "services/ClipboardMonitor.h"
#include "services/NativeNotificationService.h"
#include <QDesktopServices>
#include <QUrl>
#include <QMessageBox>
#include "screens/SubscriptionsScreen.h"

MainWindow::MainWindow(QWidget *parent) : QMainWindow(parent) {
    m_settings = new AppSettings(this);
    m_manager = new DownloadManager(m_settings, this);
    m_viewModel = new DownloadViewModel(m_manager, this);
    
    // Desktop Integration
    m_tray = new SystemTrayManager(this, this);
    m_clipboardMonitor = new ClipboardMonitor(this);
    m_notifications = new NativeNotificationService(m_tray, this);

    setupUi();
    setWindowTitle("MeTube Desktop Upgraded");
    setWindowIcon(QIcon(":/icons/app.png"));
    resize(1000, 700);

    // Context connections
    connect(m_manager, &DownloadManager::errorOccurred, m_notifications, &NativeNotificationService::notifyError);
    connect(m_manager, &DownloadManager::downloadFinished, m_notifications, [this](const QString &title) {
        m_notifications->notifyCompletion(title + " is now ready to open/download on your local machine.");
    });
    
    connect(m_clipboardMonitor, &ClipboardMonitor::urlDetected, this, [this](const QString &url) {
        statusBar()->showMessage("URL Detected in clipboard: " + url, 5000);
    });

    auto onOpenFile = [this](const MeTube::DownloadItem &item) {
        if (item.filename.isEmpty()) {
            QMessageBox::warning(this, "Download Unavailable", "No filename available for this download.");
            return;
        }
        QString serverUrl = m_settings->serverUrl();
        if (!serverUrl.endsWith('/')) serverUrl += '/';

        // Match MeTube web UI 'buildDownloadLink' logic:
        // audio → audio_download/, else → download/
        QString route = (item.type == "audio" || item.filename.endsWith(".mp3", Qt::CaseInsensitive))
                            ? "audio_download/" : "download/";
        QString downloadUrl = serverUrl + route;

        // Include folder path segments (each individually encoded)
        if (!item.folder.isEmpty()) {
            QStringList segments = item.folder.split("/", Qt::SkipEmptyParts);
            for (const auto &seg : segments) {
                downloadUrl += QUrl::toPercentEncoding(seg) + "/";
            }
        }

        downloadUrl += QUrl::toPercentEncoding(item.filename);
        QDesktopServices::openUrl(QUrl(downloadUrl));
    };

    auto *queueScreen = qobject_cast<QueueScreen*>(m_stack->widget(1));
    if (queueScreen) connect(queueScreen, &QueueScreen::openFileRequested, this, onOpenFile);

    auto *historyScreen = qobject_cast<HistoryScreen*>(m_stack->widget(2));
    if (historyScreen) connect(historyScreen, &HistoryScreen::openFileRequested, this, onOpenFile);

    // Wire Screens
    auto *home = qobject_cast<HomeScreen*>(m_stack->widget(0));
    if (home) {
        connect(home, &HomeScreen::downloadRequested, this, [this](const QString &url, 
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
            m_viewModel->addDownload(url, type, quality, format, codec, folder, customNamePrefix, playlistItemLimit,
                                     autoStart, splitByChapters, chapterTemplate, subtitleLanguage, subtitleMode,
                                     ytdlOptionsPresets, ytdlOptionsOverrides);
            m_sidebar->setCurrentRow(1);
            statusBar()->showMessage("Download request sent...", 3000);
        });

        // Show user feedback when the REST /add call completes
        connect(m_viewModel, &DownloadViewModel::addDownloadResult, this, [this](bool success, const QString &msg) {
            if (success) {
                statusBar()->showMessage("Download queued successfully", 3000);
            } else {
                QMessageBox::warning(this, "Download Failed", msg);
                statusBar()->showMessage("Download failed: " + msg, 5000);
            }
        });

        home->setCustomDirs(m_viewModel->getCustomDirs());
        connect(m_viewModel, &DownloadViewModel::customDirsUpdated, home, [this, home]() {
            home->setCustomDirs(m_viewModel->getCustomDirs());
        });
    }

    // Connection status indicator in status bar
    auto *statusLabel = new QLabel("Connecting...", this);
    statusLabel->setStyleSheet("color: #f57c00; font-size: 12px; padding: 2px 8px;");
    statusBar()->addPermanentWidget(statusLabel);
    
    connect(m_manager, &DownloadManager::connectionStatusChanged, this, [statusLabel](bool connected) {
        if (connected) {
            statusLabel->setText("● Connected");
            statusLabel->setStyleSheet("color: #28a745; font-size: 12px; padding: 2px 8px;");
        } else {
            statusLabel->setText("● Disconnected");
            statusLabel->setStyleSheet("color: #dc3545; font-size: 12px; padding: 2px 8px;");
        }
    });

    connect(m_manager, &DownloadManager::socketConnecting, this, [statusLabel]() {
        statusLabel->setText("● Connecting...");
        statusLabel->setStyleSheet("color: #f57c00; font-size: 12px; padding: 2px 8px;");
    });

    // Initial load
    m_manager->refreshHistory();
}

MainWindow::~MainWindow() {
}

void MainWindow::setupUi() {
    QWidget *centralWidget = new QWidget(this);
    QHBoxLayout *mainLayout = new QHBoxLayout(centralWidget);
    mainLayout->setContentsMargins(0, 0, 0, 0);
    mainLayout->setSpacing(0);

    // Sidebar
    m_sidebar = new QListWidget(this);
    m_sidebar->setFixedWidth(200);
    m_sidebar->addItems({"Home", "Queue", "History", "Subscriptions", "Settings"});
    m_sidebar->setCurrentRow(0);
    m_sidebar->setStyleSheet("QListWidget { background-color: #f3f3f3; border-right: 1px solid #ddd; padding-top: 20px; font-size: 14px; outline: none; }"
                             "QListWidget::item { padding: 15px; border-left: 4px solid transparent; }"
                             "QListWidget::item:selected { background-color: #e5e5e5; color: #0078d4; border-left: 4px solid #0078d4; }");

    // Stack
    m_stack = new QStackedWidget(this);
    m_stack->addWidget(new HomeScreen(this));
    m_stack->addWidget(new QueueScreen(m_viewModel, this));
    m_stack->addWidget(new HistoryScreen(m_viewModel, this));
    m_stack->addWidget(new SubscriptionsScreen(m_viewModel, this));
    m_stack->addWidget(new SettingsScreen(m_settings, m_viewModel, this));

    mainLayout->addWidget(m_sidebar);
    mainLayout->addWidget(m_stack, 1);

    setCentralWidget(centralWidget);

    connect(m_sidebar, &QListWidget::currentRowChanged, m_stack, &QStackedWidget::setCurrentIndex);
}

void MainWindow::onNavigationChanged(int index) {
    m_stack->setCurrentIndex(index);
}
