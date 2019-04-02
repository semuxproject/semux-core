cmake_minimum_required(VERSION 3.5)

project(crypto)
set(CMAKE_BUILD_TYPE Release)
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS} -Os -fdata-sections -ffunction-sections -fstack-protector")
add_definitions(-DED25519_CUSTOMHASH -DED25519_CUSTOMRANDOM)
list(APPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_LIST_DIR}/cmake")

# Java JNI
include_directories("./jni")

# libsodium library
include_directories("./libsodium/src/libsodium/include")
add_custom_target(
    sodium
    COMMAND env CFLAGS=${CMAKE_C_FLAGS} | grep CFLAGS
    COMMAND env LDFLAGS=${CMAKE_SHARED_LINKER_FLAGS} | grep LDFLAGS
    COMMAND ./autogen.sh
    COMMAND ./configure --enable-shared=yes --enable-minimal=yes --disable-pie
    COMMAND make check -j${nproc}
    WORKING_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}/libsodium"
)

# source files
set(SOURCE_FILES org_semux_crypto_Native.cpp ripemd160.c ed25519-donna/ed25519.c)

# build
add_library(semuxcrypto SHARED ${SOURCE_FILES})
add_dependencies(semuxcrypto sodium)
if(CMAKE_SYSTEM_NAME STREQUAL "Darwin")
    target_link_libraries(
        semuxcrypto
        "-Wl,-force_load,${CMAKE_CURRENT_LIST_DIR}/libsodium/src/libsodium/.libs/libsodium.dylib"
    )
    add_custom_command(
        TARGET semuxcrypto
        POST_BUILD
        COMMAND ${CMAKE_INSTALL_NAME_TOOL} -change "/usr/local/lib/libsodium.23.dylib" "@loader_path/libsodium.23.dylib" $<TARGET_FILE:semuxcrypto>
        COMMAND cp $<TARGET_FILE:semuxcrypto> ${CMAKE_CURRENT_LIST_DIR}/../../resources/native/x86_64-darwin
        COMMAND cp ${CMAKE_CURRENT_LIST_DIR}/libsodium/src/libsodium/.libs/libsodium.23.dylib ${CMAKE_CURRENT_LIST_DIR}/../../resources/native/x86_64-darwin
    )
else()
    target_link_libraries(
        semuxcrypto
        "-Wl,--whole-archive ${CMAKE_CURRENT_LIST_DIR}/libsodium/src/libsodium/.libs/libsodium.a -Wl,--no-whole-archive"
    )
endif()