#include <QApplication>
#include <QIcon>
#include "ui/MainWindow.h"

int main(int argc, char *argv[]) {
    QApplication a(argc, argv);
    
    a.setApplicationName("MeTube Desktop");
    a.setOrganizationName("iTzJok3r");
    a.setApplicationVersion("1.0.0");
    a.setWindowIcon(QIcon(":/icons/app.png"));

    MainWindow w;
    w.show();
    
    return a.exec();
}
