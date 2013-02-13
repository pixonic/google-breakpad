#include "../android/google_breakpad/integration.h"
#include "client/linux/handler/exception_handler.h"
#include "client/linux/handler/minidump_descriptor.h"

#include <jni.h>
#include <android/log.h>

#define  LOG_TAG "jni/BrackpadIntegration"
#define  LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

JavaVM *gJavaVMInstance = NULL;
void crashHandlerSetJavaVM(JavaVM *javaVM)
{
	gJavaVMInstance = javaVM;
}

namespace
{
	JNIEnv* getJNIEnv()
	{
		JNIEnv* env = NULL;
		if(gJavaVMInstance)
		{
			if(gJavaVMInstance->GetEnv((void**)&env, JNI_VERSION_1_4) == JNI_EDETACHED)
			{
				gJavaVMInstance->AttachCurrentThread(&env, NULL);
			}
		}
		else
		{
			LOGD("getJNIEnv: invalid java vm");
		}
		return env;
	}

	bool crashHandlerDumpCallback(const google_breakpad::MinidumpDescriptor& descriptor, void* context, bool succeeded)
	{
		LOGD("crashHandlerDumpCallback: application crashed");

		std::string dumpPath(descriptor.path());
		std::string dumpFile = "crash.dmp";

		size_t found = dumpPath.find_last_of("/\\");
		if(found != std::string::npos)
		{
			dumpFile = dumpPath.substr(found + 1);
		}

		JNIEnv *env = getJNIEnv();
		if(env)
		{
			jclass classID = env->FindClass("com/pixonic/breakpadintergation/CrashHandler");
			if(classID)
			{
				jmethodID methodID = env->GetStaticMethodID(classID, "nativeCrashed", "(Ljava/lang/String;)V");
				if(methodID)
				{
					// create the first parameter for java method
					jstring firstParameter = env->NewStringUTF(dumpFile.c_str());

					env->CallStaticVoidMethod(classID, methodID, firstParameter);

					// delete local references
					env->DeleteLocalRef(firstParameter);
				}
				else
				{
					LOGD("crashHandlerDumpCallback: java method not found");
				}

				env->DeleteLocalRef(classID);
			}
			else
			{
				LOGD("crashHandlerDumpCallback: java class not found");
			}
		}
		else
		{
			LOGD("crashHandlerDumpCallback: invalid Java environment");
		}

		// remove local file
		::remove(dumpPath.c_str());

		return succeeded;
	}

	void setupCrashHandler(const std::string &path)
	{
		LOGD("setupCrashHandler");

		std::string writeablePath = path;
		size_t found = writeablePath.find_last_of("/\\");
		if(found == writeablePath.length() - 1)
		{
			writeablePath = writeablePath.substr(0, writeablePath.length() - 1);
		}
		google_breakpad::MinidumpDescriptor dumpDescriptor(writeablePath);
		static google_breakpad::ExceptionHandler exceptionHandler(dumpDescriptor, NULL, crashHandlerDumpCallback, NULL,
		        true, -1);
	}
}

extern "C"
{
	void Java_com_pixonic_breakpadintergation_CrashHandler_nativeInit(JNIEnv *env, jobject self, jstring path)
	{
		jboolean isCopy;
		const char* chars = env->GetStringUTFChars(path, &isCopy);
		string pathStr(chars);
		if(isCopy)
		{
			env->ReleaseStringUTFChars(path, chars);
		}

		setupCrashHandler(pathStr);
	}
}
