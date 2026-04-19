#ifndef SUBSCRIPTIONSSCREEN_H
#define SUBSCRIPTIONSSCREEN_H

#include <QWidget>
#include <QTableWidget>
#include <QPushButton>
#include <QLineEdit>
#include "viewmodels/DownloadViewModel.h"

class SubscriptionsScreen : public QWidget {
    Q_OBJECT
public:
    explicit SubscriptionsScreen(DownloadViewModel *viewModel, QWidget *parent = nullptr);

public slots:
    void refresh();

private slots:
    void onAddClicked();
    void onEditClicked();
    void onDeleteClicked();

private:
    DownloadViewModel *m_viewModel;
    
    QTableWidget *m_table;
    QPushButton *m_addBtn;
    QPushButton *m_editBtn;
    QPushButton *m_deleteBtn;
    QPushButton *m_refreshBtn;
};

#endif // SUBSCRIPTIONSSCREEN_H
