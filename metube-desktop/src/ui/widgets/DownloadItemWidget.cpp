#include "DownloadItemWidget.h"
#include <QVBoxLayout>
#include <QHBoxLayout>
#include <QProgressBar>
#include <QLabel>
#include <QPushButton>
#include <QDesktopServices>
#include <QUrl>

DownloadItemWidget::DownloadItemWidget(const MeTube::DownloadItem &item, QWidget *parent) 
    : QWidget(parent), m_item(item) {
    
    auto *layout = new QVBoxLayout(this);
    layout->setContentsMargins(10, 10, 10, 10);

    // Title and Status Row
    auto *topLayout = new QHBoxLayout();
    m_titleLabel = new QLabel(item.title.isEmpty() ? item.url : item.title, this);
    m_titleLabel->setStyleSheet("font-weight: bold; font-size: 14px;");
    
    m_statusLabel = new QLabel(item.statusText, this);
    m_statusLabel->setStyleSheet("color: #666;");
    
    topLayout->addWidget(m_titleLabel, 1);
    topLayout->addWidget(m_statusLabel);
    layout->addLayout(topLayout);

    // Progress Bar
    m_progressBar = new QProgressBar(this);
    m_progressBar->setRange(0, 100);
    m_progressBar->setValue(static_cast<int>(item.percent));
    m_progressBar->setFixedHeight(12);
    m_progressBar->setTextVisible(false);
    layout->addWidget(m_progressBar);

    // Details Row (Speed, ETA, etc.)
    auto *detailsLayout = new QHBoxLayout();
    m_detailsLabel = new QLabel(QString("%1 | %2 | %3").arg(item.size, item.speed, item.eta), this);
    m_detailsLabel->setStyleSheet("font-size: 11px; color: #888;");
    detailsLayout->addWidget(m_detailsLabel, 1);

    // Context Buttons (Open/Download)
    if (item.status == MeTube::DownloadStatus::Completed) {
        auto *openBtn = new QPushButton("Download", this);
        openBtn->setFixedWidth(100);
        connect(openBtn, &QPushButton::clicked, this, [this, item]() {
            emit openFileRequested(item);
        });
        detailsLayout->addWidget(openBtn);
    }
    
    auto *deleteBtn = new QPushButton("Remove", this);
    deleteBtn->setFixedWidth(80);
    deleteBtn->setStyleSheet("color: #d93025;");
    connect(deleteBtn, &QPushButton::clicked, this, [this, item]() {
        emit deleteRequested(item.id);
    });
    detailsLayout->addWidget(deleteBtn);
    
    layout->addLayout(detailsLayout);

    // Styling
    setStyleSheet("DownloadItemWidget { background-color: white; border-bottom: 1px solid #eee; }");
    if (item.status == MeTube::DownloadStatus::Failed) {
        m_statusLabel->setStyleSheet("color: #d93025; font-weight: bold;");
    }
}

void DownloadItemWidget::updateItem(const MeTube::DownloadItem &item) {
    m_item = item;
    m_titleLabel->setText(item.title.isEmpty() ? item.url : item.title);
    m_statusLabel->setText(item.statusText);
    m_progressBar->setValue(static_cast<int>(item.percent));
    m_detailsLabel->setText(QString("%1 | %2 | %3").arg(item.size, item.speed, item.eta));
}
