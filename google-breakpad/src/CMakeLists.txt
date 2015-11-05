if (NOT ANDROID)
	message(FATAL "This package is android-only")
endif()

include_directories(.)

include_directories(common/android/include src)

enable_language(ASM)

add_library(Breakpad STATIC
    client/linux/crash_generation/crash_generation_client.cc
    client/linux/handler/exception_handler.cc
    client/linux/handler/minidump_descriptor.cc
    client/linux/log/log.cc
    client/linux/minidump_writer/linux_dumper.cc
    client/linux/minidump_writer/linux_ptrace_dumper.cc
    client/linux/minidump_writer/minidump_writer.cc
    client/minidump_file_writer.cc
    common/android/breakpad_getcontext.S
    common/convert_UTF.c
    common/md5.cc
	common/string_conversion.cc
    common/linux/elfutils.cc
    common/linux/file_id.cc
    common/linux/guid_creator.cc
    common/linux/linux_libc_support.cc
    common/linux/memory_mapped_file.cc
    common/linux/safe_readlink.cc)

target_link_libraries(Breakpad -llog)
