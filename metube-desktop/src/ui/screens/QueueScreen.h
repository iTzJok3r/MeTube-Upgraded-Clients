#ifndef QUEUESCREEN_H
#define QUEUESCREEN_H

#include <QWidget>
#include <QLabel>
#include <QPushButton>
#include "viewmodels/DownloadViewModel.h"
#include "ui/widgets/DownloadListWidget.h"

class QueueScreen : public QWidget {
    Q_OBJECT
public:
    explicit QueueScreen(DownloadViewModel *viewModel, QWidget *parent = nullptr);

public slots:
    void refresh();

signals:
    void openFileRequested(const MeTube::DownloadItem &item);

private:
    DownloadViewModel *m_viewModel;
    
    QLabel *m_pendingHeader;
    QWidget *m_pendingControls;
    QPushButton *m_startPendingBtn;
    QPushButton *m_cancelPendingBtn;
    DownloadListWidget *m_pendingListWidget;

    QLabel *m_queueHeader;
    DownloadListWidget *m_listWidget;
    QLabel *m_emptyLabel;
};

#endif // QUEUESCREEN_H
