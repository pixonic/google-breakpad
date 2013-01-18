google-breakpad
===============

Библиотека для отправки на сервер информации (callstack, os ver, device type, ...) при нативных крэшах Android приложений

Подключение
===========

* Импорт библиотеки в Eclipse

To import the SDK library project and the samples, with the new SDK, go to Eclipse's 'File' > 'Import' menu, and select 'General' / 'Existing Projects into Workspace':
Для импорта проекта библиотеки перейдите откройте меню 'File' > 'Import и выбирите 'General' / 'Existing Projects into Workspace':
Выбирите директорию BreakpabIntergation. У вас должен появиться проект BreakpabIntergation.


* Подключение NDK модуля

В Application.mk добавте модуль breakpad_client
	
	APP_MODULES += breakpad_client

В Android.mk подключите .mk файл NDK модуля, который находится в <install dir>/google-breakpad/android/google_breakpad/Android.mk

К примеру:
	
	include $(LOCAL_PATH)/../../third-party/breakpad/google-breakpad/android/google_breakpad/Android.mk

В LOCAL_C_INCLUDES добавте пути к <install dir>/google-breakpad, <install dir>/google-breakpad/src и <install dir>/google-breakpad/src/common/android/include.

К примеру:

	LOCAL_C_INCLUDES +=	$(LOCAL_PATH)/../third-party/breakpad/google-breakpad \
						$(LOCAL_PATH)/../third-party/breakpad/google-breakpad/src \
						$(LOCAL_PATH)/../third-party/breakpad/google-breakpad/src/common/android/include

В LOCAL_STATIC_LIBRARIES добавле библиотеку breakpad_client

	LOCAL_STATIC_LIBRARIES += breakpad_client
	
* Сборка проекта с использование goolegle breakpad модул.

В нативном коде, в методе JNI_OnLoad нужно вызвать метод crashHandlerSetJavaVM(JavaVM *javaVM); и передать в него указатель на виртуальную машину Java:

	#include "android/google_breakpad/integration.h"
	
	//...
	
	jint JNI_OnLoad(JavaVM *vm, void *reserved)
	{
		cocos2d::JniHelper::setJavaVM(vm);		
		// Try to catch crashes...
		crashHandlerSetJavaVM(vm);

		return JNI_VERSION_1_4;
	}

Для инициализации модуля CrashHandler в Java коде, в методе onCreate для Activity следует вызывать CrashHandler.init(Activity):

	import com.pixonic.breakpabintergation.CrashHandler;
	
	//...
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// initialize crash handlers
		CrashHandler.init(this);
		
		super.onCreate(savedInstanceState);
		
		//...
	}
	
Опционально для библиотеки может быть задано имя приложения которое используется для отправки информации о креше на сервер. Для этого нужно вызвать метод CrashHandler.setApplicationName(String appName). Если имя не задано, по умолчанию будет использоваться package name для Activity.

	CrashHandler.setApplicationName("My Cool Game").