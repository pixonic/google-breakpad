cmake_minimum_required(VERSION 3.1.0)

project(AndroidBreakpad)

add_subdirectory(src)

include_directories(src src/common/android/include android/google_breakpad)

add_library(AndroidBreakpad SHARED
	android/setup.h
	android/setup.cc
)

target_link_libraries(AndroidBreakpad Breakpad)