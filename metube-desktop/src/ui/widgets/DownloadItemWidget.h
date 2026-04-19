#ifndef DOWNLOADITEMWIDGET_H
#define DOWNLOADITEMWIDGET_H

#include <QWidget>
#include <QProgressBar>
#include <QLabel>
#include "core/Models.h"

class DownloadItemWidget : public QWidget {
    Q_OBJECT
public:
    explicit DownloadItemWidget(const MeTube::DownloadItem &item, QWidget *parent = nullptr);

    void updateItem(const MeTube::DownloadItem &item);

signals:
    void deleteRequested(const QString &id);
    void openFileRequested(const MeTube::DownloadItem &item);

private:
    void setupUi();

    QLabel *m_titleLabel;
    QLabel *m_statusLabel;
    QProgressBar *m_progressBar;
    QLabel *m_speedLabel;
    QLabel *m_etaLabel;
    QLabel *m_detailsLabel;
    
    QString m_id;
    MeTube::DownloadItem m_item;
};

#endif // DOWNLOADITEMWIDGET_H
