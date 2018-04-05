# Semux Native Library

## Build on Linux


Prerequisites:
1. Build and install libsodium 1.0.16

Build:
```
mkdir build && cd build
cmake ..
make
```

## Build on macOS

Prerequisites:
1. Build and install libsodium 1.0.16

Build:
```
mkdir build && cd build
cmake ..
make
install_name_tool -change "/usr/local/lib/libsodium.23.dylib" "@loader_path/libsodium.23.dylib" crypto/libcrypto.dylib
```

## Build on Windows

Prerequisites:
1. Visual Studio 2012 build tools
2. CMake 3.8+
3. Download and unpack libsodium 1.0.16 pre-built binaries
4. Set `sodiumDIR` environment variable

Build:
```
mkdir build && cd build
cmake -G "Visual Studio 11 2012 x64" ..
# Build the solution with Visual Studio
```
