cmake_minimum_required(VERSION 3.22.1)
project(droidshield)

add_library(droidshield SHARED
    jni_bridge.cpp
)

target_link_libraries(droidshield
    log
)
