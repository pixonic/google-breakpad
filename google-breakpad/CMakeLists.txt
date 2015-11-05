cmake_minimum_required(VERSION 3.1.0)

project(AndroidBreakpad)

option(BREAKPAD_SHARED_LIBRARY "Build shared library for breakpad")

if (NOT ANDROID)
	message(FATAL "This package is android-only")
endif()

include_directories(src/common/android/include src)

enable_language(ASM)

if (BREAKPAD_SHARED_LIBRARY)
	set(BREAKPAD_LIBRARY_TYPE SHARED)
else()
	set(BREAKPAD_LIBRARY_TYPE STATIC)
endif()

add_library(Breakpad ${BREAKPAD_LIBRARY_TYPE}
    src/client/linux/crash_generation/crash_generation_client.cc
    src/client/linux/handler/exception_handler.cc
    src/client/linux/handler/minidump_descriptor.cc
    src/client/linux/log/log.cc
    src/client/linux/minidump_writer/linux_dumper.cc
    src/client/linux/minidump_writer/linux_ptrace_dumper.cc
    src/client/linux/minidump_writer/minidump_writer.cc
    src/client/minidump_file_writer.cc
    src/common/android/breakpad_getcontext.S
    src/common/convert_UTF.c
    src/common/md5.cc
	src/common/string_conversion.cc
    src/common/linux/elfutils.cc
    src/common/linux/file_id.cc
    src/common/linux/guid_creator.cc
    src/common/linux/linux_libc_support.cc
    src/common/linux/memory_mapped_file.cc
    src/common/linux/safe_readlink.cc
	android/setup.h
	android/setup.cc
)

target_link_libraries(Breakpad -llog)

install(TARGETS Breakpad 
		LIBRARY DESTINATION lib
        ARCHIVE DESTINATION lib/static)

include_directories(src android/google_breakpad)

install(FILES android/setup.h DESTINATION include/breakpad)
