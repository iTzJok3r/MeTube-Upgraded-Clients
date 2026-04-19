#include "QueueScreen.h"
#include <QVBoxLayout>
#include <QHBoxLayout>
#include <QLabel>
#include <QPushButton>
#include "../widgets/DownloadListWidget.h"
#include "../../viewmodels/DownloadViewModel.h"

QueueScreen::QueueScreen(DownloadViewModel *viewModel, QWidget *parent) 
    : QWidget(parent), m_viewModel(viewModel) {
    
    auto *layout = new QVBoxLayout(this);
    
    // Pending Section
    m_pendingHeader = new QLabel("Pending Downloads", this);
    m_pendingHeader->setStyleSheet("font-size: 18px; font-weight: bold; margin-bottom: 5px;");
    layout->addWidget(m_pendingHeader);
    
    m_pendingControls = new QWidget(this);
    auto *pendingControlsLayout = new QHBoxLayout(m_pendingControls);
    pendingControlsLayout->setContentsMargins(0, 0, 0, 0);

    m_startPendingBtn = new QPushButton("Start All Pending", this);
    m_startPendingBtn->setStyleSheet("background-color: #28a745; color: white; padding: 6px 12px; border-radius: 4px;");
    m_cancelPendingBtn = new QPushButton("Cancel All Pending", this);
    m_cancelPendingBtn->setStyleSheet("background-color: #dc3545; color: white; padding: 6px 12px; border-radius: 4px;");

    pendingControlsLayout->addWidget(m_startPendingBtn);
    pendingControlsLayout->addWidget(m_cancelPendingBtn);
    pendingControlsLayout->addStretch();
    layout->addWidget(m_pendingControls);

    m_pendingListWidget = new DownloadListWidget(this);
    layout->addWidget(m_pendingListWidget, 1);
    
    // Queue Section
    m_queueHeader = new QLabel("Active Queue", this);
    m_queueHeader->setStyleSheet("font-size: 20px; font-weight: bold; margin-top: 15px; margin-bottom: 5px;");
    layout->addWidget(m_queueHeader);
    
    m_emptyLabel = new QLabel("No active downloads.\nAdd a URL from the Home tab to start downloading.", this);
    m_emptyLabel->setAlignment(Qt::AlignCenter);
    m_emptyLabel->setStyleSheet("color: #999; font-size: 14px; padding: 40px;");
    layout->addWidget(m_emptyLabel);
    
    m_listWidget = new DownloadListWidget(this);
    layout->addWidget(m_listWidget, 2);
    
    // Actions
    connect(m_listWidget, &DownloadListWidget::deleteRequested, this, [this](const QString &id) {
        m_viewModel->removeDownload(id, "queue");
    });
    connect(m_pendingListWidget, &DownloadListWidget::deleteRequested, this, [this](const QString &id) {
        m_viewModel->removeDownload(id, "pending");
    });

    connect(m_startPendingBtn, &QPushButton::clicked, this, [this]() {
        m_viewModel->startPending(); // No IDs = start all
    });
    connect(m_cancelPendingBtn, &QPushButton::clicked, this, [this]() {
        m_viewModel->cancelAdd(); // Cancel all
    });

    connect(m_listWidget, &DownloadListWidget::openFileRequested, this, &QueueScreen::openFileRequested);
    connect(m_pendingListWidget, &DownloadListWidget::openFileRequested, this, &QueueScreen::openFileRequested);
    
    connect(m_viewModel, &DownloadViewModel::downloadsUpdated, this, &QueueScreen::refresh);
    
    refresh();
}

void QueueScreen::refresh() {
    auto queueItems = m_viewModel->getQueue();
    auto pendingItems = m_viewModel->getPending();
    
    m_listWidget->updateList(queueItems);
    m_emptyLabel->setVisible(queueItems.isEmpty());
    m_listWidget->setVisible(!queueItems.isEmpty());

    m_pendingListWidget->updateList(pendingItems);
    bool hasPending = !pendingItems.isEmpty();
    m_pendingHeader->setVisible(hasPending);
    m_pendingControls->setVisible(hasPending);
    m_pendingListWidget->setVisible(hasPending);
}
