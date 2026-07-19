cmake_minimum_required(VERSION 3.22.1)
project(droidshield)

add_library(droidshield SHARED
    jni_bridge.cpp
)

target_link_libraries(droidshield
    log
)

# 16 KB page size support (Android 15+).
#
# Android 15 introduced devices with a 16 KB memory page size, and Google
# Play requires 64-bit native code to support them. A shared library whose
# PT_LOAD segments are aligned to the old 4 KB page size cannot be mapped by
# a 16 KB kernel, so the app fails to load libdroidshield.so at all — which
# for this SDK means System.loadLibrary() throws in NativeBridge's <init>
# and all four native-layer checks silently drop out of the engine.
#
# NDK r28 links with 16 KB alignment by default, but this project builds
# against AGP 8.7's default NDK (r27), where it is opt-in. Verified with
# llvm-readelf -lW: without this flag the arm64-v8a and x86_64 LOAD segments
# come out aligned 0x1000 rather than the required 0x4000.
#
# Only 64-bit ABIs need this — 16 KB pages do not exist on 32-bit Android,
# and applying it to armeabi-v7a would just pad the library for no benefit.
# Keying off pointer width rather than naming ABIs keeps this correct if a
# future 64-bit ABI is added to abiFilters.
if(CMAKE_SIZEOF_VOID_P EQUAL 8)
    target_link_options(droidshield PRIVATE
        "-Wl,-z,max-page-size=16384"
    )
endif()
