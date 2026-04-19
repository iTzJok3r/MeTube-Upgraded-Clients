#ifndef SETTINGSSCREEN_H
#define SETTINGSSCREEN_H

#include <QWidget>
#include <QLineEdit>
#include <QLabel>
#include <QPushButton>
#include "core/AppSettings.h"
#include "viewmodels/DownloadViewModel.h"

class SettingsScreen : public QWidget {
    Q_OBJECT
public:
    explicit SettingsScreen(AppSettings *settings, DownloadViewModel *viewModel, QWidget *parent = nullptr);

public slots:
    void refresh();

private:
    AppSettings *m_settings;
    DownloadViewModel *m_viewModel;
    QLineEdit *m_urlInput;

    QLabel *m_cookieStatusLabel;
    QPushButton *m_uploadCookiesBtn;
    QPushButton *m_deleteCookiesBtn;
};

#endif // SETTINGSSCREEN_H
