cmake_minimum_required(VERSION 3.16)

project(MeTubeDesktop VERSION 1.0.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(Qt6 REQUIRED COMPONENTS Widgets Network Gui Core WebSockets)

set(CMAKE_AUTOMOC ON)
set(CMAKE_AUTORCC ON)
set(CMAKE_AUTOUIC ON)
add_definitions(-DQT_NO_FLOAT16)

include_directories(
    src 
    src/core 
    src/services 
    src/viewmodels
    src/ui 
    src/ui/screens
    src/ui/widgets
    src/utils
)

set(SOURCES
    src/main.cpp
    src/core/AppSettings.cpp
    src/core/DownloadManager.cpp
    src/services/RequestBuilder.cpp
    src/services/MetubeRestService.cpp
    src/services/MetubeSocketService.cpp
    src/services/ClipboardMonitor.cpp
    src/services/NativeNotificationService.cpp
    src/services/SystemTrayManager.cpp
    src/viewmodels/DownloadViewModel.cpp
    src/ui/MainWindow.cpp
    src/ui/screens/HomeScreen.cpp
    src/ui/screens/QueueScreen.cpp
    src/ui/screens/HistoryScreen.cpp
    src/ui/screens/SubscriptionsScreen.cpp
    src/ui/screens/SettingsScreen.cpp
    src/ui/widgets/DownloadItemWidget.cpp
    src/ui/widgets/DownloadListWidget.cpp
    src/ui/AddDownloadDialog.cpp
    src/utils/JsonUtils.cpp
)

set(RESOURCES
    resources/resources.qrc
)

# if(WIN32)
#     set(SOURCES ${SOURCES} resources/windows.rc)
# endif()

add_executable(MeTubeDesktop ${SOURCES} ${RESOURCES})

target_link_libraries(MeTubeDesktop PRIVATE 
    Qt6::Widgets 
    Qt6::Network
    Qt6::WebSockets
)

if(MINGW)
    set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -Wl,--allow-multiple-definition")
endif()
