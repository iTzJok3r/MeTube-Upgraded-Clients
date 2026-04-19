#include "AddDownloadDialog.h"
#include <QVBoxLayout>
#include <QHBoxLayout>
#include <QLabel>
#include <QFormLayout>

AddDownloadDialog::AddDownloadDialog(QWidget *parent) : QDialog(parent) {
    setupUi();
    setWindowTitle("Add New Download");
    setMinimumWidth(400);
}

void AddDownloadDialog::setupUi() {
    QVBoxLayout *mainLayout = new QVBoxLayout(this);

    QFormLayout *form = new QFormLayout();
    
    m_urlEdit = new QLineEdit(this);
    m_urlEdit->setPlaceholderText("Paste URL here...");
    form->addRow("URL:", m_urlEdit);

    m_typeCombo = new QComboBox(this);
    m_typeCombo->addItems({"video", "audio", "captions", "thumbnail"});
    form->addRow("Type:", m_typeCombo);

    m_qualityCombo = new QComboBox(this);
    m_qualityCombo->addItems({"best", "1080", "720", "480", "360"});
    form->addRow("Quality:", m_qualityCombo);

    mainLayout->addLayout(form);

    QHBoxLayout *buttons = new QHBoxLayout();
    QPushButton *okButton = new QPushButton("Download", this);
    okButton->setDefault(true);
    QPushButton *cancelButton = new QPushButton("Cancel", this);

    buttons->addStretch();
    buttons->addWidget(okButton);
    buttons->addWidget(cancelButton);
    mainLayout->addLayout(buttons);

    connect(okButton, &QPushButton::clicked, this, &QDialog::accept);
    connect(cancelButton, &QPushButton::clicked, this, &QDialog::reject);
}

QString AddDownloadDialog::url() const { return m_urlEdit->text().trimmed(); }
QString AddDownloadDialog::type() const { return m_typeCombo->currentText(); }
QString AddDownloadDialog::quality() const { return m_qualityCombo->currentText(); }
