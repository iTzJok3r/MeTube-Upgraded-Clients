#include "SubscriptionsScreen.h"
#include <QVBoxLayout>
#include <QHBoxLayout>
#include <QLabel>
#include <QHeaderView>
#include <QInputDialog>
#include <QMessageBox>

SubscriptionsScreen::SubscriptionsScreen(DownloadViewModel *viewModel, QWidget *parent)
    : QWidget(parent), m_viewModel(viewModel) {
    auto *layout = new QVBoxLayout(this);

    auto *header = new QLabel("Subscriptions", this);
    header->setStyleSheet("font-size: 20px; font-weight: bold; margin-bottom: 10px;");
    layout->addWidget(header);

    // Toolbar
    auto *toolbar = new QHBoxLayout();
    m_refreshBtn = new QPushButton("Refresh", this);
    m_addBtn = new QPushButton("Add Subscription", this);
    m_editBtn = new QPushButton("Edit", this);
    m_deleteBtn = new QPushButton("Delete", this);
    
    toolbar->addWidget(m_refreshBtn);
    toolbar->addStretch();
    toolbar->addWidget(m_addBtn);
    toolbar->addWidget(m_editBtn);
    toolbar->addWidget(m_deleteBtn);
    layout->addLayout(toolbar);

    // Table
    m_table = new QTableWidget(0, 4, this);
    m_table->setHorizontalHeaderLabels({"Name", "URL", "Status", "Schedule"});
    m_table->horizontalHeader()->setSectionResizeMode(QHeaderView::Stretch);
    m_table->setSelectionBehavior(QAbstractItemView::SelectRows);
    m_table->setSelectionMode(QAbstractItemView::SingleSelection);
    m_table->setEditTriggers(QAbstractItemView::NoEditTriggers);
    layout->addWidget(m_table, 1);

    // Connections
    connect(m_refreshBtn, &QPushButton::clicked, this, [this]() {
        m_viewModel->fetchSubscriptions();
    });
    connect(m_addBtn, &QPushButton::clicked, this, &SubscriptionsScreen::onAddClicked);
    connect(m_editBtn, &QPushButton::clicked, this, &SubscriptionsScreen::onEditClicked);
    connect(m_deleteBtn, &QPushButton::clicked, this, &SubscriptionsScreen::onDeleteClicked);

    // Automatically check for subscription updates on backend response
    connect(m_viewModel, &DownloadViewModel::subscriptionsUpdated, this, &SubscriptionsScreen::refresh);

    refresh();
}

void SubscriptionsScreen::refresh() {
    auto subs = m_viewModel->getSubscriptions();
    m_table->setRowCount(subs.size());

    int row = 0;
    for (const auto &sub : subs) {
        QString id = sub.value("id").toString();
        QString name = sub.value("name").toString();
        QString url = sub.value("url").toString();
        QString status = sub.value("status").toString();
        QString schedule = sub.value("schedule").toString();

        auto *nameItem = new QTableWidgetItem(name);
        nameItem->setData(Qt::UserRole, id); // Store ID for CRUD referencing
        m_table->setItem(row, 0, nameItem);
        m_table->setItem(row, 1, new QTableWidgetItem(url));
        m_table->setItem(row, 2, new QTableWidgetItem(status));
        m_table->setItem(row, 3, new QTableWidgetItem(schedule));

        row++;
    }
}

void SubscriptionsScreen::onAddClicked() {
    // Quick Add dialog for parity proxying. Ideally a custom dialog with all options,
    // but demonstrating the model here.
    bool ok;
    QString url = QInputDialog::getText(this, "Add Subscription", "Playlist/Channel URL:", QLineEdit::Normal, "", &ok);
    if (ok && !url.isEmpty()) {
        QString name = QInputDialog::getText(this, "Add Subscription", "Display Name:");
        QVariantMap params;
        params.insert("url", url);
        if (!name.isEmpty()) params.insert("name", name);
        m_viewModel->addSubscription(params);
    }
}

void SubscriptionsScreen::onEditClicked() {
    int row = m_table->currentRow();
    if (row < 0) {
        QMessageBox::warning(this, "No Selection", "Please select a subscription to edit.");
        return;
    }
    QString id = m_table->item(row, 0)->data(Qt::UserRole).toString();
    QString currentName = m_table->item(row, 0)->text();

    bool ok;
    QString newName = QInputDialog::getText(this, "Edit Subscription", "New Display Name:", QLineEdit::Normal, currentName, &ok);
    if (ok) {
        QVariantMap changes;
        if (!newName.isEmpty()) changes.insert("name", newName);
        m_viewModel->updateSubscription(id, changes);
    }
}

void SubscriptionsScreen::onDeleteClicked() {
    int row = m_table->currentRow();
    if (row < 0) {
        QMessageBox::warning(this, "No Selection", "Please select a subscription to delete.");
        return;
    }
    QString id = m_table->item(row, 0)->data(Qt::UserRole).toString();

    auto result = QMessageBox::question(this, "Confirm Deletion", "Are you sure you want to delete this subscription?");
    if (result == QMessageBox::Yes) {
        m_viewModel->deleteSubscriptions(QStringList() << id);
    }
}
