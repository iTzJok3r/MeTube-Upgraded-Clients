#include "HistoryScreen.h"
#include <QVBoxLayout>
#include <QLabel>
#include <QDesktopServices>
#include <QUrl>
#include <QHBoxLayout>
#include <QFileDialog>
#include <QFile>
#include <QTextStream>
#include <QMessageBox>
#include <QPushButton>
#include "../widgets/DownloadListWidget.h"
#include "../../viewmodels/DownloadViewModel.h"

HistoryScreen::HistoryScreen(DownloadViewModel *viewModel, QWidget *parent) 
    : QWidget(parent), m_viewModel(viewModel) {
    
    auto *layout = new QVBoxLayout(this);
    
    auto *header = new QLabel("Download History", this);
    header->setStyleSheet("font-size: 20px; font-weight: bold; margin-bottom: 10px;");
    
    auto *headerLayout = new QHBoxLayout();
    headerLayout->addWidget(header);
    headerLayout->addStretch();
    
    auto *exportBtn = new QPushButton("Export List", this);
    exportBtn->setStyleSheet("color: #1a73e8; border: 1px solid #1a73e8; border-radius: 4px; padding: 4px 12px; font-size: 11px;");
    connect(exportBtn, &QPushButton::clicked, this, [this]() {
        auto items = m_viewModel->getHistory();
        if (items.isEmpty()) {
            QMessageBox::information(this, "Export", "History is empty.");
            return;
        }
        
        QString fileName = QFileDialog::getSaveFileName(this, "Export History", "metube_history.txt", "Text Files (*.txt)");
        if (!fileName.isEmpty()) {
            QFile file(fileName);
            if (file.open(QIODevice::WriteOnly | QIODevice::Text)) {
                QTextStream out(&file);
                for (const auto &item : items) {
                    out << item.title << " | " << item.url << "\n";
                }
                file.close();
                QMessageBox::information(this, "Export", "History exported successfully.");
            }
        }
    });
    headerLayout->addWidget(exportBtn);
    layout->addLayout(headerLayout);
    
    // Empty state label
    m_emptyLabel = new QLabel("No completed downloads yet.", this);
    m_emptyLabel->setAlignment(Qt::AlignCenter);
    m_emptyLabel->setStyleSheet("color: #999; font-size: 14px; padding: 40px;");
    layout->addWidget(m_emptyLabel);
    
    m_listWidget = new DownloadListWidget(this);
    layout->addWidget(m_listWidget, 1);
    
    connect(m_listWidget, &DownloadListWidget::deleteRequested, this, [this](const QString &id) {
        m_viewModel->removeDownload(id, "done");
    });
    connect(m_listWidget, &DownloadListWidget::openFileRequested, this, &HistoryScreen::openFileRequested);
    
    connect(m_viewModel, &DownloadViewModel::downloadsUpdated, this, &HistoryScreen::refresh);
    
    refresh();
}

void HistoryScreen::refresh() {
    auto items = m_viewModel->getHistory();
    m_listWidget->updateList(items);
    m_emptyLabel->setVisible(items.isEmpty());
    m_listWidget->setVisible(!items.isEmpty());
}
