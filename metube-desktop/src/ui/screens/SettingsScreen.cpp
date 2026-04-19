#include "SettingsScreen.h"
#include <QVBoxLayout>
#include <QFormLayout>
#include <QLineEdit>
#include <QPushButton>
#include <QGroupBox>
#include <QMessageBox>
#include "../../core/AppSettings.h"

SettingsScreen::SettingsScreen(AppSettings *settings, DownloadViewModel *viewModel, QWidget *parent) 
    : QWidget(parent), m_settings(settings), m_viewModel(viewModel) {
    
    auto *layout = new QVBoxLayout(this);
    layout->setContentsMargins(40, 40, 40, 40);

    auto *header = new QLabel("Settings", this);
    header->setStyleSheet("font-size: 24px; font-weight: bold; margin-bottom: 20px;");
    layout->addWidget(header);

    auto *group = new QGroupBox("Backend Configuration", this);
    auto *form = new QFormLayout(group);
    
    m_urlInput = new QLineEdit(this);
    m_urlInput->setText(m_settings->serverUrl());
    m_urlInput->setPlaceholderText("https://y.itzjok3r.qzz.io/");
    form->addRow("Server URL:", m_urlInput);
    
    layout->addWidget(group);

    // Cookies
    auto *cookieGroup = new QGroupBox("Cookies", this);
    auto *cookieLayout = new QVBoxLayout(cookieGroup);
    m_cookieStatusLabel = new QLabel("Status: Unknown", this);
    m_uploadCookiesBtn = new QPushButton("Upload Cookies.txt", this);
    m_deleteCookiesBtn = new QPushButton("Delete Cookies", this);

    cookieLayout->addWidget(m_cookieStatusLabel);
    cookieLayout->addWidget(m_uploadCookiesBtn);
    cookieLayout->addWidget(m_deleteCookiesBtn);
    layout->addWidget(cookieGroup);

    auto *saveBtn = new QPushButton("Save Settings", this);
    saveBtn->setFixedHeight(40);
    saveBtn->setStyleSheet("background-color: #28a745; color: white; font-weight: bold;");
    layout->addWidget(saveBtn);
    
    layout->addStretch();

    connect(saveBtn, &QPushButton::clicked, this, [this]() {
        m_settings->setServerUrl(m_urlInput->text().trimmed());
        QMessageBox::information(this, "Settings Saved", "Application settings have been updated.");
    });

    connect(m_uploadCookiesBtn, &QPushButton::clicked, this, [this]() {
        QMessageBox::information(this, "Not Implemented", "File dialog and upload logic to be wired via RequestBuilder in future update.");
    });
    connect(m_deleteCookiesBtn, &QPushButton::clicked, this, [this]() {
        auto result = QMessageBox::question(this, "Confirm Deletion", "Are you sure you want to delete cookies?");
        if (result == QMessageBox::Yes) {
            m_viewModel->deleteCookies();
        }
    });

    connect(m_viewModel, &DownloadViewModel::cookieStatusUpdated, this, &SettingsScreen::refresh);
    refresh();
}

void SettingsScreen::refresh() {
    bool hasCookies = m_viewModel->getHasCookies();
    m_cookieStatusLabel->setText(hasCookies ? "Status: Active Cookies Present" : "Status: No Cookies Configured");
    m_cookieStatusLabel->setStyleSheet(hasCookies ? "color: #28a745; font-weight: bold;" : "color: #dc3545; font-weight: bold;");
}
