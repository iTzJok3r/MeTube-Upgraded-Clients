#ifndef HOMESCREEN_H
#define HOMESCREEN_H

#include <QWidget>
#include <QLineEdit>
#include <QComboBox>
#include <QPushButton>
#include <QGroupBox>
#include <QSpinBox>
#include <QCheckBox>
#include <QVariantMap>
#include <QStringList>

#include "services/RequestBuilder.h"

class HomeScreen : public QWidget {
    Q_OBJECT
public:
    explicit HomeScreen(QWidget *parent = nullptr);

signals:
    void downloadRequested(const QString &url, 
                          const QString &type, 
                          const QString &quality,
                          const QString &format,
                          const QString &codec,
                          const QString &folder,
                          const QString &customNamePrefix,
                          int playlistItemLimit,
                          bool autoStart,
                          bool splitByChapters,
                          const QString &chapterTemplate,
                          const QString &subtitleLanguage,
                          const QString &subtitleMode,
                          const QStringList &ytdlOptionsPresets,
                          const QVariantMap &ytdlOptionsOverrides);

public slots:
    void setCustomDirs(const QStringList &dirs);

private slots:
    void onSubmit();
    void onTypeChanged(const QString &type);

private:
    QLineEdit *m_urlInput;
    QGroupBox *m_optionsGroup;
    QComboBox *m_typeCombo;
    QComboBox *m_qualityCombo;
    QComboBox *m_formatCombo;
    QComboBox *m_codecCombo;
    
    QGroupBox *m_advancedOptionsGroup;
    QComboBox *m_folderCombo; // Editable so users can type or select
    QLineEdit *m_customNamePrefixInput;
    QSpinBox *m_playlistLimitSpin;
    QCheckBox *m_autoStartCheck;
    QCheckBox *m_splitChaptersCheck;
    QLineEdit *m_chapterTemplateInput;
    QLineEdit *m_subtitleLanguageInput;
    QComboBox *m_subtitleModeCombo;
    QLineEdit *m_presetsInput; // comma separated presets
    
    QPushButton *m_submitBtn;
};

#endif // HOMESCREEN_H
