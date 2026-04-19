#ifndef HISTORYSCREEN_H
#define HISTORYSCREEN_H

#include <QWidget>
#include <QLabel>
#include "viewmodels/DownloadViewModel.h"
#include "ui/widgets/DownloadListWidget.h"

class HistoryScreen : public QWidget {
    Q_OBJECT
public:
    explicit HistoryScreen(DownloadViewModel *viewModel, QWidget *parent = nullptr);

public slots:
    void refresh();

signals:
    void openFileRequested(const MeTube::DownloadItem &item);

private:
    DownloadViewModel *m_viewModel;
    DownloadListWidget *m_listWidget;
    QLabel *m_emptyLabel;
};

#endif // HISTORYSCREEN_H
