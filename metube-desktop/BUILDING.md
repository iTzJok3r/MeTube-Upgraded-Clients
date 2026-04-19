# Building MeTube Desktop

This document outlines the dependencies and build instructions for compiling the MeTube Desktop client from source.

## Requirements

### Global Dependencies
- **C++ Compiler**: A C++17 compatible compiler (e.g., GCC, Clang, MSVC).
- **CMake**: Version 3.16 or higher.
- **Qt 6 Framework**: Including the following specific Qt modules:
  - `Qt6::Core`
  - `Qt6::Gui`
  - `Qt6::Widgets`
  - `Qt6::Network`
  - `Qt6::WebSockets`

### Platform-Specific Dependencies
- **Windows**: MinGW-w64 or MSVC toolkit. A standard Qt 6 offline/online installation covers all requirements.
- **Linux**: Standard build essentials and Qt 6 development packages. E.g., on Ubuntu/Debian:
  ```bash
  sudo apt install build-essential cmake qt6-base-dev libqt6websockets6-dev libgl1-mesa-dev
  ```
- **macOS**: Xcode Command Line Tools and Qt 6 (typically installed via Homebrew: `brew install qt`).

## Build Instructions (Command Line)

**1. Clone the repository and prepare the build directory**
```bash
git clone https://github.com/your-username/metube.git
cd metube/metube-desktop
mkdir build
cd build
```

**2. Configure CMake**
Depending on your platform and generator:
```bash
# Standard Unix Makefiles / Default generator
cmake ..

# Windows with MinGW
cmake .. -G "MinGW Makefiles"
```

**3. Build the Application**
```bash
cmake --build .
```

The resulting executable (`MeTubeDesktop` or `MeTubeDesktop.exe`) will be generated inside the `build` directory.

## IDE Integration

You can natively open `CMakeLists.txt` using **Qt Creator**, **Visual Studio Code** (with the CMake Tools and C/C++ extensions), or **Visual Studio 2022**. The IDE will detect the targets and configure the build context automatically.
