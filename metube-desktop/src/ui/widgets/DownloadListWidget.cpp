#include "DownloadListWidget.h"

DownloadListWidget::DownloadListWidget(QWidget *parent) : QWidget(parent) {
    QVBoxLayout *mainLayout = new QVBoxLayout(this);
    mainLayout->setContentsMargins(0, 0, 0, 0);

    QScrollArea *scrollArea = new QScrollArea(this);
    scrollArea->setWidgetResizable(true);
    scrollArea->setFrameShape(QFrame::NoFrame);

    QWidget *container = new QWidget();
    m_listLayout = new QVBoxLayout(container);
    m_listLayout->setAlignment(Qt::AlignTop);
    m_listLayout->setSpacing(0);

    scrollArea->setWidget(container);
    mainLayout->addWidget(scrollArea);
}

void DownloadListWidget::updateList(const QList<MeTube::DownloadItem> &items) {
    QStringList currentIds;
    for (const auto &item : items) {
        currentIds.append(item.id);
        if (m_widgets.contains(item.id)) {
            m_widgets[item.id]->updateItem(item);
        } else {
            DownloadItemWidget *w = new DownloadItemWidget(item, this);
            connect(w, &DownloadItemWidget::deleteRequested, this, &DownloadListWidget::deleteRequested);
            connect(w, &DownloadItemWidget::openFileRequested, this, &DownloadListWidget::openFileRequested);
            m_listLayout->addWidget(w);
            m_widgets[item.id] = w;
        }
    }

    QMutableMapIterator<QString, DownloadItemWidget*> i(m_widgets);
    while (i.hasNext()) {
        i.next();
        if (!currentIds.contains(i.key())) {
            i.value()->deleteLater();
            i.remove();
        }
    }
}
