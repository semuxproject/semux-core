cmake_minimum_required(VERSION 3.5)

project(crypto)
set(CMAKE_BUILD_TYPE Release)
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(CMAKE_CXX_FLAGS_RELEASE "-Os -fdata-sections -ffunction-sections -fstack-protector")
add_definitions(-DED25519_CUSTOMHASH -DED25519_CUSTOMRANDOM)
list(APPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_LIST_DIR}/cmake")

# Java JNI
include_directories("./jni")

# libsodium library
include_directories("./libsodium/src/libsodium/include")
add_custom_target(
    sodium
    COMMAND make clean
    COMMAND ./autogen.sh
    COMMAND ./configure --enable-shared=no --enable-minimal=yes --disable-pie --host=${CMAKE_SYSTEM_PROCESSOR}-${TARGET_ABI}
    COMMAND make -j${nproc}
    WORKING_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/libsodium"
)

# source files
set(SOURCE_FILES org_semux_crypto_Native.cpp ripemd160.c ed25519-donna/ed25519.c)

# build
add_library(semuxcrypto SHARED ${SOURCE_FILES})
add_dependencies(semuxcrypto sodium)
target_link_libraries(
    semuxcrypto
    "-Wl,--whole-archive ${CMAKE_CURRENT_LIST_DIR}/libsodium/src/libsodium/.libs/libsodium.a -Wl,--no-whole-archive"
)