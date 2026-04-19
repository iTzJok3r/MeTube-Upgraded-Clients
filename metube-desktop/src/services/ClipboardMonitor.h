#ifndef CLIPBOARDMONITOR_H
#define CLIPBOARDMONITOR_H

#include <QObject>
#include <QClipboard>
#include <QApplication>
#include <QRegularExpression>

class ClipboardMonitor : public QObject {
    Q_OBJECT
public:
    explicit ClipboardMonitor(QObject *parent = nullptr) : QObject(parent) {
        m_clipboard = QApplication::clipboard();
        connect(m_clipboard, &QClipboard::dataChanged, this, &ClipboardMonitor::checkClipboard);
    }

signals:
    void urlDetected(const QString &url);

private slots:
    void checkClipboard() {
        QString text = m_clipboard->text().trimmed();
        // Detect any HTTP/HTTPS URL — MeTube supports 1000+ sites via yt-dlp
        static QRegularExpression urlPattern("^https?://[^\\s]+$");
        if (urlPattern.match(text).hasMatch()) {
            emit urlDetected(text);
        }
    }

private:
    QClipboard *m_clipboard;
};

#endif // CLIPBOARDMONITOR_H
