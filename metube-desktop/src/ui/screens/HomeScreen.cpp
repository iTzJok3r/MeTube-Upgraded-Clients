#include "HomeScreen.h"
#include <QVBoxLayout>
#include <QHBoxLayout>
#include <QFormLayout>
#include <QLabel>
#include <QScrollArea>
#include <QMessageBox>
#include <QInputDialog>

HomeScreen::HomeScreen(QWidget *parent) : QWidget(parent) {
    auto *mainLayout = new QVBoxLayout(this);
    mainLayout->setContentsMargins(0, 0, 0, 0);

    auto *scrollArea = new QScrollArea(this);
    scrollArea->setWidgetResizable(true);
    scrollArea->setFrameShape(QFrame::NoFrame);

    auto *container = new QWidget();
    auto *layout = new QVBoxLayout(container);

    auto *header = new QLabel("Add a Download", this);
    header->setStyleSheet("font-size: 18px; font-weight: bold; margin-bottom: 10px;");
    layout->addWidget(header);

    // URL input
    m_urlInput = new QLineEdit(this);
    m_urlInput->setPlaceholderText("Paste a URL here (YouTube, etc.)");
    m_urlInput->setMinimumHeight(36);
    layout->addWidget(m_urlInput);

    // Basic Options group
    m_optionsGroup = new QGroupBox("Download Options", this);
    auto *optForm = new QFormLayout(m_optionsGroup);

    m_typeCombo = new QComboBox(this);
    m_typeCombo->addItems({"video", "audio", "captions", "thumbnail"});
    optForm->addRow("Type:", m_typeCombo);

    m_qualityCombo = new QComboBox(this);
    m_qualityCombo->addItems({"best", "2160", "1440", "1080", "720", "480", "360", "240", "worst"});
    optForm->addRow("Quality:", m_qualityCombo);

    m_formatCombo = new QComboBox(this);
    optForm->addRow("Format:", m_formatCombo);

    m_codecCombo = new QComboBox(this);
    optForm->addRow("Codec:", m_codecCombo);

    // Dynamically update format/quality/codec options when type changes
    connect(m_typeCombo, &QComboBox::currentTextChanged, this, &HomeScreen::onTypeChanged);
    onTypeChanged(m_typeCombo->currentText());

    layout->addWidget(m_optionsGroup);

    // Advanced Options group
    m_advancedOptionsGroup = new QGroupBox("Advanced Options", this);
    auto *advForm = new QFormLayout(m_advancedOptionsGroup);

    m_folderCombo = new QComboBox(this);
    m_folderCombo->setEditable(true);
    m_folderCombo->setPlaceholderText("Default specific folder");
    advForm->addRow("Folder:", m_folderCombo);

    m_customNamePrefixInput = new QLineEdit(this);
    advForm->addRow("Name Prefix:", m_customNamePrefixInput);

    m_playlistLimitSpin = new QSpinBox(this);
    m_playlistLimitSpin->setRange(0, 1000);
    m_playlistLimitSpin->setSpecialValueText("Unlimited (0)");
    advForm->addRow("Playlist Limit:", m_playlistLimitSpin);

    m_autoStartCheck = new QCheckBox("Start download immediately", this);
    m_autoStartCheck->setChecked(true);
    advForm->addRow("", m_autoStartCheck);

    m_splitChaptersCheck = new QCheckBox("Split by chapters", this);
    advForm->addRow("", m_splitChaptersCheck);

    m_chapterTemplateInput = new QLineEdit(this);
    m_chapterTemplateInput->setPlaceholderText("Template for chapters");
    advForm->addRow("Chapter Format:", m_chapterTemplateInput);

    m_subtitleLanguageInput = new QLineEdit(this);
    m_subtitleLanguageInput->setPlaceholderText("en, fr, id (comma separated)");
    advForm->addRow("Sub. Language:", m_subtitleLanguageInput);

    m_subtitleModeCombo = new QComboBox(this);
    m_subtitleModeCombo->addItems({"prefer_manual", "prefer_auto", "merge_all"});
    advForm->addRow("Sub. Mode:", m_subtitleModeCombo);

    m_presetsInput = new QLineEdit(this);
    m_presetsInput->setPlaceholderText("Comma separated presets (e.g. podcast, hq)");
    advForm->addRow("ytdl Presets:", m_presetsInput);

    layout->addWidget(m_advancedOptionsGroup);

    // Submit button
    m_submitBtn = new QPushButton("Download", this);
    m_submitBtn->setMinimumHeight(40);
    m_submitBtn->setStyleSheet("background-color: #1a73e8; color: white; font-weight: bold; border-radius: 6px;");
    connect(m_submitBtn, &QPushButton::clicked, this, &HomeScreen::onSubmit);
    layout->addWidget(m_submitBtn);

    auto *batchBtn = new QPushButton("Batch Import", this);
    batchBtn->setMinimumHeight(32);
    batchBtn->setStyleSheet("color: #1a73e8; border: 1px solid #1a73e8; border-radius: 4px; margin-top: 5px;");
    connect(batchBtn, &QPushButton::clicked, this, [this]() {
        bool ok;
        QString text = QInputDialog::getMultiLineText(this, "Batch Import", "Paste URLs (one per line):", "", &ok);
        if (ok && !text.isEmpty()) {
            QStringList lines = text.split("\n", Qt::SkipEmptyParts);
            for (const QString &line : lines) {
                QString url = line.trimmed();
                if (url.isEmpty()) continue;
                
                // Trigger download for each URL with current UI options
                emit downloadRequested(url, m_typeCombo->currentText(), m_qualityCombo->currentText(),
                                       m_formatCombo->currentText(), m_codecCombo->currentText(),
                                       m_folderCombo->currentText().trimmed(), m_customNamePrefixInput->text().trimmed(),
                                       m_playlistLimitSpin->value(), m_autoStartCheck->isChecked(),
                                       m_splitChaptersCheck->isChecked(), m_chapterTemplateInput->text().trimmed(),
                                       m_subtitleLanguageInput->text().trimmed(), m_subtitleModeCombo->currentText(),
                                       m_presetsInput->text().split(",", Qt::SkipEmptyParts), QVariantMap());
            }
            QMessageBox::information(this, "Batch Import", QString("Added %1 URLs to queue.").arg(lines.size()));
        }
    });
    layout->addWidget(batchBtn);

    layout->addStretch();
    
    scrollArea->setWidget(container);
    mainLayout->addWidget(scrollArea);
}

void HomeScreen::setCustomDirs(const QStringList &dirs) {
    QString current = m_folderCombo->currentText();
    m_folderCombo->clear();
    m_folderCombo->addItem("");
    m_folderCombo->addItems(dirs);
    m_folderCombo->setCurrentText(current);
}

void HomeScreen::onTypeChanged(const QString &type) {
    m_formatCombo->clear();
    m_codecCombo->clear();
    m_qualityCombo->clear();

    if (type == "video") {
        m_formatCombo->addItems({"any", "mp4", "ios"});
        m_codecCombo->addItems({"auto", "h264", "h265", "av1", "vp9"});
        m_qualityCombo->addItems({"best", "2160", "1440", "1080", "720", "480", "360", "240", "worst"});
    } else if (type == "audio") {
        m_formatCombo->addItems({"m4a", "mp3", "opus", "wav", "flac"});
        m_codecCombo->addItem("auto");
        m_qualityCombo->addItems({"best", "320", "192", "128"});
    } else if (type == "captions") {
        m_formatCombo->addItems({"srt", "txt", "vtt", "ttml", "sbv", "scc", "dfxp"});
        m_codecCombo->addItem("auto");
        m_qualityCombo->addItem("best");
    } else if (type == "thumbnail") {
        m_formatCombo->addItem("jpg");
        m_codecCombo->addItem("auto");
        m_qualityCombo->addItem("best");
    }
}

void HomeScreen::onSubmit() {
    QString url = m_urlInput->text().trimmed();
    if (url.isEmpty()) {
        QMessageBox::warning(this, "Missing URL", "Please enter a URL to download.");
        return;
    }

    QString type = m_typeCombo->currentText();
    QString quality = m_qualityCombo->currentText();
    QString format = m_formatCombo->currentText();
    QString codec = m_codecCombo->currentText();

    // Default empty values to what the backend expects
    if (format.isEmpty()) format = "any";
    if (codec.isEmpty()) codec = "auto";

    QString folder = m_folderCombo->currentText().trimmed();
    QString customNamePrefix = m_customNamePrefixInput->text().trimmed();
    int playlistItemLimit = m_playlistLimitSpin->value();
    bool autoStart = m_autoStartCheck->isChecked();
    bool splitByChapters = m_splitChaptersCheck->isChecked();
    QString chapterTemplate = m_chapterTemplateInput->text().trimmed();
    QString subtitleLanguage = m_subtitleLanguageInput->text().trimmed();
    QString subtitleMode = m_subtitleModeCombo->currentText();
    
    QStringList ytdlOptionsPresets;
    if (!m_presetsInput->text().trimmed().isEmpty()) {
        const auto parts = m_presetsInput->text().split(",");
        for (const auto &p : parts) ytdlOptionsPresets.append(p.trimmed());
    }

    emit downloadRequested(url, type, quality, format, codec, folder, customNamePrefix, playlistItemLimit,
                           autoStart, splitByChapters, chapterTemplate, subtitleLanguage, subtitleMode,
                           ytdlOptionsPresets, QVariantMap());
    m_urlInput->clear();
}
