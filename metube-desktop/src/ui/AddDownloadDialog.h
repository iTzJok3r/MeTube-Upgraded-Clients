#ifndef ADDDOWNLOADDIALOG_H
#define ADDDOWNLOADDIALOG_H

#include <QDialog>
#include <QLineEdit>
#include <QComboBox>
#include <QPushButton>

class AddDownloadDialog : public QDialog {
    Q_OBJECT
public:
    explicit AddDownloadDialog(QWidget *parent = nullptr);

    QString url() const;
    QString type() const;
    QString quality() const;

private:
    void setupUi();

    QLineEdit *m_urlEdit;
    QComboBox *m_typeCombo;
    QComboBox *m_qualityCombo;
};

#endif // ADDDOWNLOADDIALOG_H
