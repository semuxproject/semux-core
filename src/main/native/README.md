# Semux Native Library

libsemuxcrypto is a JNI library that aims to increase the performance Semux wallet by implementing cryptography functions in C++. This library relies on thrid-parties including [libsodium](https://github.com/jedisct1/libsodium) and [ed25519-donna](https://github.com/floodyberry/ed25519-donna).  

## Build on x86_64 Linux

Build on linux supports cross-compiling to the following platforms:

1. aarch64-linux
2. win64
3. x86_64-linux 

Prerequisites:
1. cmake
2. binutils-x86_64-linux-gnu
3. binutils-aarch64-linux-gnu
4. binutils-mingw-w64
5. automake
6. autoconf

Steps to build on Debian/Ubuntu based distributions:
```
sudo apt install cmake binutils binutils-aarch64-linux-gnu binutils-mingw-w64

mkdir build && cd build

cmake -vvv -DCMAKE_TOOLCHAIN_FILE=../cmake/toolchain-x86_64-linux.cmake ../
make -j$(nproc)
cp ./native/libsemuxcrypto.so ../../resources/native/x86_64-linux/
 
cmake -vvv -DCMAKE_TOOLCHAIN_FILE=../cmake/toolchain-aarch64-linux.cmake ../
make -j$(nproc)
cp ./native/libsemuxcrypto.so ../../resources/native/aarch64-linux/

cmake -vvv -DCMAKE_TOOLCHAIN_FILE=../cmake/toolchain-w64.cmake ../
make -j$(nproc)
cp ./native/libsemuxcrypto.dll ../../resources/native/win64/
```

## Build on macOS

Prerequisites:
1. clang
2. cmake
3. autoconf
4. automake

Build:
```
mkdir build && cd build
cmake ..
make
```
