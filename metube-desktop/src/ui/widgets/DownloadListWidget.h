#ifndef DOWNLOADLISTWIDGET_H
#define DOWNLOADLISTWIDGET_H

#include <QWidget>
#include <QVBoxLayout>
#include <QScrollArea>
#include <QMap>
#include "core/Models.h"
#include "DownloadItemWidget.h"

class DownloadListWidget : public QWidget {
    Q_OBJECT
public:
    explicit DownloadListWidget(QWidget *parent = nullptr);

    void updateList(const QList<MeTube::DownloadItem> &items);

signals:
    void deleteRequested(const QString &id);
    void openFileRequested(const MeTube::DownloadItem &item);

private:
    QVBoxLayout *m_listLayout;
    QMap<QString, DownloadItemWidget*> m_widgets;
};

#endif // DOWNLOADLISTWIDGET_H
