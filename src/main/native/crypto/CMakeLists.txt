cmake_minimum_required(VERSION 3.5)

project(crypto)
set(CMAKE_BUILD_TYPE Release)
set(CMAKE_CXX_STANDARD 11)
list(APPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_LIST_DIR}/cmake")

# Java JNI
find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

# libsodium library
find_package(sodium REQUIRED)
include_directories(${sodium_INCLUDE_DIR})

# source files
set(SOURCE_FILES org_semux_crypto_Native.cpp)

# build
add_library(crypto SHARED ${SOURCE_FILES})
target_link_libraries(crypto sodium)